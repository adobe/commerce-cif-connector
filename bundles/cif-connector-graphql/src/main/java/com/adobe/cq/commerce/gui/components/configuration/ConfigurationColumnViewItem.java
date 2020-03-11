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

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.virtual.catalog.data.Constants;
import com.day.cq.commons.jcr.JcrConstants;

/**
 * Sling Model for the column-view item of the configuration console
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ConfigurationColumnViewItem.class,
    resourceType = "commerce/gui/components/configuration/columnviewitem")
public class ConfigurationColumnViewItem {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationColumnViewItem.class);

    @Self
    private SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    boolean hasCommerceSetting;

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
        boolean isContainer = isConfigurationContainer();
        return isContainer && hasCommerceSetting;
    }

    public List<String> getQuickActionsRel() {
        List<String> actions = new ArrayList<>();

        if (isConfigurationContainer()) {
            // for /conf/<bucket>/settings folder we add the "Create" activator
            // so we can do a "client-side render condition"
            actions.add(hasCommerceSetting ? "none" : "cq-confadmin-actions-create-activator");
        }

        if (!isConfigurationContainer()) {
            // for items which are not configuration containers (folders)
            actions.add("cq-confadmin-actions-properties-activator");
            actions.add("cq-confadmin-actions-delete-activator");
        }

        return actions;
    }

    private boolean isConfigurationContainer() {
        return (resource.getPath()
            .startsWith(Constants.CONF_ROOT) && (resource.isResourceType(JcrResourceConstants.NT_SLING_FOLDER) || resource.isResourceType(
                JcrResourceConstants.NT_SLING_ORDERED_FOLDER))
            && resource
                .getChild(Constants.CONF_CONTAINER_BUCKET_NAME) != null);
    }
}
