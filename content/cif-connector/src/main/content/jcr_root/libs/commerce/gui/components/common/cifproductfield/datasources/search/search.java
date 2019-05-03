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

package libs.commerce.gui.components.common.cifproductfield.datasources.search;

import java.io.IOException;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.ServletException;

import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;

public class search extends SlingSafeMethodsServlet {
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        final String VIRTUAL_PRODUCT_QUERY_LANGUAGE = "virtualProductOmnisearchQuery";
        final String PARAMETER_OFFSET = "_commerce_offset";
        final String PARAMETER_LIMIT = "_commerce_limit";
        final Config cfg = new Config(request.getResource().getChild(Config.DATASOURCE));
        final SlingScriptHelper sling = ((SlingBindings) request.getAttribute(SlingBindings.class.getName())).getSling();
        final ExpressionHelper ex = new ExpressionHelper(sling.getService(ExpressionResolver.class), request);;
        final String itemRT = cfg.get("itemResourceType", String.class);
        final long offset = ex.get(cfg.get("offset", "0"), long.class);
        final long limit = ex.get(cfg.get("limit", "20"), long.class);

        Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.putAll(request.getParameterMap());
        queryParameters.put(PARAMETER_OFFSET, String.valueOf(offset));
        queryParameters.put(PARAMETER_LIMIT, String.valueOf(limit));
        String queryString = new ObjectMapper().writeValueAsString(queryParameters);
        Iterator<Resource> virtualResults = request.getResourceResolver().findResources(queryString, VIRTUAL_PRODUCT_QUERY_LANGUAGE);

        final DataSource ds = new SimpleDataSource(new TransformIterator<>(virtualResults, new Transformer<Resource, Resource>() {
            public Resource transform(Resource r) {
                return new ResourceWrapper(r) {
                    public String getResourceType() {
                        return itemRT;
                    }
                };
            }
        }));

        request.setAttribute(DataSource.class.getName(), ds);
    }
}
