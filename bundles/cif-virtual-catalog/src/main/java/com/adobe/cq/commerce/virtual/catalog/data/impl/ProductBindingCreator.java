/*******************************************************************************
 *
 *
 *      Copyright 2020 Adobe. All rights reserved.
 *      This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License. You may obtain a copy
 *      of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software distributed under
 *      the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *      OF ANY KIND, either express or implied. See the License for the specific language
 *      governing permissions and limitations under the License.
 *
 ******************************************************************************/

package com.adobe.cq.commerce.virtual.catalog.data.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.virtual.catalog.data.Constants;
import com.day.cq.commons.jcr.JcrConstants;

@Component(
    service = ResourceChangeListener.class,
    property = { ResourceChangeListener.PATHS + "=glob:/conf/**/*",
        ResourceChangeListener.CHANGES + "=REMOVED",
        ResourceChangeListener.CHANGES + "=ADDED" })
public class ProductBindingCreator implements ResourceChangeListener {

    private static final Logger LOG = LoggerFactory.getLogger(ProductBindingCreator.class);
    private static final String PRODUCT_BINDING_SERVICE = "product-binding-service";
    private static final String BINDINGS_PARENT_PATH = "/var/commerce/products";

    private ResourceResolver resolver;

    @Reference
    private ResourceResolverFactory resolverFactory = null;

    protected void activate(final ComponentContext context) throws LoginException {
        LOG.debug("Activating the component");
        final Map<String, Object> map = new HashMap<>();
        map.put(ResourceResolverFactory.SUBSERVICE, PRODUCT_BINDING_SERVICE);
        resolver = resolverFactory.getServiceResourceResolver(map);
        LOG.debug("Do we have a resolver? {}", resolver != null);
    }

    protected void deactivate() {
        resolver.close();
    }

    @Override
    public void onChange(List<ResourceChange> list) {
        LOG.debug("Change detected somewhere...");
        list.stream().filter(change -> {
            String path = change.getPath();
            LOG.debug("Processing path {}", path);
            return !path.endsWith(JcrConstants.JCR_CONTENT) && path.contains(Constants.COMMERCE_BUCKET_PATH);
        }).forEach(change -> {
            if (change.getType() == ResourceChange.ChangeType.ADDED) {
                processAddition(change);
            } else if (change.getType() == ResourceChange.ChangeType.REMOVED) {
                processRemoval(change);
            }
        });
        try {
            resolver.commit();
        } catch (PersistenceException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void processAddition(ResourceChange change) {
        String path = change.getPath();
        LOG.debug("Process resource addition at path {}", path);

        Resource changedResource = resolver.getResource(path);
        Resource contentResource = changedResource.getChild(JcrConstants.JCR_CONTENT);

        ValueMap properties;
        if (contentResource != null) {
            properties = contentResource.getValueMap();
        } else {
            properties = changedResource.getValueMap();
        }

        String storeView = properties.get("magentoStore", "");
        if (StringUtils.isEmpty(storeView)) {
            LOG.warn("The configuration at path {} doesn't have a 'storeView' property");
            return;
        }

        String configRoot = path.substring(0, path.indexOf(Constants.COMMERCE_BUCKET_PATH) - 1);
        String configName = configRoot.substring(configRoot.lastIndexOf("/") + 1);

        String bindingName = configName + "-" + storeView;
        LOG.debug("New binding name: {}", bindingName);

        Map<String, Object> mappingProperties = new HashMap<>();
        mappingProperties.put("jcr:primaryType", "sling:Folder");
        mappingProperties.put("jcr:title", bindingName);
        mappingProperties.put("cq:conf", configRoot);

        Resource parent = resolver.getResource(BINDINGS_PARENT_PATH);
        if (parent == null) {
            LOG.warn("Binding parent path not found at {}. Nothing to do here...", BINDINGS_PARENT_PATH);
            return;
        }

        try {
            LOG.debug("Creating a new resource at {}, properties are ", parent.getPath() + "/" + bindingName, mappingProperties);
            resolver.create(parent, bindingName, mappingProperties);
        } catch (PersistenceException e) {
            LOG.error(e.getMessage(), e);
        }

    }

    private void processRemoval(ResourceChange change) {
        LOG.debug("Processing resource removal at {}", change.getPath());
    }

}
