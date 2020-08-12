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
%><%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" %><%
%><%@page import="java.util.ArrayList,
                  java.util.List,
                  javax.jcr.RepositoryException,
                  javax.jcr.security.AccessControlManager,
                  javax.jcr.security.Privilege,
                  com.day.cq.i18n.I18n,
                  com.day.cq.wcm.api.Page,
                  com.day.cq.wcm.core.utils.ScaffoldingUtils,
                  org.apache.jackrabbit.util.Text,
                  org.apache.sling.api.resource.Resource,
                  org.apache.sling.api.resource.ResourceResolver,
                  org.apache.sling.api.resource.ValueMap,
                  org.apache.sling.api.scripting.SlingScriptHelper,
                  com.adobe.cq.commerce.api.Product,
                  com.adobe.cq.commerce.api.conf.CommerceBasePathsService,
                  com.adobe.granite.ui.components.ComponentHelper,
                  com.adobe.granite.xss.XSSAPI" %><%
    final ComponentHelper cmp = new ComponentHelper(pageContext);
    final I18n i18n = cmp.getI18n();
    final Product product = resource.adaptTo(Product.class);

    AccessControlManager acm = null;
    try {
        acm = resourceResolver.adaptTo(Session.class).getAccessControlManager();
    } catch (RepositoryException e) {
        log.error("Unable to get access manager", e);
    }

%><%!

    private boolean hasTouchScaffold(Resource resource, ValueMap properties, SlingScriptHelper sling) throws RepositoryException {
        ResourceResolver resourceResolver = resource.getResourceResolver();
        String scaffoldPath = properties.get("cq:scaffolding", "");
        if (scaffoldPath.length() == 0) {
            // search all scaffolds for a path match
            CommerceBasePathsService cbps = sling.getService(CommerceBasePathsService.class);
            Resource scRoot = resourceResolver.getResource(cbps.getScaffoldingBasePath());
            Node root = scRoot == null ? null : scRoot.adaptTo(Node.class);
            if (root != null) {
                scaffoldPath = ScaffoldingUtils.findScaffoldByPath(root, resource.getPath());
            }
        }
        boolean hasTouchScaffold = false;
        if (scaffoldPath != null && scaffoldPath.length() > 0) {
            Resource scaffold = resourceResolver.getResource(scaffoldPath);
            hasTouchScaffold = scaffold != null && scaffold.getChild("jcr:content/cq:dialog") != null;
        }
        return hasTouchScaffold;
    }

    private List<String> getActionRels(Resource resource, ValueMap properties, Product product, AccessControlManager acm, SlingScriptHelper sling) throws RepositoryException {
        boolean isVirtual = isVirtual(resource) || isCloudBoundFolder(resource);
        boolean isBinding = isBinding(resource);
        List<String> applicableRelationships = new ArrayList<String>();
        if (!isVirtual) { //creation is disabled for a virtual resource
            if (isBinding) {
                applicableRelationships.add("cq-commerce-products-folderproperties-activator");
                return applicableRelationships;
            }

            if (product != null) {
                applicableRelationships.add("cq-commerce-products-createvariation-activator");
            } else {
                applicableRelationships.add("cq-commerce-products-createproduct-activator");
                applicableRelationships.add("cq-commerce-products-createfolder-activator");
                applicableRelationships.add("cq-commerce-products-import-activator");
                applicableRelationships.add("cq-commerce-products-bindproducttree-activator");
            }
            applicableRelationships.add("cq-commerce-products-create-activator");
        }

        if (hasPermission(acm, resource, Privilege.JCR_READ)) {
            if (product != null) {
                boolean hasTouchScaffold = hasTouchScaffold(resource, properties, sling);
                if (hasTouchScaffold) {
                    applicableRelationships.add("foundation-admin-properties-activator");
                } else {
                    applicableRelationships.add("cq-commerce-products-edit-activator");
                }
                applicableRelationships.add("cq-commerce-collections-addcollectiontoproduct-activator");
            }
            else {
                applicableRelationships.add("cq-commerce-products-folderproperties-activator");
            }
        }

        if (hasPermission(acm, resource, Privilege.JCR_REMOVE_NODE)) {
            if (product != null) {
                applicableRelationships.add("cq-commerce-products-move-activator");
            } else {
                applicableRelationships.add("cq-commerce-products-movefolder-activator");
            }
            applicableRelationships.add("cq-commerce-products-delete-activator");
            applicableRelationships.add("cq-commerce-collections-removefromcollection-activator");
            // enable collection folders to be removed
            applicableRelationships.add("cq-commerce-collections-removecollection-activator");
        }

        if (hasPermission(acm, resource, "crx:replicate")) {
            applicableRelationships.add("cq-commerce-products-publish-activator");
            applicableRelationships.add("cq-commerce-products-unpublish-activator");
        }

        return applicableRelationships;
    }

    private boolean hasPermission(AccessControlManager acm, Resource child, String privilege) {
        try {
            if (acm != null) {
                if (isVirtual(child)) {
                    return hasVirtualPermission(acm, child, privilege);
                } else {
                    Privilege p = acm.privilegeFromName(privilege);
                    return acm.hasPrivileges(child.getPath(), new Privilege[]{p});
                }
            }
        } catch (RepositoryException e) {
            // ignore
        }
        return false;
    }

    private boolean hasVirtualPermission(AccessControlManager acm, Resource child, String privilege) {
        //a virtual resource may have read permission only and that is defined by the closes non-virtual parent
        if (Privilege.JCR_READ.equals(privilege)) {
            //search for product data root which is JCR based
            Resource resource = child;
            while (isVirtual(resource)) {
                resource = resource.getParent();
                if (resource == null) {
                    break;
                }
            }

            //check read permission of root
            if (resource != null) {
                try {
                    Privilege p = acm.privilegeFromName(privilege);
                    return acm.hasPrivileges(resource.getPath(), new Privilege[]{p});
                } catch (RepositoryException e) {
                    //ignore
                }
            }
        }

        return false;
    }

    private boolean isBinding(Resource resource) {
        if (isVirtual(resource)) {
            return false;
        }

        Node node = resource.adaptTo(Node.class);
        try {
            return node.hasProperty("cq:conf");
        } catch (RepositoryException ex) {
            return false;
        }
    }

    private boolean isVirtual(Resource resource) {
        Node node = resource.adaptTo(Node.class);
        return node == null;
    }

    private boolean isCloudBoundFolder(Resource resource) {
        if (isVirtual(resource)) {
            return false;
        }

        Node node = resource.adaptTo(Node.class);
        try {
            return node.hasProperty("cq:catalogDataResourceProviderFactory");
        } catch (RepositoryException ex) {
            return false;
        }
    }


    private String getAdminUrl(Resource pageResource, Page requestPage) {
        String base = requestPage != null ? requestPage.getVanityUrl() : "/aem/products";

        if (base == null) {
            base = requestPage.getProperties().get("sling:vanityPath", base);
        }

        if (base == null) {
            base = Text.escapePath(requestPage.getPath());
        }

        // when viewing the collection members, clicking a product card opens the product properties page
        if (requestPage != null) {
            String productPropertiesAdminUrl = requestPage.getProperties().get("productPropertiesAdminUrl", String.class);
            if (StringUtils.isNotEmpty(productPropertiesAdminUrl)) {
                base = productPropertiesAdminUrl + ".html?item=";
            } else {
                base = base + ".html";
            }
        } else {
            base = base + ".html";
        }

        return base + Text.escapePath(pageResource.getPath());
    }

    /**
     * A shortcut for <code>xssAPI.encodeForHTML(i18n.getVar(text))</code>.
     */
    protected final String outVar(XSSAPI xssAPI, I18n i18n, String text) {
        return xssAPI.encodeForHTML(i18n.getVar(text));
    }

    /**
     * A shortcut for <code>xssAPI.encodeForHTMLAttr(i18n.getVar(text))</code>.
     */
    protected final String outAttrVar(XSSAPI xssAPI, I18n i18n, String text) {
        return xssAPI.encodeForHTMLAttr(i18n.getVar(text));
    }
%>
