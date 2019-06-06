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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.graphql.magento.GraphqlDataService;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

class GraphqlQueryLanguageProvider<T> implements QueryLanguageProvider<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlQueryLanguageProvider.class);

    static final String VIRTUAL_PRODUCT_QUERY_LANGUAGE = "virtualProductOmnisearchQuery";
    private static final String[] SUPPORTED_LANGUAGES = { VIRTUAL_PRODUCT_QUERY_LANGUAGE };

    static final String FULLTEXT_PARAMETER = "fulltext";
    static final String OFFSET_PARAMETER = "_commerce_offset";
    static final String LIMIT_PARAMETER = "_commerce_limit";

    private ResourceMapper<T> resourceMapper;
    private GraphqlDataService graphqlDataService;
    private ObjectMapper jsonMapper;

    GraphqlQueryLanguageProvider(ResourceMapper<T> resourceMapper, GraphqlDataService graphqlDataService) {
        this.resourceMapper = resourceMapper;
        this.graphqlDataService = graphqlDataService;
        jsonMapper = new ObjectMapper();
    }

    @Override
    public String[] getSupportedLanguages(ResolveContext<T> paramResolveContext) {
        return SUPPORTED_LANGUAGES;
    }

    @Override
    public Iterator<Resource> findResources(ResolveContext<T> ctx, String query, String language) {
        if (!VIRTUAL_PRODUCT_QUERY_LANGUAGE.equals(language)) {
            return null;
        }

        Map<String, Object> queryParameters = stringToMap(query);

        String fulltext = getFullText(queryParameters);
        Integer offset = NumberUtils.createInteger(
            (queryParameters.get(OFFSET_PARAMETER) == null) ? "" : queryParameters.get(OFFSET_PARAMETER).toString());
        Integer limit = NumberUtils.createInteger(
            (queryParameters.get(LIMIT_PARAMETER) == null) ? "" : queryParameters.get(LIMIT_PARAMETER).toString());

        // Convert offset and limit to current page. We use the limit as the "page size".
        // We assume that offset % limit = 0
        Integer currentPage = Integer.valueOf(1); // Magento paging starts with page 1
        if (offset != null && limit != null && offset.intValue() > 0 && limit.intValue() > 0) {
            currentPage = (offset.intValue() / limit.intValue()) + 1;
        }

        List<ProductInterface> products = graphqlDataService.searchProducts(fulltext, currentPage, limit);
        List<Resource> resources = new ArrayList<>();
        for (ProductInterface product : products) {
            List<CategoryInterface> categories = product.getCategories();
            if (categories != null && !categories.isEmpty()) {
                for (CategoryInterface category : categories) {
                    String path = resourceMapper.getRoot() + "/" + category.getUrlPath() + "/" + product.getSku();
                    resources.add(new ProductResource(ctx.getResourceResolver(), path, product));
                }
            } else {
                String path = resourceMapper.getRoot() + "/" + product.getSku();
                resources.add(new ProductResource(ctx.getResourceResolver(), path, product));
            }
        }

        return resources.iterator();
    }

    private String getFullText(Map<String, Object> queryParams) {
        Set<Map.Entry<String, Object>> entries = queryParams.entrySet();
        Iterator<Map.Entry<String, Object>> it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if (entry.getKey().contains(FULLTEXT_PARAMETER)) {
                if (entry.getValue() == null) {
                    continue;
                }
                if (entry.getValue() instanceof String) {
                    return entry.getValue().toString();
                } else if (entry.getValue() instanceof List) {
                    return StringUtils.join((List<String>) entry.getValue(), ' ');
                }
            }
        }
        return null;
    }

    @Override
    public Iterator<ValueMap> queryResources(ResolveContext<T> paramResolveContext, String query, String language) {
        return null;
    }

    private Map<String, Object> stringToMap(String s) {
        try {
            return jsonMapper.readValue(s, new TypeReference<HashMap<String, Object>>() {});
        } catch (Exception e) {
            LOGGER.error("Cannot deserialize query string: " + s, e);
            return Collections.emptyMap();
        }
    }
}
