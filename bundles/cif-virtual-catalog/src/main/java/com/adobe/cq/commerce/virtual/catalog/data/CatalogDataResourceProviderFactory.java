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

package com.adobe.cq.commerce.virtual.catalog.data;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ResourceProvider;

/**
 * Factory for creating catalog data resource providers.
 */
public interface CatalogDataResourceProviderFactory<T> {
    /**
     * Property name for factory service identifier for OSGi.
     */
    String PROPERTY_FACTORY_SERVICE_ID = "catalogDataResourceProviderFactory";

    /**
     * Property name for factory service identifier in root folder.
     */
    String PROPERTY_FACTORY_ID = "cq:" + PROPERTY_FACTORY_SERVICE_ID;

    /**
     * Creates a new resource provider instance on each call.
     *
     * @param root the Resource for the root folder of the resource provider
     * @return a new ResourceProvider instance
     */
    ResourceProvider<T> createResourceProvider(Resource root);
}
