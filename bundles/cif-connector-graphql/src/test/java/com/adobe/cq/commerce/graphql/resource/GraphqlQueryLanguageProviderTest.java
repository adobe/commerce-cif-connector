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
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.graphql.magento.GraphqlDataService;
import com.adobe.cq.commerce.graphql.testing.Utils;

import static com.adobe.cq.commerce.graphql.resource.GraphqlQueryLanguageProvider.VIRTUAL_PRODUCT_QUERY_LANGUAGE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class GraphqlQueryLanguageProviderTest {

    ResourceMapper<?> resourceMapper;
    GraphqlDataService graphqlDataService;
    GraphqlQueryLanguageProvider<?> queryLanguageProvider;

    @Before
    public void setUp() throws Exception {
        resourceMapper = Mockito.mock(ResourceMapper.class);
        graphqlDataService = Mockito.mock(GraphqlDataService.class);
        queryLanguageProvider = new GraphqlQueryLanguageProvider<>(resourceMapper, graphqlDataService);

        Mockito.when(graphqlDataService.searchProducts(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Collections.emptyList());
    }

    @Test
    public void testQueryLanguageProvider() throws IOException {
        // The search request coming from com.adobe.cq.commerce.impl.omnisearch.ProductsOmniSearchHandler is serialized in JSON
        String jsonRequest = getResource("commerce-products-omni-search-request.json");

        queryLanguageProvider.findResources(null, jsonRequest, VIRTUAL_PRODUCT_QUERY_LANGUAGE);

        // mock query has limit = 20 and offset = 20 --> so we expect page 2 and limit 20
        Mockito.verify(graphqlDataService, Mockito.times(1)).searchProducts("gloves", Integer.valueOf(2), Integer.valueOf(20));

        assertNull(queryLanguageProvider.findResources(null, jsonRequest, "whatever"));
    }

    @Test
    public void testQueryLanguageProvider2() throws IOException {
        // The search request coming from com.adobe.cq.commerce.impl.omnisearch.ProductsOmniSearchHandler is serialized in JSON
        String jsonRequest = getResource("commerce-products-omni-search-request2.json");

        queryLanguageProvider.findResources(null, jsonRequest, VIRTUAL_PRODUCT_QUERY_LANGUAGE);

        // mock query has limit = 10 and offset = 0 --> so we expect page 1 and limit 10
        Mockito.verify(graphqlDataService, Mockito.times(1)).searchProducts("gloves", Integer.valueOf(1), Integer.valueOf(10));
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
