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
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
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

class GraphqlResourceProvider extends ResourceProvider<Object> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlResourceProvider.class);

    private String root;
    private Integer rootCategoryId;
    private ResourceMapper resourceMapper;
    private GraphqlQueryLanguageProvider queryLanguageProvider;

    GraphqlResourceProvider(String root, GraphqlDataService graphqlDataService, Map<String, String> properties) {
        this.root = root;
        rootCategoryId = Integer.valueOf(properties.get(Constants.MAGENTO_ROOT_CATEGORY_ID_PROPERTY));
        resourceMapper = new ResourceMapper(root, graphqlDataService, properties);
        queryLanguageProvider = new GraphqlQueryLanguageProvider(resourceMapper, graphqlDataService, properties);
    }

    @Override
    public Resource getResource(ResolveContext<Object> ctx, String path, ResourceContext resourceContext, Resource parent) {
        LOGGER.debug("getResource called for " + path);

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

        ResourceResolver resolver = ctx.getResourceResolver();
        CategoryResource category = resourceMapper.resolveCategory(resolver, path);
        if (category != null) {
            return category;
        } else {
            Resource resource;
            if (path.endsWith("/image")) {
                resource = resourceMapper.resolveProductImage(resolver, path);
            } else {
                resource = resourceMapper.resolveProduct(resolver, path);
            }
            return resource;
        }
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext<Object> ctx, Resource parent) {
        ValueMap valueMap = parent.getValueMap();
        String commerceType = valueMap.get(CommerceConstants.PN_COMMERCE_TYPE, String.class);
        ResourceResolver resolver = ctx.getResourceResolver();
        if (root.equals(parent.getPath()) || CATEGORY.equals(commerceType)) {
            return resourceMapper.listCategoryChildren(resolver, parent);
        } else if (PRODUCT.equals(commerceType)) {
            return resourceMapper.listProductChildren(resolver, parent);
        }
        return null;
    }

    @Override
    public GraphqlQueryLanguageProvider getQueryLanguageProvider() {
        return queryLanguageProvider;
    }
}
