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

package com.adobe.cq.commerce.graphql.resource;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.graphql.client.GraphqlResponse;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataService;
import com.adobe.cq.commerce.graphql.testing.Utils;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.Error;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;
import com.google.common.collect.Iterators;
import com.google.gson.reflect.TypeToken;

import static com.adobe.cq.commerce.graphql.resource.GraphqlQueryLanguageProvider.VIRTUAL_PRODUCT_QUERY_LANGUAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class GraphqlQueryLanguageProviderTest {

    ResourceMapper<?> resourceMapper;
    GraphqlDataService graphqlDataService;
    GraphqlQueryLanguageProvider<?> queryLanguageProvider;
    ResolveContext ctx;

    @Before
    public void setUp() throws Exception {
        resourceMapper = Mockito.mock(ResourceMapper.class);
        graphqlDataService = Mockito.mock(GraphqlDataService.class);
        ctx = Mockito.mock(ResolveContext.class);
        queryLanguageProvider = new GraphqlQueryLanguageProvider<>(resourceMapper, graphqlDataService, null);
    }

    @Test
    public void testQueryLanguageProvider() throws IOException {
        // The search request coming from com.adobe.cq.commerce.impl.omnisearch.ProductsOmniSearchHandler is serialized in JSON
        String jsonRequest = getResource("commerce-products-omni-search-request.json");

        Mockito.when(graphqlDataService.searchProducts(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());
        queryLanguageProvider.findResources(ctx, jsonRequest, VIRTUAL_PRODUCT_QUERY_LANGUAGE);

        // mock query has limit = 20 and offset = 20 --> so we expect page 2 and size 20
        Mockito.verify(graphqlDataService).searchProducts(eq("gloves"), any(), eq(2), eq(20), any());

        assertNull(queryLanguageProvider.findResources(ctx, jsonRequest, "whatever"));
    }

    @Test
    public void testQueryLanguageProviderWithProducts() throws IOException {
        // The search request coming from com.adobe.cq.commerce.impl.omnisearch.ProductsOmniSearchHandler is serialized in JSON
        String jsonRequest = getResource("commerce-products-omni-search-request2.json");

        String json = getResource("magento-graphql-simple-products.json");
        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> response = QueryDeserializer.getGson().fromJson(json, type);
        List<ProductInterface> products = response.getData().getProducts().getItems();

        Mockito.when(graphqlDataService.searchProducts(any(), any(), any(), any(), any())).thenReturn(products);
        Iterator<Resource> it = queryLanguageProvider.findResources(ctx, jsonRequest, VIRTUAL_PRODUCT_QUERY_LANGUAGE);

        // The mock query has limit = 2 and offset = 4 --> so we expect page 3 and size 2
        Mockito.verify(graphqlDataService).searchProducts(eq("gloves"), any(), eq(3), eq(2), any());

        // The JSON response contains 3 products but the query requested 2 products
        assertEquals(2, Iterators.size(it));
    }

    @Test
    public void testQueryLanguageProviderDefaultPagination() throws IOException {
        // The search request coming from com.adobe.cq.commerce.impl.omnisearch.ProductsOmniSearchHandler is serialized in JSON
        String jsonRequest = getResource("commerce-products-omni-search-request3.json");

        String json = getResource("magento-graphql-simple-products.json");
        Type type = TypeToken.getParameterized(GraphqlResponse.class, Query.class, Error.class).getType();
        GraphqlResponse<Query, Error> response = QueryDeserializer.getGson().fromJson(json, type);
        List<ProductInterface> products = response.getData().getProducts().getItems();

        Mockito.when(graphqlDataService.searchProducts(any(), any(), any(), any(), any())).thenReturn(products);
        Iterator<Resource> it = queryLanguageProvider.findResources(ctx, jsonRequest, VIRTUAL_PRODUCT_QUERY_LANGUAGE);

        // No limit and offset in query --> so we expect page 1 and size 20
        Mockito.verify(graphqlDataService).searchProducts(eq("gloves"), any(), eq(1), eq(20), any());

        // The JSON response contains 3 products and the query requested 20 products
        assertEquals(3, Iterators.size(it));
    }

    @Test
    public void testPagination() {
        Pair<Integer, Integer> pair = GraphqlQueryLanguageProvider.toMagentoPageNumberAndSize(0, 20);
        assertEquals(Integer.valueOf(1), pair.getLeft());
        assertEquals(Integer.valueOf(20), pair.getRight());

        pair = GraphqlQueryLanguageProvider.toMagentoPageNumberAndSize(20, 10);
        assertEquals(Integer.valueOf(3), pair.getLeft());
        assertEquals(Integer.valueOf(10), pair.getRight());

        pair = GraphqlQueryLanguageProvider.toMagentoPageNumberAndSize(20, 11);
        assertEquals(Integer.valueOf(2), pair.getLeft());
        assertEquals(Integer.valueOf(16), pair.getRight());

        pair = GraphqlQueryLanguageProvider.toMagentoPageNumberAndSize(20, 9);
        assertEquals(Integer.valueOf(3), pair.getLeft());
        assertEquals(Integer.valueOf(10), pair.getRight());

        pair = GraphqlQueryLanguageProvider.toMagentoPageNumberAndSize(31, 4);
        assertEquals(Integer.valueOf(7), pair.getLeft());
        assertEquals(Integer.valueOf(5), pair.getRight());

        pair = GraphqlQueryLanguageProvider.toMagentoPageNumberAndSize(5, 6);
        assertEquals(Integer.valueOf(1), pair.getLeft());
        assertEquals(Integer.valueOf(11), pair.getRight());
    }

    private String getResource(String filename) throws IOException {
        return IOUtils.toString(Utils.class.getClassLoader().getResourceAsStream(filename), StandardCharsets.UTF_8);
    }
}
