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

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.gui.Constants;

/**
 * Sling Model for the column-view item of the configuration console
 */
@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ConfigurationColumnViewItem.class)
public class ConfigurationColumnViewItem {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationColumnViewItem.class);

    @Self
    private SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    @PostConstruct
    private void initModel() {
        LOG.debug("Initializing column view item for resource {}", resource.getPath());
    }

    public String getTitle() {
        LOG.debug("Inspecting {}", resource.getPath());
        ValueMap properties = resource.getValueMap();
        return properties.get("jcr:title", resource.getName());
    }

    public boolean hasChildren() {
        boolean isContainer = isConfigurationContainer(resource);
        boolean hasCommerceSetting = resource.getChild("settings/cloudconfigs/commerce") != null;
        return isContainer && hasCommerceSetting;
    }

    public List<String> getQuickActionsRel() {
        return Collections.emptyList();
    }

    private boolean isConfigurationContainer(Resource res) {
        return (res.getPath()
            .startsWith(Constants.CONF_ROOT) && (resource.isResourceType("sling:Folder") || resource.isResourceType("sling:OrderedFolder")) && resource
                .getChild(Constants.CONF_CONTAINER_BUCKET_NAME) != null);
    }
}
