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

import java.util.Collection;
import java.util.Map;

/**
 * Service for getting all catalog identifiers (projects in UI) for all cloud commerce provider implementations.
 */
public interface CatalogIdentifierService {

    /**
     * Get all catalog identifiers for all cloud commerce provider implementations.
     *
     * @return A map where the keys are the names of the commerce providers and the values are a collection of all
     * catalog identifiers for that specific commerce provider.
     */
    Map<String, Collection<String>> getCatalogIdentifiersForAllCommerceProviders();
}
