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
import com.adobe.cq.commerce.magento.graphql.CategoryProducts;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.ProductSortInput;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
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

    // We cannot extend GraphqlClientImpl because it's not OSGi-exported so we use "object composition"
    protected GraphqlClient baseClient;
    protected RequestOptions requestOptions;
    private GraphqlDataServiceConfiguration configuration;

    // We maintain some caches to speed up all lookups
    private Cache<String, Optional<ProductInterface>> productCache;
    private Cache<String, Optional<CategoryProducts>> categoryCache;

    protected Map<String, GraphqlClient> clients = new ConcurrentHashMap<>();

    @Reference(
        service = GraphqlClient.class,
        bind = "bindGraphqlClient",
        unbind = "unbindGraphqlClient",
        cardinality = ReferenceCardinality.AT_LEAST_ONE,
        policy = ReferencePolicy.DYNAMIC)
    protected void bindGraphqlClient(GraphqlClient graphqlClient, Map<?, ?> properties) {
        String identifier = graphqlClient.getIdentifier();
        LOGGER.info("Registering GraphqlClient '{}'", identifier);
        clients.put(identifier, graphqlClient);
    }

    protected void unbindGraphqlClient(GraphqlClient graphqlClient, Map<?, ?> properties) {
        String identifier = graphqlClient.getIdentifier();
        LOGGER.info("De-registering GraphqlClient '{}'", identifier);
        clients.remove(identifier);
    }

    @Activate
    public void activate(GraphqlDataServiceConfiguration conf) throws Exception {
        baseClient = clients.get(conf.identifier());
        if (baseClient == null) {
            throw new RuntimeException("Cannot find GraphqlClient with identifier '" + conf.identifier() + "'");
        }

        configuration = conf;

        if (configuration.productCachingEnabled()) {

            // Used when a single product is being fetched
            productCache = CacheBuilder.newBuilder()
                .maximumSize(configuration.productCachingSize())
                .expireAfterWrite(configuration.productCachingTimeMinutes(), TimeUnit.MINUTES)
                .build();

            // Used when the products of a given category are being fetched
            categoryCache = CacheBuilder.newBuilder()
                .maximumSize(configuration.categoryCachingSize())
                .expireAfterWrite(configuration.productCachingTimeMinutes(), TimeUnit.MINUTES)
                .build();
        }

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
            return productCache.get(sku, () -> getProductBySkuImpl(sku, storeView)).orElse(null);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<ProductInterface> getProductBySkuImpl(String sku, String storeView) {

        LOGGER.debug("Trying to fetch product " + sku);

        // Search parameters
        FilterTypeInput input = new FilterTypeInput().setEq(sku);
        ProductFilterInput filter = new ProductFilterInput().setSku(input);
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
        if (StringUtils.isNotEmpty(text)) {
            ProductSortInput sortInput = new ProductSortInput().setSku(SortEnum.ASC);
            if (categoryId == null) {
                searchArgs = s -> s.search(text).sort(sortInput).currentPage(currentPage).pageSize(pageSize);
            } else {
                ProductFilterInput filter = new ProductFilterInput();
                filter.setCategoryId(new FilterTypeInput().setEq(String.valueOf(categoryId)));
                searchArgs = s -> s.search(text).filter(filter).sort(sortInput).currentPage(currentPage).pageSize(pageSize);
            }
        } else {
            // If the search is empty, we perform a "dummy" search (sku != null) that matches all products
            FilterTypeInput input = new FilterTypeInput().setNotnull("");
            ProductFilterInput filter = new ProductFilterInput().setSku(input);
            if (categoryId != null) {
                filter.setCategoryId(new FilterTypeInput().setEq(String.valueOf(categoryId)));
            }
            searchArgs = s -> s.filter(filter).currentPage(currentPage).pageSize(pageSize);
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
            productCache.put(product.getSku(), Optional.of(product));
        }

        return products;
    }

    @Override
    public CategoryTree getCategoryTree(Integer categoryId, String storeView) {
        return getCategoriesImpl(categoryId, storeView);
    }

    private CategoryTree getCategoriesImpl(Integer categoryId, String storeView) {

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

    private String toCategoryCacheKey(Integer categoryId, Integer currentPage, Integer pageSize) {
        return StringUtils.joinWith("-", categoryId, currentPage, pageSize);
    }

    @Override
    public CategoryProducts getCategoryProducts(Integer categoryId, Integer currentPage, Integer pageSize, String storeView) {
        try {
            String cacheKey = toCategoryCacheKey(categoryId, currentPage, pageSize);
            Callable<? extends Optional<CategoryProducts>> loader = () -> getCategoryProductsImpl(categoryId, currentPage, pageSize,
                storeView);
            return categoryCache.get(cacheKey, loader).orElse(null);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<CategoryProducts> getCategoryProductsImpl(Integer categoryId, Integer currentPage, Integer pageSize,
        String storeView) {

        LOGGER.debug("Trying to fetch products for category " + categoryId);

        // Search parameters
        CategoryArgumentsDefinition argsDef = q -> q.id(categoryId);

        // Main query
        CategoryTreeQueryDefinition queryDef = q -> q.products(
            o -> o.currentPage(currentPage).pageSize(pageSize),
            p -> p.totalCount().items(GraphqlQueries.CONFIGURABLE_PRODUCT_QUERY));

        String queryString = Operations.query(query -> query.category(argsDef, queryDef)).toString();
        GraphqlResponse<Query, Error> response = execute(queryString, storeView);

        Query query = response.getData();
        CategoryTree category = query.getCategory();
        List<ProductInterface> products = category.getProducts().getItems();

        LOGGER.debug("Fetched " + products.size() + " products for category " + categoryId);

        // Populate the products cache
        for (ProductInterface product : products) {
            productCache.put(product.getSku(), Optional.of(product));
        }

        return Optional.ofNullable(category.getProducts());
    }

    public GraphqlDataServiceConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public String getIdentifier() {
        return configuration.identifier();
    }
}
