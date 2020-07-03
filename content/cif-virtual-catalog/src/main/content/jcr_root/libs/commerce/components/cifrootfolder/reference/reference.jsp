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
          import="java.net.URLEncoder,
                  org.apache.commons.lang.StringUtils,
                  com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.Tag"%><%
Config cfg = cmp.getConfig();

String path = cmp.getExpressionHelper().getString(cmp.getConfig().get("path", String.class));
if (path == null) {
    return;
}

Resource target = resourceResolver.getResource(path);
if (target == null) {
    return;
}

String configPath = target.getValueMap().get("cq:conf", String.class);
if (StringUtils.isBlank(configPath)) {
    return;
}

String href = "/mnt/overlay/wcm/core/content/sites/properties.html?item=" + URLEncoder.encode(configPath + "/settings/cloudconfigs/commerce");

Tag tag = cmp.consumeTag();

AttrBuilder attrs = tag.getAttrs();

attrs.addHref("href", href);

attrs.addClass("coral-Link");

attrs.add("target", cfg.get("target", String.class));
%><a <%= attrs.build() %>><%
    out.print(xssAPI.encodeForHTML(i18n.getVar(cfg.get("text", ""), cfg.get("text_commentI18n", String.class))));
%></a>
