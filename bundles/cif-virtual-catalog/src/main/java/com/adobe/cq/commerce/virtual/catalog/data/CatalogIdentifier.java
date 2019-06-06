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

/**
 * Provides details about catalog identifiers (projects in UI) configured for a cloud commerce provider.
 */
public interface CatalogIdentifier {

    /**
     * Get a collection of all configured catalog identifiers (project in UI).
     *
     * @return A collection containing all configured catalog identifiers (project in UI).
     */
    Collection<String> getAllCatalogIdentifiers();

    /**
     * Gets the commerce provider name for the current implementation.
     *
     * @return The name of the commerce provider.
     */
    String getCommerceProviderName();
}
