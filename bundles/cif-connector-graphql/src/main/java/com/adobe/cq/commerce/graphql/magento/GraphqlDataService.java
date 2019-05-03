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

import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

public interface GraphqlDataService {

    public String getIdentifier();

    /**
     * Fetches a product by sku.
     * 
     * @param sku The product SKU.
     * @return The Magento GraphQL product or null if the product is not found.
     * @exception RuntimeException if the GraphQL HTTP request does not return 200 or if the JSON response cannot be parsed or deserialized.
     */
    public ProductInterface getProductBySku(String sku);

    /**
     * Performs a full-text search and returns the matching products.
     * 
     * @param text The full-text search.
     * @param currentPage Specifies which page of results to return. The default value is 1.
     * @param pageSize Specifies the maximum number of results to return at once. This attribute is optional.
     * @return The list of matching products or an empty list if the search doesn't match any product.
     * @exception RuntimeException if the GraphQL HTTP request does not return 200 or if the JSON response cannot be parsed or deserialized.
     */
    public List<ProductInterface> searchProducts(String text, Integer currentPage, Integer pageSize);

    /**
     * Returns the Magento category tree for the given category id.
     * 
     * @param categoryId The category id.
     * @return The category tree starting at the given category.
     * @exception RuntimeException if the GraphQL HTTP request does not return 200 or if the JSON response cannot be parsed or deserialized.
     */
    public CategoryTree getCategoryTree(Integer categoryId);

    /**
     * Returns the products of the given category id.
     * 
     * @param categoryId The category id.
     * @return The list of products for this category.
     * @exception RuntimeException if the GraphQL HTTP request does not return 200 or if the JSON response cannot be parsed or deserialized.
     */
    public List<ProductInterface> getCategoryProducts(Integer categoryId);

    /**
     * Returns the OSGi configuration of that GraphQL client.
     * 
     * @return The OSGi configuration of this instance.
     */
    public GraphqlDataServiceConfiguration getConfiguration();

}
