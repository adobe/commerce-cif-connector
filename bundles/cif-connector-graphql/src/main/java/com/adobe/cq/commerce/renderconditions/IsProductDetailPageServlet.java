/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.renderconditions;

import javax.servlet.Servlet;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@Component(
    service = Servlet.class,
    name = "IsProductDetailPageRenderConditionServlet",
    immediate = true,
    property = {
        "sling.servlet.resourceTypes=commerce/gui/components/authoring/editor/pagepreview/renderconditions/isproductdetailpage",
        "sling.servlet.methods=GET"
    })
public class IsProductDetailPageServlet extends SlingSafeMethodsServlet {

    static final String PRODUCT_RT = "core/cif/components/commerce/product/v1/product";

    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {
        final Config cfg = new Config(request.getResource());
        final SlingScriptHelper sling = ((SlingBindings) request.getAttribute(SlingBindings.class.getName())).getSling();
        final ExpressionHelper ex = new ExpressionHelper(sling.getService(ExpressionResolver.class), request);
        final String path = ex.getString(cfg.get("path", String.class));
        boolean decision = isProductDetailPage(path, request.getResourceResolver());
        request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(decision));
    }

    private boolean isProductDetailPage(String path, ResourceResolver resourceResolver) {
        if (StringUtils.isBlank(path)) {
            return false;
        }

        Resource resource = resourceResolver.resolve(path);
        if (resource instanceof NonExistingResource) {
            return false;
        }

        Page page = resourceResolver.adaptTo(PageManager.class).getPage(resource.getPath());
        if (page == null) {
            return false;
        }

        Resource pageContent = page.getContentResource();
        if (pageContent == null) {
            return false;
        }

        boolean val = containsComponent(PRODUCT_RT, pageContent);

        return val;
    }

    static boolean containsComponent(String resourceType, Resource resource) {
        if (resource == null)
            return false;

        if (resource.isResourceType(resourceType)) {
            return true;
        }

        for (Resource child : resource.getChildren()) {
            boolean val = containsComponent(resourceType, child);
            if (val) {
                return true;
            }
        }

        return false;
    }
}
