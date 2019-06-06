/*******************************************************************************
 *
 * Copyright 2019 Adobe. All rights reserved. This file is licensed to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.adobe.cq.commerce.virtual.catalog.data;

import java.util.List;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ResourceProvider;

/**
 * Manages the resource registrations for the {@link ResourceProvider}.
 */
public interface CatalogDataResourceProviderManager {

  /**
   * Returns the registered resource provider factories.
   * 
   * @return a {@link Map} where a keys are factory identifiers and values are the corresponding
   *         resource providers
   */
  Map<String, CatalogDataResourceProviderFactory<?>> getProviderFactories();

  /**
   * Returns all the products data roots found in the JCR.
   * 
   * @return The list of data roots.
   */
  List<Resource> getDataRoots();
}
