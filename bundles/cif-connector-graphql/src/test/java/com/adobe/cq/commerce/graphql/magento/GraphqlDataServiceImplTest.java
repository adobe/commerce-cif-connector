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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.GraphqlClientConfiguration;
import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceImpl.ArrayKey;
import com.adobe.cq.commerce.graphql.resource.Constants;
import com.adobe.cq.commerce.graphql.testing.Utils;
import com.adobe.cq.commerce.graphql.testing.Utils.GetQueryMatcher;
import com.adobe.cq.commerce.graphql.testing.Utils.HeadersMatcher;
import com.adobe.cq.commerce.magento.graphql.CategoryProducts;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.Error.Location;
import com.google.gson.Gson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class GraphqlDataServiceImplTest {

    private static final String SKU = "meskwielt";
    private static final String NAME = "El Gordo Down Jacket";

    private static final Integer ROOT_CATEGORY_ID = 4;
    private static final String ROOT_CATEGORY_NAME = "Default Category";

    private static final Integer MEN_COATS_CATEGORY_ID = 19;
    private static final String STORE_CODE = "Something";

    private GraphqlDataServiceImpl dataService;
    private GraphqlClient graphqlClient;
    private HttpClient httpClient;
    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        httpClient = Mockito.mock(HttpClient.class);

        GraphqlClientConfiguration graphqlClientConfiguration = mock(GraphqlClientConfiguration.class);
        when(graphqlClientConfiguration.httpMethod()).thenReturn(HttpMethod.POST);
        when(graphqlClientConfiguration.identifier()).thenReturn("default");
        when(graphqlClientConfiguration.maxHttpConnections()).thenReturn(1);

        graphqlClient = new GraphqlClientImpl();
        Utils.activateComponent(graphqlClient, GraphqlClientConfiguration.class, graphqlClientConfiguration);
        Whitebox.setInternalState(graphqlClient, "gson", new Gson());
        Whitebox.setInternalState(graphqlClient, "client", httpClient);

        MockGraphqlDataServiceConfiguration config = new MockGraphqlDataServiceConfiguration();

        dataService = new GraphqlDataServiceImpl();
        dataService.activate(config);
        dataService.bindGraphqlClient(graphqlClient, null);
    }

    private String getResource(String filename) throws IOException {
        return IOUtils.toString(Utils.class.getClassLoader().getResourceAsStream(filename), StandardCharsets.UTF_8);
    }

    @Test
    public void testGetProductBySku() throws Exception {
        // This checks that the generated GraphQL query is what we expect
        // It ensures that all changes made to the GraphQL queries are backed up by tests
        String query = getResource("graphql-queries/product-by-sku.txt");

        Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK, query);

        ProductInterface product = dataService.getProductBySku(SKU, null);
        assertEquals(SKU, product.getSku());
        assertEquals(NAME, product.getName());

        assertTrue(product instanceof ConfigurableProduct);
        ConfigurableProduct configurableProduct = (ConfigurableProduct) product;
        assertEquals(15, configurableProduct.getVariants().size());

        assertNull(dataService.getProductBySku(null, null));
    }

    @Test
    public void testLateClientBinding() throws Exception {
        dataService.unbindGraphqlClient(graphqlClient, null);
        Exception exception = null;
        try {
            dataService.getProductBySku(SKU, null);
        } catch (Exception e) {
            exception = e;
        }
        assertTrue(exception.getCause() instanceof NullPointerException);

        dataService.bindGraphqlClient(graphqlClient, null);
        Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK);
        ProductInterface product = dataService.getProductBySku(SKU, null);
        assertEquals(SKU, product.getSku());
    }

    @Test
    public void testLateClientBindingBeforeActivate() throws Exception {
        dataService = new GraphqlDataServiceImpl();
        dataService.bindGraphqlClient(graphqlClient, null);
        assertNull(dataService.baseClient);

        // unbind and rebind without config, to make sure we don't get a NPE
        dataService.unbindGraphqlClient(graphqlClient, null);
        dataService.bindGraphqlClient(graphqlClient, null);
        assertNull(dataService.baseClient);

        MockGraphqlDataServiceConfiguration config = new MockGraphqlDataServiceConfiguration();
        dataService.activate(config);
        assertEquals(graphqlClient, dataService.baseClient);
    }

    @Test
    public void testWrongClient() throws Exception {
        dataService.unbindGraphqlClient(graphqlClient, null);

        GraphqlClientConfiguration graphqlClientConfiguration = mock(GraphqlClientConfiguration.class);
        when(graphqlClientConfiguration.httpMethod()).thenReturn(HttpMethod.POST);
        when(graphqlClientConfiguration.identifier()).thenReturn("wrongid");
        when(graphqlClientConfiguration.maxHttpConnections()).thenReturn(1);

        GraphqlClientImpl wrongClient = new GraphqlClientImpl();
        Utils.activateComponent(wrongClient, GraphqlClientConfiguration.class, graphqlClientConfiguration);
        dataService.bindGraphqlClient(wrongClient, null);

        Exception exception = null;
        try {
            dataService.getProductBySku(SKU, null);
        } catch (Exception e) {
            exception = e;
        }
        assertTrue(exception.getCause() instanceof NullPointerException);

        dataService.unbindGraphqlClient(wrongClient, null);
    }

    @Test
    public void testNoProductBySku() throws Exception {
        Utils.setupHttpResponse("magento-graphql-no-product.json", httpClient, HttpStatus.SC_OK);
        assertNull(dataService.getProductBySku(SKU, null));

        // This would fail if the product was not properly cached because the mocked HTTP response was already consumed
        assertNull(dataService.getProductBySku(SKU, null));
    }

    @Test
    public void testCachingConsidersStoreView() throws Exception {
        dataService = Mockito.spy(dataService);

        // The caching is done on a store basis, so we expect one call to getProductBySkuImpl() by store view

        Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK);
        assertNotNull(dataService.getProductBySku(SKU, "store1"));

        Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK);
        assertNotNull(dataService.getProductBySku(SKU, "store2"));
        assertNotNull(dataService.getProductBySku(SKU, "store2")); // Data comes from cache

        Mockito.verify(dataService).getProductBySkuImpl(SKU, "store1");
        Mockito.verify(dataService).getProductBySkuImpl(SKU, "store2");

        // The caching is done on a store basis, so we expect one call to getCategoryProductsImpl() by store view

        Utils.setupHttpResponse("magento-graphql-category-products.json", httpClient, HttpStatus.SC_OK);
        assertNotNull(dataService.getCategoryProducts(MEN_COATS_CATEGORY_ID, 1, 10, "store1"));

        Utils.setupHttpResponse("magento-graphql-category-products.json", httpClient, HttpStatus.SC_OK);
        assertNotNull(dataService.getCategoryProducts(MEN_COATS_CATEGORY_ID, 1, 10, "store2"));
        assertNotNull(dataService.getCategoryProducts(MEN_COATS_CATEGORY_ID, 1, 10, "store2")); // Data comes from cache

        Mockito.verify(dataService).getCategoryProductsImpl(MEN_COATS_CATEGORY_ID, 1, 10, "store1");
        Mockito.verify(dataService).getCategoryProductsImpl(MEN_COATS_CATEGORY_ID, 1, 10, "store2");
    }

    @Test
    public void testDisabledCaches() throws Exception {
        MockGraphqlDataServiceConfiguration config = new MockGraphqlDataServiceConfiguration();
        config.setProductCachingEnabled(false);

        dataService = new GraphqlDataServiceImpl();
        dataService.activate(config);
        dataService.bindGraphqlClient(graphqlClient, null);

        dataService = Mockito.spy(dataService);

        // The caching is disabled so we expect 2 calls to getProductBySkuImpl()

        Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK);
        assertNotNull(dataService.getProductBySku(SKU, "store1"));

        Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK);
        assertNotNull(dataService.getProductBySku(SKU, "store1"));

        Mockito.verify(dataService, times(2)).getProductBySkuImpl(SKU, "store1");

        // The caching is disabled so we expect 2 calls to getCategoryProductsImpl()

        Utils.setupHttpResponse("magento-graphql-category-products.json", httpClient, HttpStatus.SC_OK);
        assertNotNull(dataService.getCategoryProducts(MEN_COATS_CATEGORY_ID, 1, 10, "store1"));

        Utils.setupHttpResponse("magento-graphql-category-products.json", httpClient, HttpStatus.SC_OK);
        assertNotNull(dataService.getCategoryProducts(MEN_COATS_CATEGORY_ID, 1, 10, "store1"));

        Mockito.verify(dataService, times(2)).getCategoryProductsImpl(MEN_COATS_CATEGORY_ID, 1, 10, "store1");
    }

    @SuppressWarnings("unlikely-arg-type")
    @Test
    public void testCacheKeys() {
        ArrayKey key1 = new ArrayKey("test", new Integer(1), null);
        ArrayKey key2 = new ArrayKey(new String("test"), Integer.valueOf("1"), null);
        ArrayKey key3 = new ArrayKey("test", new Integer(1));
        ArrayKey key4 = new ArrayKey("test", "1", null);

        assertTrue(key1.equals(key1));
        assertTrue(key1.hashCode() == key1.hashCode());

        assertTrue(key1.equals(key2));
        assertTrue(key1.hashCode() == key2.hashCode());

        assertFalse(key1.equals(key3));
        assertFalse(key1.hashCode() == key3.hashCode());

        assertFalse(key1.equals(key4));
        assertFalse(key1.hashCode() == key4.hashCode());

        assertFalse(key1.equals(null));

        assertFalse(key1.equals("test1null"));
        assertFalse(key1.hashCode() == "test1null".hashCode());
    }

    @Test(expected = RuntimeException.class)
    public void testGetProductBySkuError() throws Exception {
        Utils.setupHttpResponse("magento-graphql-error.json", httpClient, HttpStatus.SC_OK);
        dataService.getProductBySku(SKU, null);
    }

    @Test(expected = RuntimeException.class)
    public void testGetCategoryByIdError() throws Exception {
        Utils.setupHttpResponse("magento-graphql-error.json", httpClient, HttpStatus.SC_OK);
        dataService.getCategoryById(1, null);
    }

    @Test
    public void testGetCategoryByIdNull() throws Exception {
        Utils.setupHttpResponse("magento-graphql-error.json", httpClient, HttpStatus.SC_OK);
        assertNull(dataService.getCategoryById(null, null));
    }

    @Test
    public void testGetCategoryByPath() throws Exception {
        // This checks that the generated GraphQL query is what we expect
        // It ensures that all changes made to the GraphQL queries are backed up by tests
        String query = getResource("graphql-queries/categorylist-by-urlkey.txt");

        Utils.setupHttpResponse("magento-graphql-categorylist-dresses.json", httpClient, HttpStatus.SC_OK, query);

        CategoryTree category = dataService.getCategoryByPath("venia-dresses", null);
        assertEquals(37, category.getId().intValue());
        assertEquals("Dresses", category.getName());

        assertNull(dataService.getCategoryByPath(null, null));
    }

    @Test(expected = RuntimeException.class)
    public void testGetCategoryByPathError() throws Exception {
        Utils.setupHttpResponse("magento-graphql-error.json", httpClient, HttpStatus.SC_OK);
        dataService.getCategoryByPath(SKU, null);
    }

    @Test
    public void testGetCategoryByPathEmpty() throws Exception {
        Utils.setupHttpResponse("magento-graphql-error.json", httpClient, HttpStatus.SC_OK);
        assertNull(dataService.getCategoryByPath("/", null));
    }

    @Test
    public void testSearchProducts() throws Exception {
        // This checks that the generated GraphQL query is what we expect
        // It ensures that all changes made to the GraphQL queries are backed up by tests
        String query = getResource("graphql-queries/products-search.txt");

        Utils.setupHttpResponse("magento-graphql-products-search.json", httpClient, HttpStatus.SC_OK, query);
        List<ProductInterface> products = dataService.searchProducts("coats", null, 0, 3, null);
        assertEquals(3, products.size());
    }

    @Test
    public void testSearchProductsForCategory() throws Exception {
        // This checks that the generated GraphQL query is what we expect
        // It ensures that all changes made to the GraphQL queries are backed up by tests
        String query = getResource("graphql-queries/products-search-for-category.txt");

        Utils.setupHttpResponse("magento-graphql-products-search.json", httpClient, HttpStatus.SC_OK, query);
        List<ProductInterface> products = dataService.searchProducts("coats", 11, 0, 3, null);
        assertEquals(3, products.size());
    }

    @Test
    public void testEmptySearchProducts() throws Exception {
        // This checks that the generated GraphQL query is what we expect
        // It ensures that all changes made to the GraphQL queries are backed up by tests
        String query = getResource("graphql-queries/products-empty-search.txt");

        Utils.setupHttpResponse("magento-graphql-products-search.json", httpClient, HttpStatus.SC_OK, query);
        List<ProductInterface> products = dataService.searchProducts(null, null, 0, 3, null);
        assertEquals(3, products.size());
    }

    @Test
    public void testEmptySearchProductsForCategory() throws Exception {
        // This checks that the generated GraphQL query is what we expect
        // It ensures that all changes made to the GraphQL queries are backed up by tests
        String query = getResource("graphql-queries/products-empty-search-for-category.txt");

        Utils.setupHttpResponse("magento-graphql-products-search.json", httpClient, HttpStatus.SC_OK, query);
        List<ProductInterface> products = dataService.searchProducts(null, 11, 0, 3, null);
        assertEquals(3, products.size());
    }

    @Test
    public void testSearchCategories() throws Exception {
        // This checks that the generated GraphQL query is what we expect
        // It ensures that all changes made to the GraphQL queries are backed up by tests
        String query = getResource("graphql-queries/category-search.txt");

        Utils.setupHttpResponse("magento-graphql-category-search.json", httpClient, HttpStatus.SC_OK, query);
        List<CategoryTree> categories = dataService.searchCategories("1.2.3.4.5", null, 0, 3, null);
        assertEquals(2, categories.size());
    }

    @Test
    public void testGetCategoryProducts() throws Exception {
        // This checks that the generated GraphQL query is what we expect
        // It ensures that all changes made to the GraphQL queries are backed up by tests
        String query = getResource("graphql-queries/category-products.txt");

        Utils.setupHttpResponse("magento-graphql-category-products.json", httpClient, HttpStatus.SC_OK, query);
        CategoryProducts categoryProducts = dataService.getCategoryProducts(MEN_COATS_CATEGORY_ID, 1, 10, null);
        assertEquals(5, categoryProducts.getTotalCount().intValue());
        assertEquals(5, categoryProducts.getItems().size());
    }

    @Test
    public void testMagentoError() throws Exception {
        Utils.setupHttpResponse("magento-graphql-error.json", httpClient, HttpStatus.SC_OK);
        GraphqlResponse<Query, Error> response = dataService.execute("{dummy}", null);

        assertEquals(1, response.getErrors().size());
        Error error = response.getErrors().get(0);
        assertEquals("Cannot query field \"categoriess\" on type \"ProductInterface\".", error.getMessage());
        assertEquals("graphql", error.getCategory());

        assertEquals(1, error.getLocations().size());
        Location location = error.getLocations().get(0);
        assertEquals(23, location.getLine().intValue());
        assertEquals(9, location.getColumn().intValue());
    }

    @Test
    public void testHttpError() throws Exception {
        Utils.setupHttpResponse("magento-graphql-error.json", httpClient, HttpStatus.SC_SERVICE_UNAVAILABLE);
        exceptionRule.expect(RuntimeException.class);
        exceptionRule.expectMessage("GraphQL query failed with response code 503");
        dataService.execute("{dummy}", null);
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidResponse() throws Exception {
        Utils.setupHttpResponse("sample-graphql-generic-response.json", httpClient, HttpStatus.SC_OK);
        dataService.getProductBySku(SKU, null);
    }

    @Test
    public void testStoreCodeHeader() throws Exception {
        Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK);

        dataService.execute("{dummy}", STORE_CODE);

        // Check that the HTTP client is sending the custom request headers
        List<Header> headers = Collections.singletonList(new BasicHeader(Constants.STORE_HEADER, STORE_CODE));
        HeadersMatcher matcher = new HeadersMatcher(headers);
        Mockito.verify(httpClient, Mockito.times(1)).execute(Mockito.argThat(matcher));
    }

    @Test
    public void testGetHttpMethod() throws Exception {
        // This checks that the generated GraphQL query is what we expect
        // It ensures that all changes made to the GraphQL queries are backed up by tests
        String query = getResource("graphql-queries/product-by-sku.txt");

        Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK, query);

        dataService.requestOptions.withHttpMethod(HttpMethod.GET);
        ProductInterface product = dataService.getProductBySku(SKU, null);
        assertEquals(SKU, product.getSku());
        assertEquals(NAME, product.getName());

        // Check that the HTTP client is called with the right method
        GetQueryMatcher matcher = new GetQueryMatcher(new GraphqlRequest(query));
        Mockito.verify(httpClient, Mockito.times(1)).execute(Mockito.argThat(matcher));
    }

}
