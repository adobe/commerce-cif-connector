/*******************************************************************************
 *
 *
 *      Copyright 2020 Adobe. All rights reserved.
 *      This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License. You may obtain a copy
 *      of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software distributed under
 *      the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *      OF ANY KIND, either express or implied. See the License for the specific language
 *      governing permissions and limitations under the License.
 *
 ******************************************************************************/

package com.adobe.cq.commerce.virtual.catalog.data;

import org.apache.commons.lang3.StringUtils;

/**
 * Constants related to the CIF Connector
 */
public abstract class Constants {

    public static final String CONF_ROOT = "/conf";

    public static final String CONF_CONTAINER_BUCKET_NAME = "settings";

    public static final String CLOUDCONFIGS_BUCKET_NAME = "cloudconfigs";

    public static final String COMMERCE_CONFIG_NAME = "commerce";

    public static final String COMMERCE_BUCKET_PATH = StringUtils.join(new String[] { CONF_CONTAINER_BUCKET_NAME, CLOUDCONFIGS_BUCKET_NAME,
        COMMERCE_CONFIG_NAME }, "/");

    public static final String CONFIGURATION_NAME = StringUtils.join(new String[] { CLOUDCONFIGS_BUCKET_NAME, COMMERCE_CONFIG_NAME }, "/");

    public static final String PN_CONF = "cq:conf";
    public static final String PN_MAGENTO_ROOT_CATEGORY_ID = "magentoRootCategoryId";
    public static final String PN_CATALOG_PROVIDER_FACTORY = "cq:catalogDataResourceProviderFactory";
    public static final String PN_CATALOG_IDENTIFIER = "cq:catalogIdentifier";
    public static final String PN_GRAPHQL_CLIENT = "cq:graphqlClient";
    public static final String PN_MAGENTO_STORE = "magentoStore";
    public static final String PN_CATALOG_PATH = "cq:catalogPath";

    private Constants() {

    }

}
