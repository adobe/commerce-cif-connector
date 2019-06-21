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

package com.adobe.cq.commerce.graphql.resource;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataService;
import com.day.cq.commons.jcr.JcrConstants;

import static com.adobe.cq.commerce.graphql.resource.Constants.CATEGORY;
import static com.adobe.cq.commerce.graphql.resource.Constants.PRODUCT;

class GraphqlResourceProvider<T> extends ResourceProvider<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlResourceProvider.class);

    private String root;
    private Integer rootCategoryId;
    private ResourceMapper<T> resourceMapper;
    private GraphqlQueryLanguageProvider<T> queryLanguageProvider;

    GraphqlResourceProvider(String root, GraphqlDataService graphqlDataService, Scheduler scheduler) {
        this.root = root;
        rootCategoryId = graphqlDataService.getConfiguration().rootCategoryId();
        resourceMapper = new ResourceMapper<T>(root, graphqlDataService, scheduler);
        queryLanguageProvider = new GraphqlQueryLanguageProvider<T>(resourceMapper, graphqlDataService);
    }

    @Override
    public Resource getResource(ResolveContext<T> ctx, String path, ResourceContext resourceContext, Resource parent) {
        LOGGER.debug("getResource called for " + path);

        // needed for testing, harmless during runtime
        if (path.endsWith("/.")) {
            path = path.substring(0, path.length() - 2);
        }

        if (path.equals(root)) {
            Resource resource = ctx.getParentResourceProvider().getResource(
                (ResolveContext) ctx.getParentResolveContext(), path, resourceContext, parent);
            if (resource == null) {
                return null;
            } else {
                return new RootCategoryResource(resource, rootCategoryId);
            }
        }

        if (path.contains(JcrConstants.JCR_CONTENT) || path.endsWith(".jpg") ||
            path.contains("assets") || path.contains("thumbnail") || path.contains("rep:policy")) {
            return null;
        }

        CategoryResource category = resourceMapper.resolveCategory(ctx, path);
        if (category != null) {
            return category;
        } else {
            Resource resource;
            if (path.endsWith("/image")) {
                resource = resourceMapper.resolveProductImage(ctx, path);
            } else {
                resource = resourceMapper.resolveProduct(ctx, path);
            }
            return resource;
        }
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext<T> ctx, Resource parent) {
        ValueMap valueMap = parent.getValueMap();
        String commerceType = valueMap.get(CommerceConstants.PN_COMMERCE_TYPE, String.class);
        if (root.equals(parent.getPath()) || CATEGORY.equals(commerceType)) {
            return resourceMapper.listCategoryChildren(ctx, parent);
        } else if (PRODUCT.equals(commerceType)) {
            return resourceMapper.listProductChildren(ctx, parent);
        }
        return null;
    }

    @Override
    public QueryLanguageProvider<T> getQueryLanguageProvider() {
        return queryLanguageProvider;
    }
}
