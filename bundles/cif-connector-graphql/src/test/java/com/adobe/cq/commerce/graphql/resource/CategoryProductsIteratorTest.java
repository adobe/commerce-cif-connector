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
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataService;
import com.adobe.cq.commerce.graphql.testing.Utils;
import com.adobe.cq.commerce.magento.graphql.Query;
import com.adobe.cq.commerce.magento.graphql.gson.QueryDeserializer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CategoryProductsIteratorTest {

    private static final String STORE_VIEW = "default";
    private static final Integer CATEGORY_ID = Integer.valueOf(1);
    private static final String CATALOG_ROOT_PATH = "/var/commerce/products/magento-graphql";

    private GraphqlDataService graphqlDataService;
    private Resource category;

    @Before
    public void setUp() throws IOException {
        category = mock(Resource.class);
        ValueMap valueMap = new ValueMapDecorator(Collections.singletonMap(Constants.CIF_ID, CATEGORY_ID));
        when(category.getValueMap()).thenReturn(valueMap);
        when(category.getPath()).thenReturn(CATALOG_ROOT_PATH);
        
        graphqlDataService = mock(GraphqlDataService.class);

        String json1 = getResource("magento-graphql-category-products-page1.json");
        Query query1 = QueryDeserializer.getGson().fromJson(json1, Query.class);
        when(graphqlDataService.getCategoryProducts(CATEGORY_ID, 1, 3, STORE_VIEW)).thenReturn(query1.getCategory().getProducts());

        String json2 = getResource("magento-graphql-category-products-page2.json");
        Query query2 = QueryDeserializer.getGson().fromJson(json2, Query.class);
        when(graphqlDataService.getCategoryProducts(CATEGORY_ID, 2, 3, STORE_VIEW)).thenReturn(query2.getCategory().getProducts());

        String json3 = getResource("magento-graphql-category-products-page3.json");
        Query query3 = QueryDeserializer.getGson().fromJson(json3, Query.class);
        when(graphqlDataService.getCategoryProducts(CATEGORY_ID, 3, 3, STORE_VIEW)).thenReturn(query3.getCategory().getProducts());
    }

    @Test
    public void testIterator() {
        CategoryProductsIterator it = new CategoryProductsIterator(category, graphqlDataService, 3, STORE_VIEW);

        int count = 0;
        while (it.hasNext()) {
            count++;
            Resource product = it.next();
            Assert.assertEquals(CATALOG_ROOT_PATH + "/product-" + count, product.getPath());
        }
        Assert.assertEquals(8, count);
        Mockito.verify(graphqlDataService, Mockito.times(3)).getCategoryProducts(any(), any(), any(), any());
    }

    @Test
    public void testBackendError() throws IOException {

        // We test that the iterator properly stops if the backend doesn't return more products
        String json3 = getResource("magento-graphql-category-products-page3.json");
        Query query3 = QueryDeserializer.getGson().fromJson(json3, Query.class);
        query3.getCategory().getProducts().setItems(Collections.emptyList());
        when(graphqlDataService.getCategoryProducts(CATEGORY_ID, 3, 3, STORE_VIEW)).thenReturn(query3.getCategory().getProducts());

        CategoryProductsIterator it = new CategoryProductsIterator(category, graphqlDataService, 3, STORE_VIEW);

        int count = 0;
        while (it.hasNext()) {
            count++;
            Resource product = it.next();
            Assert.assertEquals(CATALOG_ROOT_PATH + "/product-" + count, product.getPath());
        }
        Assert.assertEquals(6, count);
        Mockito.verify(graphqlDataService, Mockito.times(3)).getCategoryProducts(any(), any(), any(), any());
    }

    private String getResource(String filename) throws IOException {
        return IOUtils.toString(Utils.class.getClassLoader().getResourceAsStream(filename), StandardCharsets.UTF_8);
    }
}
