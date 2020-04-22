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

import java.time.OffsetDateTime;
import java.util.Calendar;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.graphql.resource.Constants;
import com.adobe.cq.launches.api.Launch;
import com.adobe.cq.wcm.launches.utils.LaunchUtils;
import com.day.cq.commons.inherit.ComponentInheritanceValueMap;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

public class CatalogSearchSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(CatalogSearchSupport.class);

    public static final String PN_CATALOG_PATH = "cq:catalogPath";
    public static final String COMPONENT_DIALIG_URI_MARKER = "/_cq_dialog.html/";
    public static final String PAGE_PROPERTIES_URI_MARKER = "/sites/properties.html";
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
        if (StringUtils.isBlank(path)) {
            return null;
        }
        PageManager pageManager = resolver.adaptTo(PageManager.class);
        Page parentPage = pageManager.getContainingPage(path);
        if (parentPage == null) {
            return null;
        }
        LOGGER.info("Picker parent page {}", parentPage.getPath());

        Long epoch = null;
        if (parentPage.getPath() != null && LaunchUtils.isLaunchBasedPath(parentPage.getPath())) {
            Resource launchResource = LaunchUtils.getLaunchResource(parentPage.adaptTo(Resource.class));
            Launch launch = launchResource.adaptTo(Launch.class);
            Calendar liveDate = launch.getLiveDate();
            if (liveDate != null) {
                TimeZone timeZone = liveDate.getTimeZone();
                OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(liveDate.toInstant(), timeZone.toZoneId());
                epoch = offsetDateTime.toEpochSecond();
            }
            Resource targetResource = LaunchUtils.getTargetResource(parentPage.adaptTo(Resource.class), null);
            Page targetPage = pageManager.getPage(targetResource.getPath());
            parentPage = targetPage != null ? targetPage : parentPage;
        }

        LOGGER.info("Picker checking properties at {}", parentPage.getPath());
        InheritanceValueMap inheritedProperties = new HierarchyNodeInheritanceValueMap(parentPage.getContentResource());
        String catalogPath = inheritedProperties.getInherited(PN_CATALOG_PATH, String.class);
        return (catalogPath != null && epoch != null) ? (catalogPath + "/_" + epoch) : catalogPath;
    }

    /**
     * Searches for the {@link #PN_CATALOG_PATH} property of the current site using algorithms specific to pickers.
     *
     * @param request the current {@code SlingHttpServletRequest}
     *
     * @return the value of the {@link #PN_CATALOG_PATH} property or or {@code null} if not found
     */
    public String findCatalogPathForPicker(SlingHttpServletRequest request) {
        if (request != null && request.getRequestURL() != null) {
            LOGGER.info("Picker path at {}", request.getRequestURL().toString());
        }

        String requestURI = request.getRequestURI();
        if (requestURI.contains(COMPONENT_DIALIG_URI_MARKER)) {
            String suffix = request.getRequestPathInfo().getSuffix();
            return findCatalogPath(suffix);
        } else if (requestURI.contains(PAGE_PROPERTIES_URI_MARKER)) {
            String item = request.getParameter("item");
            return findCatalogPath(item);
        }

        return null;
    }
}
