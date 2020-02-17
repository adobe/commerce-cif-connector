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

package com.adobe.cq.commerce.gui.components.configuration;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.day.cq.commons.jcr.JcrConstants;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = ConfigurationColumnPreview.class,
    resourceType = "commerce/gui/components/configuration/columnpreview")
public class ConfigurationColumnPreview {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationColumnPreview.class);

    @Self
    public SlingHttpServletRequest request;

    @Inject
    public Resource resource;

    boolean isFolder = false;

    private Resource itemResource;

    private ValueMap properties;

    @PostConstruct
    protected void initModel() {

        Config cfg = new Config(request.getResource());

        final SlingScriptHelper sling = ((SlingBindings) request.getAttribute(SlingBindings.class.getName())).getSling();
        ExpressionResolver expressionResolver = sling.getService(ExpressionResolver.class);
        final ExpressionHelper ex = new ExpressionHelper(expressionResolver, request);

        String itemResourcePath = ex.getString(cfg.get("path", String.class));
        LOG.debug("Item in preview is at path {}", itemResourcePath);

        itemResource = request.getResourceResolver().getResource(itemResourcePath);
        isFolder = itemResource.isResourceType(JcrConstants.NT_FOLDER) || itemResource.isResourceType(JcrResourceConstants.NT_SLING_FOLDER)
            || itemResource
                .isResourceType(JcrResourceConstants.NT_SLING_ORDERED_FOLDER);

        if (isFolder) {
            properties = itemResource.getValueMap();
        } else {
            Resource jcrContent = itemResource.getChild(JcrConstants.JCR_CONTENT);
            properties = jcrContent != null ? jcrContent.getValueMap() : itemResource.getValueMap();
        }
    }

    public String getTitle() {
        return properties.get(JcrConstants.JCR_TITLE, itemResource.getName());
    }

    public boolean isFolder() {
        return isFolder;
    }

    public String getItemResourcePath() {
        return itemResource.getPath();
    }
}
