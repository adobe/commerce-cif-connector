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

package com.adobe.cq.commerce.utils;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.adobe.cq.commerce.virtual.catalog.data.Constants;

public class ConfigInheritance {

    public static JSONObject getInheritanceMap(Resource configContainerResource, String[] configKeys) throws JSONException {
        ResourceResolver resourceResolver = configContainerResource.getResourceResolver();
        // Start with a JSON documents that assumes no inheritance
        JSONObject config = new JSONObject();
        config.put("inherited", false);

        // Get the parent resource to start gathering inherited properties
        Resource parentConfigContainerResource = configContainerResource.getParent();

        // If the resource is still under /conf
        if (configContainerResource.getPath().startsWith("/conf") &&
            parentConfigContainerResource != null &&
            !parentConfigContainerResource.getPath().equals("/conf")) {

            // Set inherited prop true and add a new JSON object to hold inherited properties values
            config.put("inherited", true);
            JSONObject inheritedProps = new JSONObject();

            Resource parentConfig;
            while (parentConfigContainerResource != null && !parentConfigContainerResource.getPath().equals("/conf")) {
                // Read configuration going up the tree
                parentConfig = parentConfigContainerResource.getChild(Constants.COMMERCE_BUCKET_PATH + "/jcr:content");
                extractInheritedProps(configKeys, parentConfigContainerResource, inheritedProps, parentConfig);
                // go up the inheritance chain
                parentConfigContainerResource = parentConfigContainerResource.getParent();
            }

            Resource appsDefaultConfig = resourceResolver.getResource("/apps/settings/cloudconfigs/commerce/jcr:content");
            extractInheritedProps(configKeys, parentConfigContainerResource, inheritedProps, appsDefaultConfig);

            Resource libsDefaultConfig = resourceResolver.getResource("/libs/settings/cloudconfigs/commerce/jcr:content");
            extractInheritedProps(configKeys, parentConfigContainerResource, inheritedProps, libsDefaultConfig);

            config.put("inheritedProperties", inheritedProps);

            JSONArray overriddenProps = new JSONArray();
            Resource configContent = configContainerResource.getChild(Constants.COMMERCE_BUCKET_PATH + "/jcr:content");

            // If this is an existing node, check which properties are set that override inheritance
            if (configContent != null) {
                ValueMap vm = configContent.getValueMap();
                for (String key : configKeys) {
                    if (vm.containsKey(key)) {
                        overriddenProps.put(key);
                    }
                }
            }

            config.put("overriddenProperties", overriddenProps);
        }

        return config;
    }

    private static void extractInheritedProps(String[] configKeys, Resource parentConfigContainerResource, JSONObject inheritedProps,
        Resource configNode) throws JSONException {
        if (configNode != null) {
            ValueMap vm = configNode.getValueMap();
            // Iterate through properties relevant for the configuration
            for (String key : configKeys) {
                // Check if the property was already set in a child configuration. "Deeper" nodes take precedence
                if (!inheritedProps.has(key) && vm.containsKey(key)) {
                    JSONObject propMeta = new JSONObject();
                    propMeta.put("value", vm.get(key));
                    propMeta.put("inheritedFrom", parentConfigContainerResource.getPath());
                    inheritedProps.put(key, propMeta);
                }
            }
        }
    }
}
