<%--

    Copyright 2019 Adobe. All rights reserved.
    This file is licensed to you under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under
    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
    OF ANY KIND, either express or implied. See the License for the specific language
    governing permissions and limitations under the License.

--%><%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page session="false"
          import="javax.jcr.Session,
                  java.util.Map,
                  java.util.HashMap,
                  org.apache.commons.collections4.Transformer,
                  org.apache.commons.collections4.iterators.TransformIterator,
                  org.apache.sling.api.resource.ResourceWrapper,
                  com.adobe.granite.omnisearch.spi.core.OmniSearchHandler,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.ExpressionHelper,
                  com.adobe.granite.ui.components.ds.DataSource,
                  com.adobe.granite.ui.components.ds.SimpleDataSource,
                  com.day.cq.search.result.SearchResult" %><%

final Config cfg = new Config(resource.getChild(Config.DATASOURCE));
final ExpressionHelper ex = cmp.getExpressionHelper();

final String itemRT = cfg.get("itemResourceType", String.class);
final long offset = ex.get(cfg.get("offset", "0"), long.class);
final long limit = ex.get(cfg.get("limit", "20"), long.class);

Map<String, Object> params = new HashMap<>();
for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
    params.put(entry.getKey(), entry.getValue());
}

String filter = "(component.name=com.adobe.cq.commerce.impl.omnisearch.ProductsOmniSearchHandler)";
OmniSearchHandler[] handlers = sling.getServices(OmniSearchHandler.class, filter);
final SearchResult result = handlers[0].getResults(resourceResolver, params, limit, offset);

final DataSource ds = new SimpleDataSource(new TransformIterator<>(result.getResources(), new Transformer<Resource, Resource>() {
    public Resource transform(Resource r) {
        return new ResourceWrapper(r) {
            public String getResourceType() {
                return itemRT;
            }
        };
    }
}));

request.setAttribute(DataSource.class.getName(), ds);
%>