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
%><%@include file="/libs/commerce/gui/components/admin/products/products.jsp" %><%
%><%@page session="false"%><%
%><%@page import="java.util.Iterator,
				  java.util.List,
				  java.util.UUID,
				  org.apache.commons.lang.StringUtils,
				  org.apache.jackrabbit.util.Text,
                  org.apache.sling.api.resource.ResourceUtil,
                  com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Tag,
                  com.adobe.cq.commerce.api.Product,
                  com.adobe.cq.commerce.common.CommerceHelper" %><%

    String title = CommerceHelper.getCardTitle(resource, pageManager);

    List<String> applicableRelationships = getActionRels(resource, properties, product, acm, sling);

    Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();

    attrs.add("itemscope", "itemscope");
    attrs.add("data-path", resource.getPath()); // for compatibility
    attrs.add("data-timeline", true);
    attrs.add("data-item-title", title);
    attrs.add("data-href", "#" + UUID.randomUUID());

    boolean isError = resource.getValueMap().get("isError", false);
    if (isError) {
        response.sendError(500, "Server error");
        return;
    }

    if (hasChildren(resource, product != null)) {
        attrs.add("variant", "drilldown");
    }
%><coral-columnview-item <%= attrs.build() %>>
    <coral-columnview-item-thumbnail><%
        if (product != null) {
            String thumbnailUrl = CommerceHelper.getProductCardThumbnail(request.getContextPath(), product);
    %><img class="foundation-collection-item-thumbnail" src="<%= xssAPI.getValidHref(thumbnailUrl) %>" alt="" itemprop="thumbnail"><%
    } else {
        String icon = isVirtual(resource) || isCloudBoundFolder(resource) ? "cloud": "folder";
    %><coral-icon class="foundation-collection-item-thumbnail" icon="<%= icon %>"></coral-icon><%
        } %>
    </coral-columnview-item-thumbnail>
    <coral-columnview-item-content class="foundation-collection-item-title" itemprop="title" title="<%= xssAPI.encodeForHTMLAttr(title) %>"><%= xssAPI.encodeForHTML(title) %></coral-columnview-item-content>

    <meta class="foundation-collection-quickactions" data-foundation-collection-quickactions-rel="<%= xssAPI.encodeForHTMLAttr(StringUtils.join(applicableRelationships, " ")) %>"/>
    <link itemprop="admin" href="<%= xssAPI.getValidHref(request.getContextPath() + "/aem/products.html" + Text.escapePath(resource.getPath())) %>">
</coral-columnview-item><%!

    private boolean hasChildren(Resource resource, boolean isProduct) {
        Boolean hasChildren = resource.getValueMap().get("hasChildren", Boolean.class);
        if (hasChildren != null) {
            return hasChildren;
        }

        for (Iterator<Resource> it = resource.listChildren(); it.hasNext();) {
            Resource r = it.next();
            if (isProduct) {
                if (ResourceUtil.getValueMap(r).get("cq:commerceType", "").equals("variant")) {
                    return true;
                }
            } else {
                if (r.adaptTo(Product.class) != null) {
                    return true;
                }
                if (r.isResourceType("sling:Folder") || r.isResourceType("sling:OrderedFolder")) {
                    return true;
                }
            }
        }

        return false;
    }
%>
