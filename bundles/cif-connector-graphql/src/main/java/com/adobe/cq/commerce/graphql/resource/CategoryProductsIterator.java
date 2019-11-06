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
import java.util.Iterator;
import java.util.List;

import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.graphql.magento.GraphqlDataService;
import com.adobe.cq.commerce.magento.graphql.CategoryProducts;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;

public class CategoryProductsIterator implements Iterator<Resource> {

    private Resource category;

    private String storeView;
    private Integer categoryId;
    private Integer totalCount;

    private int pageSize;
    private int nextIndex = 0;
    private List<Resource> items = new ArrayList<Resource>();

    private GraphqlDataService graphqlDataService;

    /**
     * Builds an interator that dynamically loads category products "page by page". The loading of products is
     * triggered by calls to {@link #hasNext()}: based on the page size and on the pages already fetched, the iterator
     * will decide if a new page of data must be loaded.
     * 
     * @param category The category resource.
     * @param graphqlDataService The service to fetch categor and product data.
     * @param pageSize The page size when fetching data.
     * @param storeView The Magento store view used to access the Magento catalog.
     */
    public CategoryProductsIterator(Resource category, GraphqlDataService graphqlDataService, Integer pageSize, String storeView) {
        this.category = category;
        this.categoryId = category.getValueMap().get(Constants.CIF_ID, Integer.class);
        this.graphqlDataService = graphqlDataService;
        this.pageSize = pageSize != null ? pageSize : 20;
        this.storeView = storeView;
    }

    @Override
    public boolean hasNext() {

        if (dataIsAvailable()) {
            return true; // We already have the next product in the items list
        }

        if (!canLoadMoreProducts()) {
            return false; // We already fetched all the products
        }

        Integer nextPage = (items.size() / pageSize) + 1; // Magento pagination starts at page 1
        CategoryProducts categoryProducts = graphqlDataService.getCategoryProducts(categoryId, nextPage, pageSize, storeView);
        totalCount = categoryProducts.getTotalCount();

        List<ProductInterface> products = categoryProducts.getItems();
        if (products.size() == 0) {
            return false; // We tried to get more products but the backend returned nothing
        }

        // We fetched the next page and add the products to the items list
        for (ProductInterface product : products) {
            String path = category.getPath() + "/" + product.getSku();
            items.add(new ProductResource(category.getResourceResolver(), path, product));
        }
        return true;
    }

    private boolean canLoadMoreProducts() {
        return totalCount == null || totalCount > items.size();
    }

    private boolean dataIsAvailable() {
        return nextIndex < items.size();
    }

    @Override
    public Resource next() {
        Resource next = items.get(nextIndex);
        nextIndex++;
        return next;
    }

}
