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
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.graphql.magento.GraphqlDataService;
import com.adobe.cq.commerce.magento.graphql.CategoryInterface;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.commons.inherit.InheritanceValueMap;
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
    private String storeView;

    GraphqlQueryLanguageProvider(ResourceMapper<T> resourceMapper, GraphqlDataService graphqlDataService, InheritanceValueMap properties) {
        this.resourceMapper = resourceMapper;
        this.graphqlDataService = graphqlDataService;
        jsonMapper = new ObjectMapper();

        if (properties != null) {
            storeView = properties.getInherited(Constants.MAGENTO_STORE_PROPERTY, String.class);
        }
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
        Integer offset = queryParameters.containsKey(OFFSET_PARAMETER) ? Integer.valueOf(queryParameters.get(OFFSET_PARAMETER).toString())
            : Integer.valueOf(0);
        Integer limit = queryParameters.containsKey(LIMIT_PARAMETER) ? Integer.valueOf(queryParameters.get(LIMIT_PARAMETER).toString())
            : Integer.valueOf(20);

        LOGGER.debug("Performing product search with '{}' (offset: {}, limit: {})", fulltext, offset, limit);

        // Convert offset and limit to Magento page number and size
        Pair<Integer, Integer> pagination = toMagentoPageNumberAndSize(offset, limit);

        List<ProductInterface> products = graphqlDataService.searchProducts(fulltext, pagination.getLeft(), pagination.getRight(),
            storeView);

        // The Magento page might start before 'offset' and be bigger than 'limit', so we extract exactly what we need
        int start = offset - ((pagination.getLeft() - 1) * pagination.getRight());
        int end = start + limit;
        if (start >= 0 && end <= products.size()) {
            products = products.subList(start, end);
        }

        LOGGER.debug("Returning {} products", products.size());

        List<Resource> resources = new ArrayList<>();
        for (ProductInterface product : products) {
            List<CategoryInterface> categories = product.getCategories();
            if (categories != null && !categories.isEmpty()) {
                CategoryInterface category = categories.stream().filter(c -> c.getUrlPath() != null).findFirst().orElse(null);
                if (category != null) {
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

    /**
     * This method calculates the best possible matching Magento page number and size that
     * returns a range of items that "encompasses" the given offset and limit. The corresponding
     * page might start before the <code>offset</code> and might contain more than <code>limit</code> products.
     * This means that one might have to "re-extract" the right range of products from the page.
     * 
     * @param offset The range offset.
     * @param limit The range limit.
     * @return A page number/size pair encompassing the given range.
     */
    protected static Pair<Integer, Integer> toMagentoPageNumberAndSize(int offset, int limit) {
        if (offset % limit == 0) {
            return Pair.of((offset / limit) + 1, limit); // page number and size perfectly matches offset and limit
        } else {
            // Finds a Magento (page,size) that encompasses the AEM (offset,limit) range
            // AEM (20,11) |------ offset ------|-- limit --|
            // Mag. (2,16) |---- page 1 ----|---- page 2 ----|
            int total = offset + limit;
            for (int size = limit; size < total; size++) {
                int min = offset - (offset % size);
                int max = min + size;
                if (min <= offset && max >= total) {
                    return Pair.of((min / size) + 1, size);
                }
            }
            return Pair.of(1, total); // Worst is one single page of size total
        }
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
