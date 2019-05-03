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
<%@include file="/libs/granite/ui/global.jsp" %>
<%@page session="false"
        import="com.adobe.granite.ui.components.rendercondition.RenderCondition,
                  com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition,
				  org.apache.commons.lang.StringUtils,
                  com.adobe.granite.ui.components.Config,
                  javax.jcr.Node" %>
<%

    final Config cfg = cmp.getConfig();
    final String path = StringUtils.trimToNull(cmp.getExpressionHelper().getString(cfg.get("path", String.class)));

    if(path == null) {
        return;
    }

    Resource content = resourceResolver.getResource(path);
    boolean decision = true;
    if ( content.adaptTo(Node.class) == null) {
        decision = false;
    }
    request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(decision));
%>