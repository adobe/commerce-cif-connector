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

package com.adobe.cq.commerce.gui.components.common.cifproductfield.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.Servlet;

import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.graphql.search.CatalogSearchSupport;
import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.adobe.cq.commerce.graphql.resource.GraphqlQueryLanguageProvider.*;

@Component(
    service = Servlet.class,
    name = "CIFProductFieldSearchDataSourceServlet",
    immediate = true,
    property = {
        "sling.servlet.resourceTypes=commerce/gui/components/common/cifproductfield/datasources/search",
        "sling.servlet.methods=GET"
    })
public class SearchDataSourceServlet extends SlingSafeMethodsServlet {
    static final String VIRTUAL_PRODUCT_QUERY_LANGUAGE = "virtualProductOmnisearchQuery";

    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        final String PARAMETER_OFFSET = "_commerce_offset";
        final String PARAMETER_LIMIT = "_commerce_limit";
        final String PARAMETER_COMMERCE_TYPE = "_commerce_commerce_type";
        final Config cfg = new Config(request.getResource().getChild(Config.DATASOURCE));
        final SlingScriptHelper sling = ((SlingBindings) request.getAttribute(SlingBindings.class.getName())).getSling();
        final ExpressionHelper ex = new ExpressionHelper(sling.getService(ExpressionResolver.class), request);
        final String itemRT = cfg.get("itemResourceType", String.class);
        final long offset = ex.get(cfg.get("offset", "0"), long.class);
        final long limit = ex.get(cfg.get("limit", "20"), long.class);
        final String commerceType = ex.get(cfg.get("commerceType", "product"), String.class);

        Map<String, Object> queryParameters = new HashMap<>(request.getParameterMap());
        queryParameters.put(PARAMETER_OFFSET, String.valueOf(offset));
        queryParameters.put(PARAMETER_LIMIT, String.valueOf(limit));
        queryParameters.put(PARAMETER_COMMERCE_TYPE, commerceType);

        final String rootPath = request.getParameter("root");

        String rootCategoryId = new CatalogSearchSupport(request.getResourceResolver()).findCategoryId(rootPath);
        if (rootCategoryId != null) {
            queryParameters.put(CATEGORY_ID_PARAMETER, rootCategoryId);
            queryParameters.put(CATEGORY_PATH_PARAMETER, rootPath);
        }
        String queryString = new ObjectMapper().writeValueAsString(queryParameters);
        try {
            Iterator<Resource> virtualResults = request.getResourceResolver().findResources(queryString, VIRTUAL_PRODUCT_QUERY_LANGUAGE);

            final DataSource ds = new SimpleDataSource(new TransformIterator<>(virtualResults, r -> new ResourceWrapper(r) {
                public String getResourceType() {
                    return itemRT;
                }
            }));

            request.setAttribute(DataSource.class.getName(), ds);
        } catch (Exception x) {
            response.sendError(500, x.getMessage());
        }
    }
}
