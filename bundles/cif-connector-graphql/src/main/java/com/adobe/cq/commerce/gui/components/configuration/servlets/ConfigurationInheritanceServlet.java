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

package com.adobe.cq.commerce.gui.components.configuration.servlets;

import java.io.IOException;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.utils.ConfigInheritance;
import com.adobe.cq.commerce.virtual.catalog.data.Constants;

@Component(
    service = Servlet.class,
    name = "ConfigurationInheritanceServlet",
    immediate = true,
    property = {
        "sling.servlet.methods=GET",
        "sling.servlet.selectors=cifconfig",
        "sling.servlet.extensions=json",
        "sling.servlet.resourceTypes=sling/servlet/default"
    })
public class ConfigurationInheritanceServlet extends SlingSafeMethodsServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationInheritanceServlet.class);
    private final static String[] CONFIG_KEYS = {
        "cq:catalogDataResourceProviderFactory",
        "cq:catalogIdentifier",
        "cq:graphqlClient",
        "jcr:language",
        "magentoGraphqlEndpoint",
        "magentoRootCategoryId",
        "magentoStore"
    };

    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {

        try {
            Resource resource = request.getResource();
            String resourcePath = resource.getPath();

            // Get the container resource path
            String configContainerPath = resourcePath.endsWith(Constants.COMMERCE_BUCKET_PATH) ? resourcePath.replace(
                "/" + Constants.COMMERCE_BUCKET_PATH, "") : resourcePath;

            // Get the container resource
            Resource configContainerResource = request.getResourceResolver().getResource(configContainerPath);

            if (configContainerResource == null) {
                response.sendError(500, "Unable to load configuration container resource");
                return;
            }

            // Start with a JSON documents that assumes no inheritance
            JSONObject config = ConfigInheritance.getInheritanceMap(configContainerResource, CONFIG_KEYS);

            response.setContentType("application/json");
            response.getWriter().println(config.toString());
        } catch (JSONException e) {
            LOGGER.error("Unable to retrieve config: {}", e.getMessage());
            response.sendError(500, "Unable to create JSON response");
        }
    }
}
