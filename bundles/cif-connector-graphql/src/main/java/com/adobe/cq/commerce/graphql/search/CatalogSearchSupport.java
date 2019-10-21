/*******************************************************************************
 *
 *    Copyright 2019 Adobe. All rights reserved.
 *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License. You may obtain a copy
 *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under
 *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *    OF ANY KIND, either express or implied. See the License for the specific language
 *    governing permissions and limitations under the License.
 *
 ******************************************************************************/

package com.adobe.cq.commerce.graphql.search;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.graphql.resource.Constants;
import com.day.cq.commons.inherit.ComponentInheritanceValueMap;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

public class CatalogSearchSupport {
    public static final String PN_CATALOG_PATH = "cq:catalogPath";
    private ResourceResolver resolver;

    public CatalogSearchSupport(ResourceResolver resolver) {
        this.resolver = resolver;
    }

    /**
     * Searches for the category identifier at the give path. It the give path doesn't have a {@link Constants#CIF_ID} property
     * then the search continues in the parent resources for the {@link Constants#MAGENTO_ROOT_CATEGORY_ID_PROPERTY}.
     *
     * @param path a path in the content repository
     * @return the category identifier or {@code null} if not found
     */
    public String findCategoryId(final String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }

        Resource rootResource = resolver.getResource(path);
        if (rootResource == null) {
            return null;
        }

        String categoryId = null;
        if (Constants.CATEGORY.equals(rootResource.getValueMap().get(CommerceConstants.PN_COMMERCE_TYPE, String.class))) {
            categoryId = rootResource.getValueMap().get(Constants.CIF_ID, String.class);
        }
        if (StringUtils.isBlank(categoryId)) {
            InheritanceValueMap inherited = new ComponentInheritanceValueMap(rootResource);
            categoryId = inherited.getInherited(Constants.MAGENTO_ROOT_CATEGORY_ID_PROPERTY, String.class);
        }

        if (StringUtils.isBlank(categoryId)) {
            return null;
        }

        return categoryId;
    }

    /**
     * Searches for the {@link #PN_CATALOG_PATH} property at the specified path or its parent pages.
     *
     * @param path a path in the content repository
     * @return the value of the {@link #PN_CATALOG_PATH} property or {@code null} if not found
     */
    public String findCatalogPath(String path) {
        if (!StringUtils.isNotBlank(path)) {
            return null;
        }
        Page parentPage = resolver.adaptTo(PageManager.class).getContainingPage(path);
        if (parentPage == null) {
            return null;
        }
        InheritanceValueMap inheritedProperties = new HierarchyNodeInheritanceValueMap(parentPage.getContentResource());
        return inheritedProperties.getInherited(PN_CATALOG_PATH, String.class);
    }
}
