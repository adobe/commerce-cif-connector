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
        import="com.adobe.granite.ui.components.rendercondition.RenderCondition,
                  com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition,
				  org.apache.commons.lang.StringUtils,
				  org.apache.sling.api.resource.NonExistingResource,
				  org.apache.sling.api.resource.Resource,
				  org.apache.sling.api.resource.ResourceResolver,
                  com.adobe.granite.ui.components.Config,
                  com.day.cq.wcm.api.Page,
                  com.day.cq.wcm.api.PageManager" %><%
%><%
    final Config cfg = cmp.getConfig();
    final String path = cmp.getExpressionHelper().getString(cfg.get("path", String.class));
    boolean decision = isProductDetailPage(path, resourceResolver);
    request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(decision));
%><%!
    private boolean isProductDetailPage(String path, ResourceResolver resourceResolver) {
        if(StringUtils.isBlank(path)) {
            return false;
        }

        Resource resource = resourceResolver.resolve(path);
        if (resource instanceof NonExistingResource) {
            return false;
        }

        Page page = resourceResolver.adaptTo(PageManager.class).getPage(resource.getPath()) ;
        if (page == null) {
            return false;
        }

        Resource pageContent = page.getContentResource();
        if ( pageContent == null) {
            return false;
        }

        String cqTemplate = pageContent.getValueMap().get("cq:template", String.class);
        if (StringUtils.isBlank(cqTemplate)) {
            return false;
        }
        return cqTemplate.endsWith("/settings/wcm/templates/product-page");
    }
%>