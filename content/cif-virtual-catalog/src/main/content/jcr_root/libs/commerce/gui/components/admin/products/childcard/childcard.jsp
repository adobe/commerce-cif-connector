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
%><%@page session="false" contentType="text/html; charset=utf-8"%><%
%><%@page import="java.util.Calendar,
                  java.util.List,
                  javax.jcr.security.Privilege,
                  org.apache.commons.lang.StringUtils,
                  org.apache.jackrabbit.util.Text,
                  org.apache.sling.api.resource.Resource,
                  com.adobe.granite.ui.components.Tag,
                  com.adobe.granite.ui.components.AttrBuilder,
                  com.day.cq.commons.jcr.JcrConstants,
                  com.day.cq.i18n.I18n,
                  com.adobe.cq.commerce.api.Product,
                  com.adobe.cq.commerce.common.CommerceHelper,
                  java.util.Iterator,
                  org.apache.sling.api.resource.ResourceUtil"%><%


    Calendar modifiedDateRaw = properties.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
    Calendar publishedDateRaw = properties.get("cq:lastReplicated", Calendar.class);

    Calendar createdDateRaw = properties.get(JcrConstants.JCR_CREATED, Calendar.class);
    Calendar twentyFourHoursAgo = Calendar.getInstance();
    twentyFourHoursAgo.add(Calendar.DATE, -1);
    if ((createdDateRaw == null) || (modifiedDateRaw != null && modifiedDateRaw.before(createdDateRaw))) {
        createdDateRaw = modifiedDateRaw;
    }
    boolean isNew = createdDateRaw != null && twentyFourHoursAgo.before(createdDateRaw);

    String lastReplicationAction = properties.get("cq:lastReplicationAction", String.class);
    boolean deactivated = "Deactivate".equals(lastReplicationAction);
    String title = CommerceHelper.getCardTitle(resource, pageManager);

    List<String> applicableRelationships = getActionRels(resource, properties, product, acm, sling);

    Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();
    String imageUrl;
    boolean isFolder = false;
    if (product != null) {
        imageUrl = (product != null) ? CommerceHelper.getProductCardThumbnail(request.getContextPath(), product) : "";
        attrs.addClass("card-asset");
    } else {
        imageUrl = request.getContextPath() + xssAPI.getValidHref(resource.getPath()) + ".folderthumbnail.jpg?width=240&height=160";
        isFolder = true;
    }
    attrs.add("itemprop", "item");
    attrs.add("itemscope", "itemscope");
    attrs.add("data-timeline", true);
    attrs.add("data-gridlayout-sortkey", isNew ? 10 : 0);
    attrs.add("data-path", resource.getPath()); // for compatibility

    attrs.addClass("foundation-collection-navigator");
    //attrs passed to component may contain this tag, avoid setting it twice, tag already set is not our usecase.
    if (!attrs.build().contains("data-foundation-collection-navigator-href=")) {
        attrs.add("data-foundation-collection-navigator-href",
                xssAPI.getValidHref(request.getContextPath() + getAdminUrl(resource, currentPage)));
    }

    if (request.getAttribute("cq.6.0.legacy.semantics.foundation-collection") != null) {
        attrs.addClass("foundation-collection-item");
        attrs.add("data-foundation-collection-item-id", resource.getPath());
    }

    boolean showQuickActions = true;
    Object quickActionsAttr = request.getAttribute("com.adobe.cq.item.quickActions");
    if (quickActionsAttr != null) {
        if (quickActionsAttr.getClass().getName().equals("java.lang.String")) {
            showQuickActions =  quickActionsAttr.equals("true");
        } else {
            showQuickActions = (Boolean) quickActionsAttr;
        }
    }

    if (isFolder) {
        attrs.add("variant", "inverted");
    }

%><coral-card <%= attrs.build() %>><%
    if(StringUtils.isNotBlank(imageUrl)){%>
    <coral-card-asset>
        <img src="<%=xssAPI.getValidHref(imageUrl)%>"/>
    </coral-card-asset>
    <%}
        if (isNew) {
    %><coral-card-info><coral-tag color="blue" class="u-coral-pullRight"><%= xssAPI.encodeForHTML(i18n.get("New")) %></coral-tag></coral-card-info><%
        } %>
    <coral-card-content><%
        String context = isVirtual(resource) || isCloudBoundFolder(resource) ? i18n.get("Cloud products") : isFolder ? i18n.get("Folder") : null;
        if (context != null) {
    %><coral-card-context><%= xssAPI.encodeForHTML(context) %></coral-card-context><%
        }
    %><coral-card-title class="foundation-collection-item-title"><%= xssAPI.encodeForHTML(title) %></coral-card-title>
        <%
            if (product != null) { %>
        <coral-card-propertylist>
            <coral-card-property title="<%= xssAPI.encodeForHTMLAttr(i18n.get("SKU")) %>"><%= xssAPI.encodeForHTML(product.getSKU()) %></coral-card-property>
        </coral-card-propertylist>
        <coral-card-propertylist>
            <coral-card-property icon="edit" title="<%= xssAPI.encodeForHTMLAttr(i18n.get("Modified")) %>">
                <% if (modifiedDateRaw != null) { %>
                <foundation-time type="datetime" value="<%= xssAPI.encodeForHTMLAttr(modifiedDateRaw.toInstant().toString()) %>"></foundation-time>
                <% } else { %>
                <%= xssAPI.encodeForHTML(i18n.get("never")) %>
                <% } %>
            </coral-card-property><%
            if (!isVirtual(resource)) {
                if (!deactivated && publishedDateRaw != null) {
        %><coral-card-property icon="globe" title="<%= xssAPI.encodeForHTMLAttr(i18n.get("Published")) %>"><foundation-time type="datetime" value="<%= xssAPI.encodeForHTMLAttr(publishedDateRaw.toInstant().toString()) %>"></foundation-time></coral-card-property><%
                } else {
        %><coral-card-property icon="globeRemove"><%= xssAPI.encodeForHTML(i18n.get("Not published")) %></coral-card-property><%
                }%>
        </coral-card-propertylist> <%
                }
            }
        %>
    </coral-card-content>
    <meta class="foundation-collection-quickactions" data-foundation-collection-quickactions-rel="<%= xssAPI.encodeForHTMLAttr(StringUtils.join(applicableRelationships, " ")) %>"/>
    <link rel="properties" href="<%=xssAPI.getValidHref(getPropertiesHref(request, resource, product))%>">
</coral-card>
<% if(showQuickActions){%>
<coral-quickactions target="_prev" alignmy="left top" alignat="left top">
    <coral-quickactions-item icon="check" class="foundation-collection-item-activator"><%= xssAPI.encodeForHTML(i18n.get("Select")) %></coral-quickactions-item><%
    if (hasPermission(acm, resource, Privilege.JCR_READ)) {
        // show touch-optimized scaffold in properties view: %>
    <coral-quickactions-item icon="infoCircle" class="foundation-anchor" data-foundation-anchor-href="<%= xssAPI.getValidHref(getPropertiesHref(request, resource, product)) %>"><%= xssAPI.encodeForHTML(getPropertiesTitle(i18n, product)) %></coral-quickactions-item><%
    }
    if (hasPermission(acm, resource, "crx:replicate")) { %>
    <coral-quickactions-item icon="globe" class="foundation-collection-action"
                             data-foundation-collection-action='{"action": "cq.wcm.publish", "data": {"referenceSrc": "<%= request.getContextPath() %>/libs/wcm/core/content/reference.json?_charset_=utf-8{&path*}", "wizardSrc": "<%= request.getContextPath() %>/libs/wcm/core/content/sites/publishpagewizard.html?_charset_=utf-8{&item*}"}}'><%= xssAPI.encodeForHTML(i18n.get("Publish")) %></coral-quickactions-item>
    <%
        } %>
</coral-quickactions>
<%}%>
<%!
    private String getPropertiesHref(HttpServletRequest request, Resource resource, Product product) {
        if (product != null) {
            return request.getContextPath() + "/mnt/overlay/commerce/gui/content/products/properties.html?item=" + Text.escapePath(resource.getPath());
        } else {
            return request.getContextPath() + "/mnt/overlay/commerce/gui/content/products/folderproperties.html" + Text.escapePath(resource.getPath());
        }
    }

    private String getPropertiesTitle(I18n i18n, Product product) {
        if (product != null) {
            return i18n.get("View Product Data");
        } else {
            return i18n.get("Properties");
        }
    }

    private boolean hasVariantChildren(Resource resource) {
        for (Iterator<Resource> it = resource.listChildren(); it.hasNext();) {
            Resource r = it.next();
            if (ResourceUtil.getValueMap(r).get("cq:commerceType", "").equals("variant")) {
                return true;
            }
        }
        return false;
    }
%>
