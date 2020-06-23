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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
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

    @Reference(target = "(" + ServiceUserMapped.SUBSERVICENAME + "=product-binding-service)")
    private ServiceUserMapped serviceUserMapped;

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
            }
            if (change.getType() == ResourceChange.ChangeType.REMOVED) {
                processDeletion(change);
            }
        });
    }

    private void processDeletion(ResourceChange change) {
        String path = change.getPath();
        LOG.debug("Process resource deletion at path {}", path);

        Resource parent = resolver.getResource(BINDINGS_PARENT_PATH);
        Iterator<Resource> resourceIterator = parent.listChildren();

        Stream<Resource> targetStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(resourceIterator, Spliterator.ORDERED),
            false);

        targetStream.filter(res -> {
            ValueMap properties = res.getValueMap();
            LOG.debug("Checking the binding at {}", res.getPath());
            String cqConf = properties.get(Constants.PN_CONF, "");
            if (StringUtils.isEmpty(cqConf)) {
                return false;
            }
            return path.equals(cqConf + "/" + Constants.COMMERCE_BUCKET_PATH);
        }).findFirst().ifPresent(res -> {
            LOG.debug("Found a binding at {} that uses {}, we'll delete it", res.getPath(), path);
            deleteJcrNode(res);
        });
    }

    private void deleteJcrNode(Resource res) {
        Session session = resolver.adaptTo(Session.class);
        try {
            session.removeItem(res.getPath());
            session.save();
        } catch (RepositoryException e) {
            LOG.error(e.getMessage(), e);
        }

    }

    private void processAddition(ResourceChange change) {
        resolver.refresh();
        String path = change.getPath();
        LOG.debug("Process resource addition at path {}", path);

        Resource changedResource = resolver.getResource(path);
        if (changedResource == null) {
            LOG.warn("Cannot retrieve resource at {}. Does the user have the require privileges?", path);
            return;
        }
        Resource contentResource = changedResource.getChild(JcrConstants.JCR_CONTENT);

        ValueMap properties;
        if (contentResource != null) {
            properties = contentResource.getValueMap();
        } else {
            properties = changedResource.getValueMap();
        }

        String storeView = properties.get(Constants.PN_MAGENTO_STORE, "");
        if (StringUtils.isEmpty(storeView)) {
            LOG.warn("The configuration at path {} doesn't have a '{}' property", path, Constants.PN_MAGENTO_STORE);
            return;
        }

        String configRoot = path.substring(0, path.indexOf(Constants.COMMERCE_BUCKET_PATH) - 1);
        String configName = configRoot.substring(configRoot.lastIndexOf("/") + 1);

        String bindingName = configName + "-" + storeView;
        LOG.debug("New binding name: {}", bindingName);

        Map<String, Object> mappingProperties = new HashMap<>();
        mappingProperties.put(JcrConstants.JCR_PRIMARYTYPE, "sling:Folder");
        mappingProperties.put(JcrConstants.JCR_TITLE, bindingName);
        mappingProperties.put(Constants.PN_CONF, configRoot);

        Resource parent = resolver.getResource(BINDINGS_PARENT_PATH);
        if (parent == null) {
            LOG.warn("Binding parent path not found at {}. Nothing to do here...", BINDINGS_PARENT_PATH);
            return;
        }

        String bindingPath = parent.getPath() + "/" + bindingName;
        LOG.debug("Check if we already have a binding at {}", bindingPath);

        try {
            LOG.debug("Creating a new resource at {}", bindingPath);
            ResourceUtil.getOrCreateResource(resolver, bindingPath, mappingProperties, "", true);

            if (contentResource != null) {
                LOG.debug("Adding {} property at {}", Constants.PN_CATALOG_PATH, contentResource.getPath());
                ModifiableValueMap map = contentResource.adaptTo(ModifiableValueMap.class);
                map.put(Constants.PN_CATALOG_PATH, bindingPath);
                resolver.commit();
            }
        } catch (PersistenceException e) {
            LOG.error(e.getMessage(), e);
        }

    }

}
