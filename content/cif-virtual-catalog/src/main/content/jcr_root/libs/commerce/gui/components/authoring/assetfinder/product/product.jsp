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
<%@ page import="com.adobe.granite.ui.components.AttrBuilder,
                 com.adobe.granite.ui.components.Config,
                 com.day.cq.commons.jcr.JcrConstants,
                 com.day.cq.i18n.I18n,
                 com.adobe.cq.commerce.api.Product,
                 com.adobe.cq.commerce.common.CommerceHelper,
                 java.util.Calendar" %><%
%><%@page session="false" %><%
%><%@include file="/libs/foundation/global.jsp" %><%

    I18n i18n = new I18n(slingRequest);
    Product product = resource.adaptTo(Product.class);
    if (product == null) {
        return;
    }
    String imageUrl = CommerceHelper.getProductCardThumbnail(request.getContextPath(), product);
    Calendar modifiedDateRaw = properties.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
    Calendar createdDateRaw = properties.get(JcrConstants.JCR_CREATED, Calendar.class);
    Calendar twentyFourHoursAgo = Calendar.getInstance();
    twentyFourHoursAgo.add(Calendar.DATE, -1);
    if ((createdDateRaw == null) || (modifiedDateRaw != null && modifiedDateRaw.before(createdDateRaw))) {
        createdDateRaw = modifiedDateRaw;
    }
    boolean isNew = createdDateRaw != null && twentyFourHoursAgo.before(createdDateRaw);

    Config cfg = new Config(resource);
    AttrBuilder attrs = new AttrBuilder(request, xssAPI);
    // Additional classes
    attrs.addClass(cfg.get("class", String.class));
    attrs.addOther("path", product.getSKU());
    // attributes needed for drag&drop
    attrs.addOther("asset-group", "product");
    attrs.addOther("type", "Products");

    attrs.addOthers(cfg.getProperties(), "id", "class");
    String title = product.getTitle();
%>
<coral-card class="editor-Card-asset card-asset cq-draggable u-coral-openHand" draggable="true" <%= attrs.build() %>>
    <coral-card-asset>
        <img class="cq-dd-image" src="<%= imageUrl != null ? xssAPI.getValidHref(imageUrl) : "" %>"
            alt="<%= xssAPI.encodeForHTMLAttr(product.getDescription()) %>" />
    </coral-card-asset>
    <%if (isNew) {
    %><coral-card-info>
        <coral-tag color="blue" class="u-coral-pullRight"><%= xssAPI.encodeForHTML(i18n.get("New")) %></coral-tag>
    </coral-card-info><%
    }%>
    <coral-card-content>
        <coral-card-title class="foundation-collection-item-title"><%= xssAPI.encodeForHTML(title) %></coral-card-title>
        <coral-card-propertylist>
            <coral-card-property title="<%= xssAPI.encodeForHTMLAttr(i18n.get("Product SKU")) %>">
                <%= xssAPI.encodeForHTML(product.getSKU()) %></coral-card-property>
        </coral-card-propertylist>
    </coral-card-content>
</coral-card>