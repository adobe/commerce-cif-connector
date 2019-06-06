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

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "CIF Catalog Magento GraphQL Configuration Factory")
public @interface GraphqlDataServiceConfiguration {

    public String CQ_CATALOG_IDENTIFIER = "cq:catalogIdentifier";
    public String CONNECTOR_ID_DEFAULT = "default";
    public String STORE_CODE_DEFAULT = "default";

    public static final int MAX_HTTP_CONNECTIONS_DEFAULT = 20;
    public static final boolean ACCEPT_SELF_SIGNED_CERTIFICATES = false;

    // Product caching deafult configuration
    int PRODUCT_CACHING_TIME_DEFAULT = 5;
    int PRODUCT_CACHE_SIZE = 1000;
    int CATEGORY_CACHE_SIZE = 100;
    boolean PRODUCT_CACHING_ENABLED_DEFAULT = true;

    // Catalog caching default configuration
    boolean CATALOG_CACHING_ENABLED_DEFAULT = true;
    int CATALOG_CACHING_TIME_DEFAULT = 60;
    int CATALOG_PAGING_LIMIT = 50;
    boolean CATALOG_CACHING_SCHEDULING = true;

    @AttributeDefinition(
        name = "Magento GraphQL Service Identifier",
        description = "A unique identifier for this configuration, used in the JCR resource property " + CQ_CATALOG_IDENTIFIER
            + " to identify the service. This MUST match the identifier of an already configured GraphQL client.",
        type = AttributeType.STRING,
        required = true)
    String identifier() default CONNECTOR_ID_DEFAULT;

    @AttributeDefinition(
        name = "Magento root category id",
        description = "The ID of the root category.",
        type = AttributeType.INTEGER,
        required = true)
    int rootCategoryId();

    @AttributeDefinition(
        name = "Magento store view",
        description = "The code of the Magento store view.",
        type = AttributeType.STRING)
    String storeCode() default STORE_CODE_DEFAULT;

    @AttributeDefinition(
        name = "Enable/disable product data caching",
        description = "Enables/disables the caching of products data in the connector",
        type = AttributeType.BOOLEAN)
    boolean productCachingEnabled() default PRODUCT_CACHING_ENABLED_DEFAULT;

    @AttributeDefinition(
        name = "Product caching time in minutes",
        description = "The caching time (in minutes) of products data in the connector",
        type = AttributeType.INTEGER)
    int productCachingTimeMinutes() default PRODUCT_CACHING_TIME_DEFAULT;

    @AttributeDefinition(
        name = "Product cache size (= number of products in the cache).",
        description = "The size of the products cache, used when a single product is fetched",
        type = AttributeType.INTEGER)
    int productCachingSize() default PRODUCT_CACHE_SIZE;

    @AttributeDefinition(
        name = "Category cache size (= number of categories in the cache)",
        description = "The size of the categories cache, used when the products of a given category are fetched",
        type = AttributeType.INTEGER)
    int categoryCachingSize() default CATEGORY_CACHE_SIZE;

    @AttributeDefinition(
        name = "Enable/disable catalog caching",
        description = "Enables/disables the caching of the catalog categories structure in the resource resolver mapper",
        type = AttributeType.BOOLEAN)
    boolean catalogCachingEnabled() default CATALOG_CACHING_ENABLED_DEFAULT;

    @AttributeDefinition(
        name = "Catalog caching time in minutes",
        description = "The caching time (in minutes) of the catalog categories structure in the resource resolver mapper",
        type = AttributeType.INTEGER)
    int catalogCachingTimeMinutes() default CATALOG_CACHING_TIME_DEFAULT;

    @AttributeDefinition(
        name = "Enable/disable cache scheduler",
        description = "Enable/disable the periodic update of the catalog cache. Only disable this when testing!",
        type = AttributeType.BOOLEAN)
    boolean catalogCachingSchedulerEnabled() default CATALOG_CACHING_SCHEDULING;
}
