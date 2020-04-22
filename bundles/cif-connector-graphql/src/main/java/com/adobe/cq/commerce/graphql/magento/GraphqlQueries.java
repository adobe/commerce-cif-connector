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

package com.adobe.cq.commerce.graphql.magento;

import java.util.function.Function;

import com.adobe.cq.commerce.magento.graphql.CategoryTreeQuery;
import com.adobe.cq.commerce.magento.graphql.ProductInterfaceQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.ProductPricesQueryDefinition;
import com.adobe.cq.commerce.magento.graphql.SimpleProductQueryDefinition;

class GraphqlQueries {

    /**
     * Generic query for product price
     */
    static final ProductPricesQueryDefinition PRODUCT_PRICE_QUERY = q -> q
        .regularPrice(rp -> rp
            .amount(a -> a
                .currency()
                .value()));

    /**
     * Generic query for simple product
     */
    static final SimpleProductQueryDefinition SIMPLE_PRODUCT_QUERY = q -> q
        .id()
        .sku()
        .name()
        .description(d -> d.html())
        .image(i -> i.url())
        .thumbnail(t -> t.url())
        .urlKey()
        .updatedAt()
        .createdAt()
        .price(PRODUCT_PRICE_QUERY);

    /**
     * Generic query for configurable product including variants
     */
    static final ProductInterfaceQueryDefinition CONFIGURABLE_PRODUCT_QUERY = q -> q
        .id()
        .sku()
        .name()
        .description(d -> d.html())
        .image(i -> i.url())
        .thumbnail(t -> t.url())
        .urlKey()
        .updatedAt()
        .createdAt()
        .price(PRODUCT_PRICE_QUERY)
        .categories(c -> c.urlPath())
        .onConfigurableProduct(cp -> cp
            .variants(v -> v
                .product(SIMPLE_PRODUCT_QUERY)));

    /**
     * Query for the direct product children of a category.
     */
    static final ProductInterfaceQueryDefinition CHILD_PRODUCT_QUERY = q -> q
        .id()
        .sku()
        .name()
        .urlKey()
        .updatedAt()
        .thumbnail(t -> t.url());

    /**
     * Generic "lambda" query for category tree WITHOUT "children" part.
     * The "children" part cannot be added because it would otherwise introduce an infinite recursion.
     */
    static final Function<CategoryTreeQuery, CategoryTreeQuery> CATEGORY_LAMBDA = q -> q
        .id()
        .name()
        .urlPath()
        .urlKey()
        .productCount()
        .childrenCount();

    /**
     * Query for searching categories.
     */
    static final Function<CategoryTreeQuery, CategoryTreeQuery> CATEGORY_SEARCH_QUERY = q -> q
        .id()
        .name()
        .urlPath()
        .urlKey();
}
