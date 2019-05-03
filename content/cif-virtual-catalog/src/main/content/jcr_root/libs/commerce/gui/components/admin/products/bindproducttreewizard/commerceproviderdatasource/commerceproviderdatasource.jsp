<%--

    Copyright 2019 Adobe. All rights reserved.
    This file is licensed to you under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under
    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
    OF ANY KIND, either express or implied. See the License for the specific language
    governing permissions and limitations under the License.

--%>
<%@page session="false"
        import="java.util.ArrayList,
                java.util.HashMap,
                java.util.Map,
                org.apache.sling.api.wrappers.ValueMapDecorator,
                com.adobe.granite.ui.components.ds.DataSource,
                com.adobe.granite.ui.components.ds.EmptyDataSource,
                com.adobe.granite.ui.components.ds.SimpleDataSource,
                com.adobe.granite.ui.components.ds.ValueMapResource,
                org.apache.sling.api.resource.ResourceMetadata,
                com.adobe.cq.commerce.virtual.catalog.data.CatalogDataResourceProviderFactory,
                com.adobe.cq.commerce.virtual.catalog.data.CatalogDataResourceProviderManager" %>
<%@include file="/libs/foundation/global.jsp" %>
<%
    // Get data for datasource
    Map<String, CatalogDataResourceProviderFactory<?>> dataResourceProviderFactories =
            sling.getService(CatalogDataResourceProviderManager.class).getProviderFactories();

    // Build datasource
    ArrayList<Resource> resourceList = new ArrayList<Resource>();
    for (String factoryName : dataResourceProviderFactories.keySet()) {
        Map<String, Object> map = new HashMap<>();
        map.put("text", factoryName);
        map.put("value", factoryName);
        ValueMapResource syntheticResource = new ValueMapResource(resourceResolver, new ResourceMetadata(), "", new ValueMapDecorator(map));
        resourceList.add(syntheticResource);
    }
    DataSource ds = resourceList.isEmpty() ? EmptyDataSource.instance() : new SimpleDataSource(resourceList.iterator());

    // Put datasource in request for consumption
    request.setAttribute(DataSource.class.getName(), ds);
%>