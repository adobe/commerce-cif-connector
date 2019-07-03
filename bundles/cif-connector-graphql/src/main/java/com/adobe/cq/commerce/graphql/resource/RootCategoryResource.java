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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.DeepReadValueMapDecorator;

import com.adobe.cq.commerce.common.ValueMapDecorator;

import static com.adobe.cq.commerce.api.CommerceConstants.PN_COMMERCE_TYPE;
import static com.adobe.cq.commerce.graphql.resource.Constants.CATEGORY;
import static com.adobe.cq.commerce.graphql.resource.Constants.CIF_ID;
import static org.apache.sling.api.resource.ResourceResolver.PROPERTY_RESOURCE_TYPE;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.NT_SLING_FOLDER;

/**
 * Resource implementation for the root category node of the product tree.
 * This node has a special status because it's backed by a JCR node and still served by the resource provider of the
 * virtual product tree.
 */
class RootCategoryResource extends ResourceWrapper {
    private Integer rootCategoryId;

    /**
     * Create a {@code RootCategoryResource} instance.
     *
     * @param rootResource the resource for root JCR node of the virtual product tree
     * @param rootCategoryId Magento root category identifier
     */
    RootCategoryResource(Resource rootResource, Integer rootCategoryId) {
        super(rootResource);
        this.rootCategoryId = rootCategoryId;
    }

    @Override
    public ValueMap getValueMap() {
        Map<String, Object> map = new HashMap<>(super.getValueMap());

        // add special properties not available in the JCR node
        map.put(PROPERTY_RESOURCE_TYPE, NT_SLING_FOLDER);
        map.put(PN_COMMERCE_TYPE, CATEGORY);
        map.put(CIF_ID, rootCategoryId);

        return new DeepReadValueMapDecorator(this, new ValueMapDecorator(Collections.unmodifiableMap(map)));
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type.equals(ValueMap.class)) {
            return (AdapterType) getValueMap();
        }

        return super.adaptTo(type);
    }
}
