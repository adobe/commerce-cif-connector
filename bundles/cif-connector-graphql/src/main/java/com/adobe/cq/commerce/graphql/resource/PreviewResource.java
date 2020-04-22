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

package com.adobe.cq.commerce.graphql.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.DeepReadValueMapDecorator;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataService;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.day.cq.commons.jcr.JcrConstants;

import static com.adobe.cq.commerce.graphql.resource.Constants.CATEGORY;
import static com.adobe.cq.commerce.graphql.resource.Constants.MAGENTO_GRAPHQL_PROVIDER;

class PreviewResource extends SyntheticResource {

    private ValueMap values;
    private Long epoch;
    private Integer categoryId;
    private GraphqlDataService graphqlDataService;
    private String storeView;

    /**
     * Creates a SyntheticResource for the given Magento GraphQL CategoryTree.
     * 
     * @param resourceResolver The resource resolver.
     * @param path The path of the category resource that will be created.
     * @param category The Magento GraphQL CategoryTree.
     */
    PreviewResource(ResourceResolver resourceResolver, String path, Long epoch, Integer categoryId, GraphqlDataService graphqlDataService,
                    String storeView) {
        super(resourceResolver, path, JcrResourceConstants.NT_SLING_FOLDER);

        this.epoch = epoch;
        this.categoryId = categoryId;
        this.graphqlDataService = graphqlDataService;
        this.storeView = storeView;

        Map<String, Object> map = new HashMap<>();
        map.put(JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER);
        map.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, JcrResourceConstants.NT_SLING_FOLDER);
        map.put(CommerceConstants.PN_COMMERCE_TYPE, CATEGORY);
        map.put(CommerceConstants.PN_COMMERCE_PROVIDER, MAGENTO_GRAPHQL_PROVIDER);
        values = new DeepReadValueMapDecorator(this, new ValueMapDecorator(map));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == ValueMap.class) {
            return (AdapterType) values;
        } else if (type == Product.class) {
            // important because of /libs/commerce/gui/components/admin/products/childdatasource/childdatasource.jsp
            return null;
        }
        return super.adaptTo(type);
    }

    @Override
    public Iterator<Resource> listChildren() {
        CategoryTree categoryTree = graphqlDataService.getCategoryById(categoryId, storeView, epoch);
        List<Resource> children = new ArrayList<>();
        if (categoryTree != null) {
            List<CategoryTree> subChildren = categoryTree.getChildren();
            if (subChildren != null) {
                for (CategoryTree child : subChildren) {
                    children.add(new CategoryResource(getResourceResolver(), getPath() + "/" + child.getUrlPath(), child));
                }
            }
        }
        return children.isEmpty() ? null : children.iterator();
    }
}
