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

package com.adobe.cq.commerce.graphql.core;

import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import com.adobe.cq.commerce.common.ValueMapDecorator;

public class SyntheticResourceWithProperties extends SyntheticResource {

  private Map<String, Object> properties;

  public SyntheticResourceWithProperties(ResourceResolver resourceResolver, String path,
      String resourceType, Map<String, Object> properties) {
    super(resourceResolver, path, resourceType);
    this.properties = properties;
  }

  @SuppressWarnings("unchecked")
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
    if (type == ValueMap.class) {
      return (AdapterType) new ValueMapDecorator(this.properties);
    }
    return super.adaptTo(type);
  }
}
