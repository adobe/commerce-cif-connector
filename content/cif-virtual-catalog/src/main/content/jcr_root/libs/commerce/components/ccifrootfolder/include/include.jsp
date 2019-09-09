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
%><%@include file="/libs/granite/ui/global.jsp"%><%
%><%@page session="false"
          import="org.apache.commons.lang.StringUtils,
                  com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Tag"%><%

    String path = cmp.getExpressionHelper().getString(cmp.getConfig().get("path", String.class));
    if (path == null) {
        return;
    }

    Resource target = resourceResolver.getResource(path);
    if (target == null) {
        return;
    }

    String provider = target.getValueMap().get("cq:catalogDataResourceProviderFactory", String.class);
    if (StringUtils.isBlank(provider)) {
        return;
    }

    Resource customFieldsParent = resourceResolver.getResource("commerce/gui/content/products/bindproducttreewizard/customfields");
    if (customFieldsParent == null) {
        return;
    }

    Resource customFields = customFieldsParent.getChild(provider);
    if (customFields == null) {
        return;
    }

    AttrBuilder attrs = new AttrBuilder(request, xssAPI);
    Tag tag = new Tag("div", attrs);
    cmp.include(customFields, customFields.getResourceType(), tag);
%>