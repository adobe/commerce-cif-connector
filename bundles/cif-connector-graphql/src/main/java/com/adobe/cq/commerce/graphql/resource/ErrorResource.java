/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.DeepReadValueMapDecorator;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.day.cq.commons.jcr.JcrConstants;

import static com.adobe.cq.commerce.graphql.resource.Constants.CATEGORY;
import static com.adobe.cq.commerce.graphql.resource.Constants.HAS_CHILDREN;
import static com.adobe.cq.commerce.graphql.resource.Constants.IS_ERROR;
import static com.adobe.cq.commerce.graphql.resource.Constants.LEAF_CATEGORY;
import static com.adobe.cq.commerce.graphql.resource.Constants.MAGENTO_GRAPHQL_PROVIDER;

/**
 * ErrorResource represents an error detected in the resource provider implementation to be reported
 * to layers above the Sling Resource API.
 */
class ErrorResource extends SyntheticResource {
    static final String NAME = "error";

    private ValueMap values;

    /**
     * Creates a SyntheticResource for the given Magento GraphQL CategoryTree.
     *
     * @param resourceResolver The resource resolver.
     * @param parentPath The path of the category resource that will be created.
     */
    ErrorResource(ResourceResolver resourceResolver, String parentPath) {
        super(resourceResolver, parentPath + "/" + NAME, JcrResourceConstants.NT_SLING_FOLDER);
        Map<String, Object> map = new HashMap<>();
        map.put(JcrConstants.JCR_PRIMARYTYPE, JcrResourceConstants.NT_SLING_FOLDER);
        map.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, JcrResourceConstants.NT_SLING_FOLDER);
        map.put(CommerceConstants.PN_COMMERCE_TYPE, CATEGORY);
        map.put(CommerceConstants.PN_COMMERCE_PROVIDER, MAGENTO_GRAPHQL_PROVIDER);

        map.put(JcrConstants.JCR_TITLE, "Server error");
        map.put(LEAF_CATEGORY, true);
        map.put(HAS_CHILDREN, false);
        map.put(IS_ERROR, true);

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
}
