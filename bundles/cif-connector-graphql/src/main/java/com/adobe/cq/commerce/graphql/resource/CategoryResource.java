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

package com.adobe.cq.commerce.graphql.resource;

import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.day.cq.commons.jcr.JcrConstants;
import static com.adobe.cq.commerce.graphql.resource.Constants.CATEGORY;
import static com.adobe.cq.commerce.graphql.resource.Constants.CIF_ID;
import static com.adobe.cq.commerce.graphql.resource.Constants.LEAF_CATEGORY;
import static com.adobe.cq.commerce.graphql.resource.Constants.MAGENTO_GRAPHQL_PROVIDER;

class CategoryResource extends SyntheticResource {

  private ValueMap values;

  /**
   * Creates a SyntheticResource for the given Magento GraphQL CategoryTree.
   * 
   * @param resourceResolver The resource resolver.
   * @param path The path of the category resource that will be created.
   * @param category The Magento GraphQL CategoryTree.
   */
  CategoryResource(ResourceResolver resourceResolver, String path, CategoryTree category) {
    super(resourceResolver, path, JcrResourceConstants.NT_SLING_FOLDER);
    Map<String, Object> map = new HashMap<>();
    map.put(JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER);
    map.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, JcrResourceConstants.NT_SLING_FOLDER);
    map.put(CommerceConstants.PN_COMMERCE_TYPE, CATEGORY);
    map.put(CommerceConstants.PN_COMMERCE_PROVIDER, MAGENTO_GRAPHQL_PROVIDER);

    if (category != null) {
      map.put(JcrConstants.JCR_TITLE, category.getName());
      map.put(CIF_ID, category.getId());
      if (category.getChildren() == null || category.getChildren().isEmpty()) {
        map.put(LEAF_CATEGORY, true);
      } else {
        map.put(LEAF_CATEGORY, false);
      }
    } else {
      map.put(JcrConstants.JCR_TITLE, MAGENTO_GRAPHQL_PROVIDER);
    }
    values = new ValueMapDecorator(map);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
    if (type == ValueMap.class) {
      return (AdapterType) values;
    } else if (type == Product.class) {
      // important because of
      // /libs/commerce/gui/components/admin/products/childdatasource/childdatasource.jsp
      return null;
    }
    return super.adaptTo(type);
  }
}
