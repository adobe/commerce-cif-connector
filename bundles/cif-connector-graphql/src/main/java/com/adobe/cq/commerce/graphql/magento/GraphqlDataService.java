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

import java.util.List;

import com.adobe.cq.commerce.magento.graphql.CategoryProducts;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

public interface GraphqlDataService {

    public String getIdentifier();

    /**
     * Fetches a product by sku.
     *
     * @param sku The product SKU.
     * @param storeView An optional Magento store view, can be null.
     * @return The Magento GraphQL product or null if the product is not found.
     * @throws RuntimeException if the GraphQL HTTP request does not return 200 or if the JSON response cannot be parsed or deserialized.
     */
    public ProductInterface getProductBySku(String sku, String storeView);

    /**
     * Fetches a category by identifier.
     *
     * @param id the identifier of the category
     * @param storeView an optional Magento store view, can be null
     * @return the Magento GraphQL category or null if the category is not found
     * @throws RuntimeException if the GraphQL HTTP request does not return 200 or if the JSON response cannot be parsed or deserialized
     */
    public CategoryTree getCategoryById(Integer id, String storeView);

    /**
     * Fetches a category by URL path.
     *
     * @param urlPath the URL path of the category
     * @param storeView an optional Magento store view, can be null
     *
     * @return the Magento GraphQL category or null if the category is not found
     * @throws RuntimeException if the GraphQL HTTP request does not return 200 or if the JSON response cannot be parsed or deserialized
     */
    public CategoryTree getCategoryByPath(String urlPath, String storeView);

    /**
     * Performs a full-text search and returns the matching products.
     *
     * @param text The full-text search.
     * @param categoryId identifier of the category at the root of category tree where the search is performed
     * @param currentPage Specifies which page of results to return. The default value is 1.
     * @param pageSize Specifies the maximum number of results to return at once. This attribute is optional.
     * @param storeView An optional Magento store view, can be null.
     * @return The list of matching products or an empty list if the search doesn't match any product.
     * @throws RuntimeException if the GraphQL HTTP request does not return 200 or if the JSON response cannot be parsed or deserialized.
     */
    public List<ProductInterface> searchProducts(String text, Integer categoryId, Integer currentPage, Integer pageSize, String storeView);

    /**
     * Performs a full-text search and returns the matching categories.
     *
     * @param text The full-text search.
     * @param categoryId identifier of the category at the root of category tree where the search is performed
     * @param currentPage Specifies which page of results to return. The default value is 1.
     * @param pageSize Specifies the maximum number of results to return at once. This attribute is optional.
     * @param storeView An optional Magento store view, can be null.
     * @return The list of matching categories or an empty list if the search doesn't match any category.
     * @throws RuntimeException if the GraphQL HTTP request does not return 200 or if the JSON response cannot be parsed or deserialized.
     */
    public List<CategoryTree> searchCategories(String text, Integer categoryId, Integer currentPage, Integer pageSize, String storeView);

    /**
     * Returns the paginated products results for the given category id and pagination arguments.
     *
     * @param categoryId The category id.
     * @param currentPage The current page number to be fetched, Magento pagination starts with page 1.
     * @param pageSize The page size.
     * @param storeView An optional Magento store view, can be null.
     * @return The list of products for this category.
     * @throws RuntimeException if the GraphQL HTTP request does not return 200 or if the JSON response cannot be parsed or deserialized.
     */
    public CategoryProducts getCategoryProducts(Integer categoryId, Integer currentPage, Integer pageSize, String storeView);
}
