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
package com.adobe.cq.commerce.virtual.catalog.models.impl;

import java.net.URLEncoder;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;

import com.adobe.cq.commerce.virtual.catalog.models.ConfigurationReference;

@Model(
    adaptables = SlingHttpServletRequest.class,
    adapters = { ConfigurationReference.class },
    resourceType = ConfigurationReferenceImpl.RESOURCE_TYPE)
public class ConfigurationReferenceImpl implements ConfigurationReference {
    protected static final String RESOURCE_TYPE = "commerce/components/cifrootfolder/reference";

    @Self
    private SlingHttpServletRequest request;

    @Inject
    private Resource resource;

    private String text;
    private String configUrl;

    @PostConstruct
    private void initModel() {
        ValueMap vm = resource.getValueMap();

        if (vm.containsKey("text")) {
            text = vm.get("text", String.class);
        } else {
            text = "Reference";
        }

        String path = request.getParameter("item") != null ? request.getParameter("item") : request.getRequestPathInfo().getSuffix();

        if (path != null) {
            Resource targetResource = request.getResourceResolver().getResource(path);
            if (targetResource != null) {
                ValueMap targetVm = targetResource.getValueMap();
                if (targetVm.containsKey("cq:conf")) {
                    String configPath = targetVm.get("cq:conf", String.class);

                    Resource configResource = request.getResourceResolver().getResource(configPath + "/settings/cloudconfigs/commerce");

                    if (configResource != null) {
                        configUrl = "/mnt/overlay/wcm/core/content/sites/properties.html?item=" +
                            URLEncoder.encode(configResource.getPath());
                    }
                }
            }
        }
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getConfigURL() {
        return configUrl;
    }

}
