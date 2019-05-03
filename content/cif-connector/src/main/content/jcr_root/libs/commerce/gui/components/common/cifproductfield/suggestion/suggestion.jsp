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
          import="java.util.Collections,
                  java.util.Comparator,
                  java.util.Iterator,
                  java.util.List,
                  java.util.regex.Matcher,
                  java.util.regex.Pattern,
                  org.apache.jackrabbit.util.Text,
                  com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.ExpressionHelper,
                  com.adobe.granite.ui.components.PagingIterator"%><%

    final ExpressionHelper ex = cmp.getExpressionHelper();
    final Config cfg = cmp.getConfig();

    final String query = ex.getString(cfg.get("query", String.class));

    final String searchName = Text.getName(query);

    final Pattern searchNamePattern = Pattern.compile(Pattern.quote(searchName), Pattern.CASE_INSENSITIVE);

%><coral-buttonlist><%
    for (Iterator<Resource> it = cmp.getItemDataSource().iterator(); it.hasNext();) {
        Resource r = it.next();
        String path = r.getValueMap().get("identifier", String.class);
        String title = r.getValueMap().get("jcr:title", String.class);

        AttrBuilder attrs = new AttrBuilder(request, xssAPI);
        attrs.add("type", "button");
        attrs.add("is", "coral-buttonlist-item");
        attrs.add("value", path);


%><button <%= attrs %>><%= mark(path, searchNamePattern, xssAPI) %> (<%=title%>) </button><%
    }
%></coral-buttonlist><%!

    private String mark(String path, Pattern regex, XSSAPI xssAPI) {
        StringBuilder sb = new StringBuilder();

        int parentIndex = path.lastIndexOf('/');

        sb.append(xssAPI.encodeForHTML(path.substring(0, parentIndex + 1)));

        String name = path.substring(parentIndex + 1);

        Matcher m = regex.matcher(name);

        if (m.lookingAt()) {
            sb.append(xssAPI.encodeForHTML(name.substring(0, m.start())));

            if (m.group().length() > 0) {
                sb.append("<mark>" + xssAPI.encodeForHTML(m.group()) + "</mark>");
            }

            sb.append(xssAPI.encodeForHTML(name.substring(m.end())));
        } else {
            sb.append(xssAPI.encodeForHTML(name));
        }

        return sb.toString();
    }
%>