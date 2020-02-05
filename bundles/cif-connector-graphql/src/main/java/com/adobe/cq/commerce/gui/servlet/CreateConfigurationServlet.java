/*
 * ******************************************************************************
 *  *
 *  *    Copyright 2020 Adobe. All rights reserved.
 *  *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License. You may obtain a copy
 *  *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software distributed under
 *  *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *  *    OF ANY KIND, either express or implied. See the License for the specific language
 *  *    governing permissions and limitations under the License.
 *  *
 *  *****************************************************************************
 */

package com.adobe.cq.commerce.gui.servlet;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.gui.Constants;

@Component(
    service = Servlet.class,
    immediate = true,
    property = {
        "sling.servlet.selectors=createcifconf",
        "sling.servlet.methods=POST",
        "sling.servlet.resourceTypes={sling:Folder, sling:OrderedFolder}",
    })
public class CreateConfigurationServlet extends SlingAllMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CreateConfigurationServlet.class);
    private static final String PARAM_CONFIGURATION_TITLE = "configTitle";
    private static final String PARAM_CONFIGURATION_PARENT = "configParent";

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        try {
            doCreate(request);
            request.getResourceResolver().commit();
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            sendResponse(response, e.getMessage(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void doCreate(SlingHttpServletRequest request) throws Exception {
        ResourceResolver resourceResolver = request.getResourceResolver();
        String configParentPath = request.getParameter(PARAM_CONFIGURATION_PARENT);

        if (StringUtils.isEmpty(configParentPath)) {
            LOG.error("Missing required parameter configParent");
            throw new Exception("Missing required parameter configParent");
        }

        if (StringUtils.isNotEmpty(configParentPath)
            && (configParentPath.startsWith(Constants.CONF_ROOT) || Constants.CONF_ROOT.equalsIgnoreCase(configParentPath))) {
            LOG.error("Attempt to create a configuration that is not under {}", Constants.CONF_ROOT);
            throw new Exception("Attempt to create a configuration that is not under " + Constants.CONF_ROOT);
        }

        String configTitle = request.getParameter(PARAM_CONFIGURATION_TITLE);
        if (StringUtils.isEmpty(configTitle)) {
            LOG.error("Missing required parameter configTitle");
            throw new Exception("Missing required parameter configTitle");
        }

        // create the container
        Resource newConfigResource = ResourceUtil.getOrCreateResource(resourceResolver,
            configParentPath,
            JcrResourceConstants.NT_SLING_FOLDER,
            JcrResourceConstants.NT_SLING_FOLDER,
            false);
        ModifiableValueMap newConfigProps = newConfigResource.adaptTo(ModifiableValueMap.class);
        newConfigProps.put("jcr:title", configTitle);

        // create the configuration container
        Resource configContainer = ResourceUtil.getOrCreateResource(resourceResolver,
            configParentPath + "/settings",
            JcrResourceConstants.NT_SLING_FOLDER,
            JcrResourceConstants.NT_SLING_FOLDER,
            false);

        // create the "commerce" folder
        Resource commerceSettings = ResourceUtil.getOrCreateResource(resourceResolver, configContainer.getPath(), JcrResourceConstants.NT_SLING_FOLDER,JcrResourceConstants.NT_SLING_FOLDER,false);
    }



    private void sendResponse(SlingHttpServletResponse response, String message, int code) {
        try {
            PrintWriter out = new PrintWriter(response.getOutputStream());                                                  
            out.println(message);
            response.sendError(code);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
