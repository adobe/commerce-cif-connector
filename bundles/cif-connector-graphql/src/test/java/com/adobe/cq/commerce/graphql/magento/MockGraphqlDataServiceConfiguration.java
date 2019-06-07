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

import java.lang.annotation.Annotation;

public class MockGraphqlDataServiceConfiguration implements Annotation, GraphqlDataServiceConfiguration {

    public static final int ROOT_CATEGORY_ID = 4;

    private String storeCode;

    @Override
    public String identifier() {
        return GraphqlDataServiceConfiguration.CONNECTOR_ID_DEFAULT;
    }

    @Override
    public int rootCategoryId() {
        return ROOT_CATEGORY_ID;
    }

    @Override
    public String storeCode() {
        return storeCode != null ? storeCode : GraphqlDataServiceConfiguration.STORE_CODE_DEFAULT;
    }

    @Override
    public boolean productCachingEnabled() {
        return GraphqlDataServiceConfiguration.PRODUCT_CACHING_ENABLED_DEFAULT;
    }

    @Override
    public int productCachingTimeMinutes() {
        return GraphqlDataServiceConfiguration.PRODUCT_CACHING_TIME_DEFAULT;
    }

    @Override
    public boolean catalogCachingEnabled() {
        return GraphqlDataServiceConfiguration.CATALOG_CACHING_ENABLED_DEFAULT;
    }

    @Override
    public int productCachingSize() {
        return GraphqlDataServiceConfiguration.PRODUCT_CACHE_SIZE;
    }

    @Override
    public int categoryCachingSize() {
        return GraphqlDataServiceConfiguration.CATEGORY_CACHE_SIZE;
    }

    @Override
    public int catalogCachingTimeMinutes() {
        return GraphqlDataServiceConfiguration.CATALOG_CACHING_TIME_DEFAULT;
    }

    @Override
    public boolean catalogCachingSchedulerEnabled() {
        return GraphqlDataServiceConfiguration.CATALOG_CACHING_SCHEDULING;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return GraphqlDataServiceConfiguration.class;
    }

    void setStoreCode(String storeCode) {
        this.storeCode = storeCode;
    }
}
