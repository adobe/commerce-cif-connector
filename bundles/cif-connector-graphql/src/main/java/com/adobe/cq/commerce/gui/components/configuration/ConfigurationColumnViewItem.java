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

package com.adobe.cq.commerce.gui.components.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.virtual.catalog.data.Constants;
import com.day.cq.commons.jcr.JcrConstants;

import static com.adobe.cq.commerce.virtual.catalog.data.Constants.CLOUDCONFIGS_BUCKET_NAME;
import static com.adobe.cq.commerce.virtual.catalog.data.Constants.CONF_CONTAINER_BUCKET_NAME;

/**
 * Sling Model for the column-view item of the configuration console
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ConfigurationColumnViewItem.class,
    resourceType = "commerce/gui/components/configuration/columnviewitem")
public class ConfigurationColumnViewItem {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationColumnViewItem.class);

    static final String CREATE_PULLDOWN_ACTIVATOR = "cq-confadmin-actions-create-pulldown-activator";
    static final String CREATE_CONFIG_ACTIVATOR = "cq-confadmin-actions-create-config-activator";
    static final String PROPERTIES_ACTIVATOR = "cq-confadmin-actions-properties-activator";
    static final String CREATE_FOLDER_ACTIVATOR = "cq-confadmin-actions-create-folder-activator";
    static final String DELETE_ACTIVATOR = "cq-confadmin-actions-delete-activator";
    static final String PUBLISH_ACTIVATOR = "cq-confadmin-actions-publish-activator";
    static final String UNPUBLISH_ACTIVATOR = "cq-confadmin-actions-unpublish-activator";

    @Inject
    private Resource resource;

    private boolean hasCommerceSetting;

    @PostConstruct
    public void initModel() {
        LOG.debug("Initializing column view item for resource {}", resource.getPath());
        hasCommerceSetting = resource.getChild(Constants.COMMERCE_BUCKET_PATH) != null;
    }

    public String getTitle() {
        Resource jcrContent = resource.getChild(JcrConstants.JCR_CONTENT);
        ValueMap properties = jcrContent != null ? jcrContent.getValueMap() : resource.getValueMap();
        return properties.get(JcrConstants.JCR_TITLE, resource.getName());
    }

    public boolean hasChildren() {
        if (isCommerceBucket())
            return false;

        boolean isContainer = isConfigurationContainer();
        boolean hasChildren = resource.hasChildren();
        boolean hasMoreChildren = getChildCount(resource) > 1;
        boolean hasSettings = resource.getChild("settings") != null;
        return isContainer && (hasCommerceSetting || hasMoreChildren) ||
            !isContainer && !hasCommerceSetting && (hasChildren && !hasSettings || hasMoreChildren && hasSettings);
    }

    private boolean isCommerceBucket() {
        return resource.getPath().endsWith(Constants.COMMERCE_BUCKET_PATH);
    }

    public List<String> getQuickActionsRel() {
        List<String> actions = new ArrayList<>();
        if (isConfigurationContainer()) {
            // for /conf/<bucket>/settings folder we add the "Create" activator
            // so we can do a "client-side render condition"
            if (!hasCommerceSetting && !resource.getPath().equals(Constants.CONF_ROOT)) {
                actions.add(CREATE_PULLDOWN_ACTIVATOR);
                actions.add(CREATE_CONFIG_ACTIVATOR);
            }
        }

        if (isCommerceBucket()) {
            // for items which are not configuration containers (folders)
            actions.add(PROPERTIES_ACTIVATOR);
            actions.add(PUBLISH_ACTIVATOR);
            actions.add(UNPUBLISH_ACTIVATOR);
        } else {
            if (!actions.contains(CREATE_PULLDOWN_ACTIVATOR)) {
                actions.add(CREATE_PULLDOWN_ACTIVATOR);
            }
            actions.add(CREATE_FOLDER_ACTIVATOR);
        }

        if (isSafeToDelete()) {
            actions.add(DELETE_ACTIVATOR);
        }

        return actions;
    }

    private boolean isConfigurationContainer() {
        return resource.getPath().startsWith(Constants.CONF_ROOT) && isFolder(resource) &&
            resource.getChild(CONF_CONTAINER_BUCKET_NAME + "/" + CLOUDCONFIGS_BUCKET_NAME) != null;
    }

    private boolean isFolder(Resource resource) {
        return resource.isResourceType(JcrConstants.NT_FOLDER) || isSlingFolder(resource);
    }

    private boolean isSafeToDelete() {
        // commerce config
        if (isCommerceBucket()) {
            return true;
        }

        // or container only without children
        if (resource.getPath().startsWith(Constants.CONF_ROOT) && isSlingFolder(resource) &&
            hasOnlyChild(resource, CONF_CONTAINER_BUCKET_NAME)) {
            Resource container = resource.getChild(CONF_CONTAINER_BUCKET_NAME);
            return hasOnlyChild(container, CLOUDCONFIGS_BUCKET_NAME) && !container.getChild(CLOUDCONFIGS_BUCKET_NAME).hasChildren();
        }

        return false;
    }

    private boolean isSlingFolder(Resource resource) {
        return resource.isResourceType(JcrResourceConstants.NT_SLING_FOLDER) ||
            resource.isResourceType(JcrResourceConstants.NT_SLING_ORDERED_FOLDER);
    }

    private boolean hasOnlyChild(Resource resource, String child) {
        return getChildCount(resource) == 1 && resource.getChild(child) != null;
    }

    private long getChildCount(Resource resource) {
        return StreamSupport.stream(resource.getChildren().spliterator(), false).count();
    }
}
