/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.datasource;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.gui.Constants;
import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.PagingIterator;
import com.adobe.granite.ui.components.ds.AbstractDataSource;
import com.adobe.granite.ui.components.ds.DataSource;

@Component(
    service = Servlet.class,
    property = {
        "sling.servlet.methods=GET",
        "sling.servlet.resourceTypes=commerce/gui/components/configuration/datasource"
    })
public class ConfigurationsDataSource extends SlingSafeMethodsServlet {
    private static final int DEFAULT_LIMIT = 20;

    private static final int DEFAULT_OFFSET = 0;

    private static final String SKIP_BUCKET = "skipbucket";

    private transient Logger log = LoggerFactory.getLogger(getClass());

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        try {
            final ResourceResolver resourceResolver = request.getResourceResolver();
            final Resource resource = request.getResource();

            Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
            final Resource parent = suffixResource != null ? suffixResource : resourceResolver.getResource(Constants.CONF_ROOT);

            Resource masonryCfg = resource.getChild("granite:data");
            Config dsCfg = new Config(resource.getChild(Config.DATASOURCE));

            String[] selectors = request.getRequestPathInfo().getSelectors();
            final int offset = (selectors.length > 0 && selectors[0] != null) ? Integer.parseInt(selectors[0]) : DEFAULT_OFFSET;
            final int limit = (selectors.length > 1 && selectors[1] != null) ? Integer.parseInt(selectors[1]) + 1
                : dsCfg.get("limit", DEFAULT_LIMIT) + 1;

            final String[] allowedResourceTypes = (masonryCfg != null) ? masonryCfg.getValueMap().get("allowedResourceTypes",
                String[].class) : new String[] {};
            final Boolean renderOOTBConfigs = (suffixResource == null && masonryCfg != null) ? masonryCfg.getValueMap().get("renderOOTB",
                false) : false;

            final String group = (masonryCfg != null) ? masonryCfg.getValueMap().get("group", String.class) : null;
            final String resourceType = dsCfg.get("itemResourceType", String.class);

            String skipBuckets = (masonryCfg != null) ? masonryCfg.getValueMap().get(SKIP_BUCKET, "") : "";
            if (!skipBuckets.isEmpty()) {
                if (suffixResource != null) {
                    Resource suffixParent = suffixResource.getParent();
                    if (suffixParent == null || !suffixParent.getPath().endsWith(group)) {
                        skipBuckets = "";
                    }
                }
            }

            final Boolean bSkipBuckets = (!skipBuckets.isEmpty()) ? true : false;

            DataSource datasource = new AbstractDataSource() {
                @Override
                public Iterator<Resource> iterator() {
                    Iterator<Resource> it = new PagingIterator<Resource>(
                        getResourceIterator(resourceResolver, parent, allowedResourceTypes, group, renderOOTBConfigs, bSkipBuckets), offset,
                        limit);

                    return new TransformIterator<Resource, Resource>(it, new Transformer<Resource, Resource>() {
                        @Override
                        public Resource transform(Resource o) {
                            Resource r = ((Resource) o);
                            return new ResourceWrapper(r) {
                                @Override
                                public String getResourceType() {
                                    return resourceType;
                                }
                            };
                        }
                    });
                }
            };

            request.setAttribute(DataSource.class.getName(), datasource);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    // Note; method is protected to facilitate unit testing
    protected Iterator<Resource> getResourceIterator(ResourceResolver resolver, Resource parent, String[] allowedResourceTypes,
        String group, Boolean renderOOTBConfigs, Boolean skipBuckets) {
        List<Resource> resourceList = new LinkedList<Resource>();

        if (renderOOTBConfigs) {
            resourceList.add(resolver.getResource(Constants.CONF_ROOT));
            resourceList.add(resolver.getResource(Constants.APPS_ROOT));
            resourceList.add(resolver.getResource(Constants.LIBS_ROOT));
            return resourceList.iterator();
        }

        // no access to `/conf` ?
        if (parent == null) {
            return resourceList.iterator();
        }

        if (!skipBuckets) {
            // Add all the folders if parent is in /conf hierarchy
            if (parent.getPath().startsWith(Constants.CONF_ROOT)) {
                resourceList.addAll(getResources(parent, new String[] { "nt:folder", "sling:Folder", "sling:OrderedFolder" }));
            }
            // configs
            String confBucketPath = Constants.COMMERCE_BUCKET_PATH;

            if (group != null) {
                confBucketPath += "/" + group;
            }
            if (hasSetting(parent, confBucketPath)) {
                resourceList.addAll(getResources(parent.getChild(confBucketPath), allowedResourceTypes));
            }
        } else {
            // Add all the child resources with allowed resource types
            resourceList.addAll(getResources(parent, allowedResourceTypes));
        }

        return resourceList.iterator();
    }

    /**
     * Returns a list of resource for the specified {@code parent} which is of
     * specified {@code allowedResourceTypes}.
     *
     * @param parent Parent resource
     * @param allowedResourceTypes Allowed resource types
     * @return A list of Resources or an empty list if none found
     */
    private List<Resource> getResources(Resource parent, String[] allowedResourceTypes) {
        List<Resource> configurations = new LinkedList<Resource>();
        if (parent != null) {
            Iterator<Resource> configurationsIt = parent.listChildren();
            while (configurationsIt.hasNext()) {
                Resource r = configurationsIt.next();

                if (!Constants.CONF_CONTAINER_BUCKET_NAME.equals(r.getName())
                    && !Constants.COMMERCE_BUCKET_NAME.equals(r.getName())
                    && !JcrConstants.JCR_CONTENT.equals(r.getName())
                    && isResourceType(r, allowedResourceTypes)) {
                    configurations.add(r);
                }
            }
        }
        return configurations;
    }

    /**
     * Returns {@code true} if specified {@code resource} has the configuration
     * setting {@code settingPath}.
     *
     * @param resource Resource to verify
     * @param settingPath Path of the setting to check for existence
     * @return {@code true} if the resource has the specified setting,
     *         {@code false} otherwise
     */
    private boolean hasSetting(Resource resource, String settingPath) {
        return (resource != null && resource.getChild(settingPath) != null);
    }

    /**
     * Returns {@code true} if specified {@code resource} matches one of the
     * specified {@code resourceTypes}.
     *
     * @param resource Resource to verify
     * @param resourceTypes Resource types
     * @return {@code true} if the resource matches a resource type,
     *         {@code false} otherwise
     */
    private boolean isResourceType(Resource resource, String... resourceTypes) {
        if (resource != null && resourceTypes != null) {
            if (resource.getChild("jcr:content") != null) {
                resource = resource.getChild("jcr:content");
            }
            for (String resourceType : resourceTypes) {
                if (resource.isResourceType(resourceType)) {
                    return true;
                }
            }
        }
        return false;
    }

}
