/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/

package com.adobe.cq.commerce.graphql.magento;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.RequestOptions;
import com.adobe.cq.commerce.graphql.resource.Constants;
import com.adobe.cq.commerce.magento.graphql.CategoryFilterInput;
import com.adobe.cq.commerce.magento.graphql.CategoryProducts;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterEqualTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterMatchTypeInput;
import com.adobe.cq.commerce.magento.graphql.FilterRangeTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductAttributeSortInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery;
import com.adobe.cq.commerce.magento.graphql.QueryQuery.CategoryArgumentsDefinition;
import com.adobe.cq.commerce.magento.graphql.QueryQuery.ProductsArgumentsDefinition;
import com.adobe.cq.commerce.magento.graphql.SortEnum;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

@Component(service = GraphqlDataService.class)
@Designate(ocd = GraphqlDataServiceConfiguration.class, factory = true)
public class GraphqlDataServiceImpl implements GraphqlDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlDataServiceImpl.class);
    private static final String MAGENTO_DEFAULT_STORE = "default";

    // We cannot extend GraphqlClientImpl because it's not OSGi-exported so we use "object composition"
    protected GraphqlClient baseClient;
    protected RequestOptions requestOptions;
    private GraphqlDataServiceConfiguration configuration;

    // We maintain some caches to speed up all lookups
    private Cache<ArrayKey, Optional<ProductInterface>> productCache;
    private Cache<ArrayKey, Optional<CategoryProducts>> categoryCache;

    private Map<String, GraphqlClient> clients = new ConcurrentHashMap<>();

    @Reference(
        service = GraphqlClient.class,
        bind = "bindGraphqlClient",
        unbind = "unbindGraphqlClient",
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC)
    protected void bindGraphqlClient(GraphqlClient graphqlClient, Map<?, ?> properties) {
        String identifier = graphqlClient.getIdentifier();
        LOGGER.info("Registering GraphqlClient '{}'", identifier);
        clients.put(identifier, graphqlClient);

        if (identifier.equals(configuration.identifier())) {
            LOGGER.info("GraphqlClient with identifier '{}' has been registered, the service is ready to handle requests.", identifier);
            baseClient = graphqlClient;
        }
    }

    protected void unbindGraphqlClient(GraphqlClient graphqlClient, Map<?, ?> properties) {
        String identifier = graphqlClient.getIdentifier();
        LOGGER.info("De-registering GraphqlClient '{}'", identifier);
        clients.remove(identifier);

        if (identifier.equals(configuration.identifier())) {
            LOGGER.info("GraphqlClient '{}' unregistered: requests cannot be handled until that dependency is satisfied", identifier);
            baseClient = null;
        }
    }

    @Activate
    public void activate(GraphqlDataServiceConfiguration conf) throws Exception {
        baseClient = clients.get(conf.identifier());
        if (baseClient == null) {
            // Because of the many:many OSGi dynamic dependencies between GraphqlDataServiceImpl and GraphqlClientImpl, we cannot enforce
            // the dependency and hence services might not start in the right order. So we ignore the missing client, it might start later
            // and would be set in the bindGraphqlClient() method.
            // Important: we let all public methods fail with NullPointerException, so that the Sling ResourceProvider will get exceptions
            // when trying to get resources. This is important, because it ensures that the resource provider will always try to refetch
            // the resources (if we for example return an empty list, this would get cached by Sling).

            LOGGER.warn("GraphqlClient '{}' not found: requests cannot be handled until that dependency is satisfied", conf.identifier());
        }

        configuration = conf;

        // Used when a single product is being fetched
        productCache = CacheBuilder.newBuilder()
            .maximumSize(configuration.productCachingEnabled() ? configuration.productCachingSize() : 0)
            .expireAfterWrite(configuration.productCachingTimeMinutes(), TimeUnit.MINUTES)
            .build();

        // Used when the products of a given category are being fetched
        categoryCache = CacheBuilder.newBuilder()
            .maximumSize(configuration.productCachingEnabled() ? configuration.categoryCachingSize() : 0)
            .expireAfterWrite(configuration.productCachingTimeMinutes(), TimeUnit.MINUTES)
            .build();

        requestOptions = new RequestOptions().withGson(QueryDeserializer.getGson());
    }

    protected GraphqlResponse<Query, Error> execute(String query, String storeView) {
        RequestOptions options = requestOptions;
        if (storeView != null) {
            Header storeHeader = new BasicHeader(Constants.STORE_HEADER, storeView);

            // Create new options to avoid setting the storeView as the new default value
            options = new RequestOptions()
                .withGson(requestOptions.getGson())
                .withHeaders(Collections.singletonList(storeHeader));
        }

        return baseClient.execute(new GraphqlRequest(query), Query.class, Error.class, options);
    }

    @Override
    public ProductInterface getProductBySku(String sku, String storeView) {
        if (sku == null) {
            return null;
        }

        try {
            ArrayKey key = toProductCacheKey(sku, storeView);
            return productCache.get(key, () -> getProductBySkuImpl(sku, storeView)).orElse(null);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    Optional<ProductInterface> getProductBySkuImpl(String sku, String storeView) {

        LOGGER.debug("Trying to fetch product " + sku);

        // Search parameters
        FilterEqualTypeInput input = new FilterEqualTypeInput().setEq(sku);
        ProductAttributeFilterInput filter = new ProductAttributeFilterInput().setSku(input);
        ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);

        // Main query
        ProductsQueryDefinition queryArgs = q -> q.items(GraphqlQueries.CONFIGURABLE_PRODUCT_QUERY);

        String queryString = Operations.query(query -> query.products(searchArgs, queryArgs)).toString();
        GraphqlResponse<Query, Error> response = execute(queryString, storeView);

        Query query = response.getData();
        Products productsQuery = query.getProducts();
        List<ProductInterface> products = productsQuery.getItems();
        ProductInterface product = products.size() > 0 ? products.get(0) : null;

        LOGGER.debug("Fetched product " + (product != null ? product.getName() : null));

        return Optional.ofNullable(product);
    }

    @Override
    public List<ProductInterface> searchProducts(String text, Integer categoryId, Integer currentPage, Integer pageSize, String storeView) {
        return searchProductsImpl(text, categoryId, currentPage, pageSize, storeView);
    }

    @Override
    public List<CategoryTree> searchCategories(String text, Integer categoryId, Integer currentPage, Integer pageSize, String storeView) {
        if (categoryId == null) {
            LOGGER.debug("Performing category search with '{}' (page: {}, size: {})", text, currentPage, pageSize);
        } else {
            LOGGER.debug("Performing category search with '{}' in category {} (page: {}, size: {})", text, categoryId, currentPage,
                pageSize);
        }

        // Search parameters
        FilterMatchTypeInput name = new FilterMatchTypeInput().setMatch(text);
        CategoryFilterInput filters = new CategoryFilterInput().setName(name);
        QueryQuery.CategoryListArgumentsDefinition searchArgs = q -> q.filters(filters);

        CategoryTreeQueryDefinition queryArgs = q -> GraphqlQueries.CATEGORY_SEARCH_QUERY.apply(q);
        String queryString = Operations.query(query -> query.categoryList(searchArgs, queryArgs)).toString();
        GraphqlResponse<Query, Error> response = execute(queryString, storeView);
        Query query = response.getData();
        List<CategoryTree> categoryList = query.getCategoryList();

        return categoryList;
    }

    private List<ProductInterface> searchProductsImpl(String text, Integer categoryId, Integer currentPage, Integer pageSize,
        String storeView) {

        if (categoryId == null) {
            LOGGER.debug("Performing product search with '{}' (page: {}, size: {})", text, currentPage, pageSize);
        } else {
            LOGGER.debug("Performing product search with '{}' in category {} (page: {}, size: {})", text, categoryId, currentPage,
                pageSize);
        }

        // Search parameters
        ProductsArgumentsDefinition searchArgs;
        ProductAttributeSortInput sortInput = new ProductAttributeSortInput().setName(SortEnum.ASC);
        if (StringUtils.isNotEmpty(text)) {
            if (categoryId == null) {
                searchArgs = s -> s.search(text).sort(sortInput).currentPage(currentPage).pageSize(pageSize);
            } else {
                ProductAttributeFilterInput filter = new ProductAttributeFilterInput();
                filter.setCategoryId(new FilterEqualTypeInput().setEq(String.valueOf(categoryId)));
                searchArgs = s -> s.search(text).filter(filter).sort(sortInput).currentPage(currentPage).pageSize(pageSize);
            }
        } else {
            // If the search is empty, we perform a "dummy" search that matches all products
            FilterRangeTypeInput input = new FilterRangeTypeInput().setFrom("");
            ProductAttributeFilterInput filter = new ProductAttributeFilterInput().setPrice(input);
            if (categoryId != null) {
                filter.setCategoryId(new FilterEqualTypeInput().setEq(String.valueOf(categoryId)));
            }
            searchArgs = s -> s.filter(filter).sort(sortInput).currentPage(currentPage).pageSize(pageSize);
        }

        // Main query
        ProductsQueryDefinition queryArgs = q -> q.items(GraphqlQueries.CONFIGURABLE_PRODUCT_QUERY);

        String queryString = Operations.query(query -> query.products(searchArgs, queryArgs)).toString();
        GraphqlResponse<Query, Error> response = execute(queryString, storeView);

        Query query = response.getData();
        List<ProductInterface> products = query.getProducts().getItems();

        LOGGER.debug("Fetched " + (products != null ? products.size() : null) + " products");

        // Populate the products cache
        for (ProductInterface product : products) {
            ArrayKey key = toProductCacheKey(product.getSku(), storeView);
            productCache.put(key, Optional.of(product));
        }

        return products;
    }

    @Override
    public CategoryTree getCategoryTree(Integer categoryId, String storeView) {
        return getCategoriesImpl(categoryId, storeView);
    }

    CategoryTree getCategoriesImpl(Integer categoryId, String storeView) {

        LOGGER.debug("Trying to fetch category " + categoryId);

        // Search parameters
        CategoryArgumentsDefinition searchArgs = q -> q.id(categoryId);

        // Create "recursive" query with depth 5 to fetch category data and children
        // There isn't any better way to build such a query with GraphQL
        CategoryTreeQueryDefinition queryArgs = q -> GraphqlQueries.CATEGORY_TREE_LAMBDA
            .apply(q)
            .children(r -> GraphqlQueries.CATEGORY_TREE_LAMBDA
                .apply(r)
                .children(s -> GraphqlQueries.CATEGORY_TREE_LAMBDA
                    .apply(s)
                    .children(t -> GraphqlQueries.CATEGORY_TREE_LAMBDA
                        .apply(t)
                        .children(u -> GraphqlQueries.CATEGORY_TREE_LAMBDA
                            .apply(u)))));

        String queryString = Operations.query(query -> query.category(searchArgs, queryArgs)).toString();
        GraphqlResponse<Query, Error> response = execute(queryString, storeView);

        Query query = response.getData();
        CategoryTree category = query.getCategory();

        LOGGER.debug("Fetched category " + (category != null ? category.getName() : null));

        return category;
    }

    @Override
    public CategoryProducts getCategoryProducts(Integer categoryId, Integer currentPage, Integer pageSize, String storeView) {
        try {
            ArrayKey key = toCategoryCacheKey(categoryId, currentPage, pageSize, storeView);
            Callable<Optional<CategoryProducts>> loader = () -> getCategoryProductsImpl(categoryId, currentPage, pageSize, storeView);
            return categoryCache.get(key, loader).orElse(null);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    Optional<CategoryProducts> getCategoryProductsImpl(Integer categoryId, Integer currentPage, Integer pageSize,
        String storeView) {

        LOGGER.debug("Trying to fetch products for category " + categoryId);

        // Search parameters
        CategoryArgumentsDefinition argsDef = q -> q.id(categoryId);

        // Main query
        ProductAttributeSortInput sortInput = new ProductAttributeSortInput().setName(SortEnum.ASC);
        CategoryTreeQueryDefinition queryDef = q -> q.products(
            o -> o.sort(sortInput).currentPage(currentPage).pageSize(pageSize),
            p -> p.totalCount().items(GraphqlQueries.CHILD_PRODUCT_QUERY));

        String queryString = Operations.query(query -> query.category(argsDef, queryDef)).toString();
        GraphqlResponse<Query, Error> response = execute(queryString, storeView);

        Query query = response.getData();
        CategoryTree category = query.getCategory();
        List<ProductInterface> products = category.getProducts().getItems();

        LOGGER.debug("Fetched " + products.size() + " products for category " + categoryId);

        return Optional.ofNullable(category.getProducts());
    }

    public GraphqlDataServiceConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public String getIdentifier() {
        return configuration.identifier();
    }

    /**
     * A class that makes it possible to use an <code>Object[]</code> array as map keys.
     * It uses {@link java.util.Arrays#equals(Object[], Object[])} and {@link java.util.Arrays#hashCode(Object[])}
     * to implement <code>equals()</code> and <code>hashCode()</code>.
     */
    static class ArrayKey {

        Object[] parts;

        public ArrayKey(Object... parts) {
            this.parts = parts;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ArrayKey that = (ArrayKey) o;
            return Arrays.equals(parts, that.parts);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(parts);
        }
    }

    private ArrayKey toProductCacheKey(String sku, String storeView) {
        return toCacheKey(sku, StringUtils.defaultString(storeView, MAGENTO_DEFAULT_STORE));
    }

    private ArrayKey toCategoryCacheKey(Integer categoryId, Integer currentPage, Integer pageSize, String storeView) {
        return toCacheKey(categoryId, currentPage, pageSize, StringUtils.defaultString(storeView, MAGENTO_DEFAULT_STORE));
    }

    private ArrayKey toCacheKey(Object... parts) {
        return new ArrayKey(parts);
    }
}
