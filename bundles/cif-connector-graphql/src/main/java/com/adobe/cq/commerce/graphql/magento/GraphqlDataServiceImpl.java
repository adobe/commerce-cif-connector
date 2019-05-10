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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
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
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.CategoryTreeQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.FilterTypeInput;
import com.adobe.cq.commerce.magento.graphql.Operations;
import com.adobe.cq.commerce.magento.graphql.ProductFilterInput;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Products;
import com.adobe.cq.commerce.magento.graphql.ProductsQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.QueryQuery.CategoryArgumentsDefinition;
import com.adobe.cq.commerce.magento.graphql.QueryQuery.ProductsArgumentsDefinition;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Component(service = GraphqlDataService.class)
@Designate(ocd = GraphqlDataServiceConfiguration.class, factory = true)
public class GraphqlDataServiceImpl implements GraphqlDataService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlDataServiceImpl.class);

    // We cannot extend GraphqlClientImpl because it's not OSGi-exported so we use "object composition"
    protected GraphqlClient baseClient;
    private GraphqlDataServiceConfiguration configuration;

    // We maintain some caches to speed up all lookups
    private LoadingCache<String, ProductInterface> productCache;
    private LoadingCache<Integer, List<ProductInterface>> categoryCache;

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
                .build(CacheLoader.from(sku -> getProductBySkuImpl(sku)));

            // Used when the products of a given category are being fetched
            categoryCache = CacheBuilder.newBuilder()
                .maximumSize(configuration.categoryCachingSize())
                .expireAfterWrite(configuration.productCachingTimeMinutes(), TimeUnit.MINUTES)
                .build(CacheLoader.from(id -> getCategoryProductsImpl(id)));
        }
    }

    protected GraphqlResponse<Query, Error> execute(String query) {
        return baseClient.execute(new GraphqlRequest(query), Query.class, Error.class, QueryDeserializer.getGson());
    }

    @Override
    public ProductInterface getProductBySku(String sku) {
        try {
            return productCache.get(sku);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private ProductInterface getProductBySkuImpl(String sku) {

        LOGGER.debug("Trying to fetch product " + sku);

        // Search parameters
        FilterTypeInput input = new FilterTypeInput().setEq(sku);
        ProductFilterInput filter = new ProductFilterInput().setSku(input);
        ProductsArgumentsDefinition searchArgs = s -> s.filter(filter);

        // Main query
        ProductsQueryDefinition queryArgs = q -> q.items(GraphqlQueries.CONFIGURABLE_PRODUCT_QUERY);

        String queryString = Operations.query(query -> query.products(searchArgs, queryArgs)).toString();
        GraphqlResponse<Query, Error> response = execute(queryString);

        Query query = response.getData();
        Products productsQuery = query.getProducts();
        List<ProductInterface> products = productsQuery.getItems();
        ProductInterface product = products.size() > 0 ? products.get(0) : null;

        LOGGER.debug("Fetched product " + (product != null ? product.getName() : null));

        return product;
    }

    @Override
    public List<ProductInterface> searchProducts(String text, Integer currentPage, Integer pageSize) {
        return searchProductsImpl(text, currentPage, pageSize);
    }

    private List<ProductInterface> searchProductsImpl(String text, Integer currentPage, Integer pageSize) {

        LOGGER.debug("Performing product search with '" + text + "'");

        // Search parameters
        ProductsArgumentsDefinition searchArgs;
        if (StringUtils.isNotEmpty(text)) {
            searchArgs = s -> s.search(text).currentPage(currentPage).pageSize(pageSize);
        } else {
            // If the search is empty, we perform a "dummy" search (sku != null) that matches all products
            FilterTypeInput input = new FilterTypeInput().setNotnull("");
            ProductFilterInput filter = new ProductFilterInput().setSku(input);
            searchArgs = s -> s.filter(filter).currentPage(currentPage).pageSize(pageSize);
        }

        // Main query
        ProductsQueryDefinition queryArgs = q -> q.items(GraphqlQueries.CONFIGURABLE_PRODUCT_QUERY);

        String queryString = Operations.query(query -> query.products(searchArgs, queryArgs)).toString();
        GraphqlResponse<Query, Error> response = execute(queryString);

        Query query = response.getData();
        List<ProductInterface> products = query.getProducts().getItems();

        LOGGER.debug("Fetched " + (products != null ? products.size() : null) + " products");

        return products;
    }

    @Override
    public CategoryTree getCategoryTree(Integer categoryId) {
        return getCategoriesImpl(categoryId);
    }

    private CategoryTree getCategoriesImpl(Integer categoryId) {

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
        GraphqlResponse<Query, Error> response = execute(queryString);

        Query query = response.getData();
        CategoryTree category = query.getCategory();

        LOGGER.debug("Fetched category " + (category != null ? category.getName() : null));

        return category;
    }

    @Override
    public List<ProductInterface> getCategoryProducts(Integer categoryId) {
        try {
            return categoryCache.get(categoryId);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private List<ProductInterface> getCategoryProductsImpl(Integer categoryId) {

        LOGGER.debug("Trying to fetch products for category " + categoryId);

        // Search parameters
        CategoryArgumentsDefinition searchArgs = q -> q.id(categoryId);

        // Main query
        CategoryTreeQueryDefinition queryArgs = q -> q
            .products(p -> p
                .items(GraphqlQueries.CONFIGURABLE_PRODUCT_QUERY));

        String queryString = Operations.query(query -> query.category(searchArgs, queryArgs)).toString();
        GraphqlResponse<Query, Error> response = execute(queryString);

        Query query = response.getData();
        CategoryTree category = query.getCategory();
        List<ProductInterface> products = category.getProducts().getItems();

        LOGGER.debug("Fetched " + products.size() + " products for category " + categoryId);

        // Populate the products cache
        for (ProductInterface product : products) {
            productCache.put(product.getSku(), product);
        }

        return products;
    }

    public GraphqlDataServiceConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public String getIdentifier() {
        return configuration.identifier();
    }
}
