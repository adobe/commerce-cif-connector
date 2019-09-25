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
%><%@include file="/libs/cq/gui/components/siteadmin/admin/properties/FilteringResourceWrapper.jsp"%><%
%><%@page session="false"
          import="com.adobe.granite.ui.components.ExpressionResolver,
                  com.day.cq.wcm.api.Page,
                  com.day.cq.wcm.api.Template,
                  com.day.cq.wcm.api.WCMFilteringResourceWrapper,
                  com.day.cq.wcm.api.components.Component,
                  javax.jcr.Node"%><%

    /*
      The component to include the dialog definition (cq:dialog) of a cq:Component.

      @component
      @name Include
      @location /cq/gui/components/siteadmin/admin/properties/include

      @property {StringEL} path The resource path which resource type will be used. The resource type must point to a cq:Component.
    */

    // TODO - CQ-22516
    // Bulk editing pages of different resource types (-> different structure)
    // -> merge the dialog structure too, using something like:

    //Resource dialogContentDefinition = resourceResolver.getResource(dialog.getPath() + "/content/");
    //Resource tabList1 = dialogContentDefinition.getChild("items/columns/items/tabs");
    //Resource tabList2 = dialogContentDefinition.getChild("items/columns/items/tabs");
    //mergedFormTabs = SchemaFormHelper.mergeFormTabResource(tabList1, tabList2);

    Config cfg = cmp.getConfig();
    String path = cmp.getExpressionHelper().getString(cfg.get("path", String.class));
    if (path == null) {
        return;
    }

    Resource content = resourceResolver.getResource(path);
    if (content == null) {
        return;
    }

    // check the parent to get the real resource type
    Resource rtResource = (content.getName().equals("jcr:content")) ? content.getParent() : content;

    // check if resource is a page or a folder
    String[] folderResourceTypes = cfg.get("folderResourceTypes", String[].class);

    boolean isPage = (rtResource.adaptTo(Page.class) != null);
    boolean isTemplate = (rtResource.adaptTo(Template.class) != null);
    boolean isFolder = false;
    String folderDialog = "sling/OrderedFolder/cq:dialog";
    String templateDialog = "cq/Template/cq:dialog";
    if (folderResourceTypes != null && !isPage && !isTemplate) {
        for (String rt:folderResourceTypes) {
            isFolder = rtResource.isResourceType(rt);
            folderDialog = "sling/" + rt.substring(("sling:").length()) + "/cq:dialog";
            if (isFolder) break;
        }
    }
    // get the dialog
    Resource dialog = null;
    Resource rt = resourceResolver.getResource(content.getResourceType());
    if (isTemplate) { // resource is a template
        dialog = resourceResolver.getResource(templateDialog);
    } else if (rt == null && isFolder) { // resource is a folder
        Node rtNode = rtResource.adaptTo(Node.class);
        if (rtNode == null) { // folder is virtual
            dialog = resourceResolver.getResource("commerce/components/ciffolder/cq:dialog");
        } else if (rtNode.hasProperty("cq:catalogDataResourceProviderFactory")) {
            dialog = resourceResolver.getResource("commerce/components/cifrootfolder/cq:dialog");
        } else {
            dialog = resourceResolver.getResource(folderDialog);
        }
    } else {
        if (rt == null) {
            return;
        }
        Component component = rt.adaptTo(Component.class);
        if (component == null) {
            return;
        }
        dialog = resourceResolver.getResource("/mnt/override" + component.getPath() + "/cq:dialog");
    }

    if (dialog == null) {
        return;
    }

    Resource dialogContent = resourceResolver.getResource(dialog.getPath() + "/content");
    if (dialogContent == null) {
        return;
    }

    ResourceWrapper hideOnEditWrapper = new FilteringResourceWrapper(dialogContent);
    ResourceWrapper wcmWrapper = new WCMFilteringResourceWrapper(hideOnEditWrapper, content, sling.getService(ExpressionResolver.class), slingRequest);

%><sling:include resource="<%= wcmWrapper %>" />