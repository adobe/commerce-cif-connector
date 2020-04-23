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

    private Boolean productCachingEnabled;

    @Override
    public String identifier() {
        return GraphqlDataServiceConfiguration.CONNECTOR_ID_DEFAULT;
    }

    @Override
    public boolean productCachingEnabled() {
        return productCachingEnabled != null ? productCachingEnabled : GraphqlDataServiceConfiguration.PRODUCT_CACHING_ENABLED_DEFAULT;
    }

    @Override
    public int productCachingTimeMinutes() {
        return GraphqlDataServiceConfiguration.PRODUCT_CACHING_TIME_DEFAULT;
    }

    @Override
    public boolean categoryCachingEnabled() {
        return GraphqlDataServiceConfiguration.CATEGORY_CACHING_ENABLED_DEFAULT;
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
    public int categoryCachingTimeMinutes() {
        return GraphqlDataServiceConfiguration.CATEGORY_CACHING_TIME_DEFAULT;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return GraphqlDataServiceConfiguration.class;
    }

    public void setProductCachingEnabled(boolean productCachingEnabled) {
        this.productCachingEnabled = productCachingEnabled;
    }
}
