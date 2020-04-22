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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.graphql.magento.GraphqlDataService;
import com.adobe.cq.commerce.magento.graphql.CategoryTree;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.google.common.collect.Lists;

class ResourceMapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceMapper.class);
    private String root;
    private GraphqlDataService graphqlDataService;
    private Integer rootCategoryId;
    private String storeView;

    ResourceMapper(String root, GraphqlDataService graphqlDataService, Map<String, String> properties) {
        this.root = root;
        this.graphqlDataService = graphqlDataService;

        // Get Magento store view property
        storeView = properties.get(Constants.MAGENTO_STORE_PROPERTY);

        // Get root category id
        rootCategoryId = Integer.valueOf(properties.get(Constants.MAGENTO_ROOT_CATEGORY_ID_PROPERTY));
    }

    String getRoot() {
        return root;
    }

    CategoryResource resolveCategory(ResourceResolver resolver, String path) {
        // Example for path: /var/commerce/products/cloudcommerce/Men/Coats
        // Remove root (/var/commerce/products/cloudcommerce) then try to find the category path Men/Coats
        String subPath = path.substring(root.length() + 1);
        if (StringUtils.isNotBlank(subPath)) {
            Pair<String, Long> epoch = toEpoch(subPath);
            CategoryTree category = graphqlDataService.getCategoryByPath(epoch.getLeft(), storeView, epoch.getRight());
            if (category != null) {
                return new CategoryResource(resolver, path, category);
            }
        }
        return null;
    }

    ProductResource resolveProduct(ResourceResolver resolver, String path) {
        List<String> productParts = resolveProductParts(path);

        try {

            // We get productParts[] like:
            // Variant lookup: [meskwielt, meskwielt.2-l]
            // Product lookup: [meskwielt]
            // --> we always use the SKU of the base/parent product to get the product
            // --> and use the 2nd part (if any) to select the right variant

            String sku = productParts.get(0);
            String subPath = path.substring(root.length() + 1);
            ProductInterface product = graphqlDataService.getProductBySku(sku, storeView, toEpoch(subPath).getRight());
            if (product != null && product.getId() != null) {
                boolean isVariant = productParts.size() > 1;
                return new ProductResource(resolver, path, product, isVariant ? productParts.get(1) : null);
            }
        } catch (Exception e) {
            LOGGER.error("Error while fetching category products", e);
            return null;
        }

        return null;
    }

    SyntheticImageResource resolveProductImage(ResourceResolver resolver, String path) {
        String productPath = path.substring(0, path.length() - "/image".length());
        List<String> productParts = resolveProductParts(productPath);
        try {
            String sku = productParts.size() == 1 ? productParts.get(0) : productParts.get(1);
            String subPath = path.substring(root.length() + 1);
            ProductInterface product = graphqlDataService.getProductBySku(sku, storeView, toEpoch(subPath).getRight());
            if (product != null) {
                String imageUrl = product.getImage().getUrl();
                if (imageUrl == null && product instanceof ConfigurableProduct) {
                    ConfigurableProduct cp = (ConfigurableProduct) product;
                    if (cp.getVariants() != null && cp.getVariants().size() > 0) {
                        imageUrl = cp.getVariants().get(0).getProduct().getImage().getUrl();
                    }
                }

                if (imageUrl != null) {
                    return new SyntheticImageResource(resolver, path, SyntheticImageResource.IMAGE_RESOURCE_TYPE, imageUrl);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error while fetching category products", e);
            return null;
        }

        return null;
    }

    private List<String> resolveProductParts(String path) {
        // To speedup the lookup, we try to find the longest possible category path
        // Example for path: /var/commerce/products/cloudcommerce/Men/Coats/meskwielt.1-s
        // Remove root (/var/commerce/products/cloudcommerce) then try to find category /Men/Coats
        // --> we find the category /Men/Coats and try to fetch the product meskwielt.1-s

        // Example for path: /var/commerce/products/cloudcommerce/Men/Coats/meskwielt.1-s/meskwielt.2-l
        // Remove root (/var/commerce/products/cloudcommerce) then try to find category /Men/Coats/meskwielt.1-s
        // --> that category is not found, so we try to find the category /Men/Coats
        // --> we find the category /Men/Coats and try to fetch the product meskwielt.1-s and variant meskwielt.2-l

        String subPath = path.substring(root.length() + 1);
        Pair<String, Long> epoch = toEpoch(subPath);
        int backtrackCounter = 0;
        List<String> productParts = new ArrayList<>();
        String[] parts = epoch.getLeft().split("/");
        for (String part : Lists.reverse(Arrays.asList(parts))) {
            productParts.add(part);
            backtrackCounter -= part.length() + 1;
            String categorySubPath = StringUtils.substring(epoch.getLeft(), 0, backtrackCounter);
            if (graphqlDataService.getCategoryByPath(categorySubPath, storeView, toEpoch(subPath).getRight()) != null) {
                break;
            }
        }
        productParts = Lists.reverse(productParts);
        return productParts;
    }

    Iterator<Resource> listCategoryChildren(ResourceResolver resolver, Resource parent) {
        String parentPath = parent.getPath();
        String parentCifId = parent.getValueMap().get(Constants.CIF_ID, String.class);
        boolean isRoot = parentPath.equals(root);
        String subPath = isRoot ? "" : parentPath.substring(root.length() + 1);
        Pair<String, Long> epoch = toEpoch(subPath);
        List<Resource> children = new ArrayList<>();
        CategoryTree categoryTree;
        try {
            if (StringUtils.isNotBlank(subPath)) {
                categoryTree = graphqlDataService.getCategoryByPath(epoch.getLeft(), storeView, epoch.getRight());
            } else {
                categoryTree = graphqlDataService.getCategoryById(rootCategoryId, storeView, epoch.getRight());
            }
        } catch (Exception x) {
            List<Resource> list = new ArrayList<>();
            list.add(new ErrorResource(resolver, parent.getPath()));
            return list.iterator();
        }
        if (categoryTree != null) {
            List<CategoryTree> subChildren = categoryTree.getChildren();
            if (subChildren != null) {
                for (CategoryTree child : subChildren) {
                    children.add(new CategoryResource(resolver, parentPath + "/" + child.getUrlKey(), child));
                }
            }
        }

        if (children.isEmpty() && StringUtils.isNotBlank(parentCifId)) {
            try {
                return new CategoryProductsIterator(parent, graphqlDataService, 20, storeView, epoch.getRight());
            } catch (Exception e) {
                LOGGER.error("Error while fetching category products for " + parentPath + " (" + parentCifId + ")", e);
            }
        }

        return children.isEmpty() ? null : children.iterator();
    }

    Iterator<Resource> listProductChildren(ResourceResolver resolver, Resource parent) {
        String sku = parent.getValueMap().get(Constants.SKU, String.class);
        String parentPath = parent.getPath();
        String subPath = parentPath.equals(root) ? "" : parentPath.substring(root.length() + 1);

        try {
            ProductInterface productInterface = graphqlDataService.getProductBySku(sku, storeView, toEpoch(subPath).getRight());
            if (productInterface == null) {
                return null;
            }

            String imageUrl = null;
            if (productInterface.getImage() != null) {
                imageUrl = productInterface.getImage().getUrl();
            }
            List<Resource> children = new ArrayList<>();

            if (productInterface instanceof ConfigurableProduct) {
                ConfigurableProduct product = (ConfigurableProduct) productInterface;
                List<ConfigurableVariant> variants = product.getVariants();
                if (variants != null && !variants.isEmpty()) {
                    for (ConfigurableVariant variant : variants) {
                        SimpleProduct simpleProduct = variant.getProduct();
                        String path = parentPath + "/" + simpleProduct.getSku();
                        children.add(new ProductResource(resolver, path, simpleProduct, simpleProduct.getSku()));

                        if (imageUrl == null && simpleProduct.getImage() != null) {
                            imageUrl = simpleProduct.getImage().getUrl();
                        }
                    }
                }
            }

            if (imageUrl != null) {
                String imagePath = parentPath + "/image";
                Resource imageResource = new SyntheticImageResource(resolver, imagePath, SyntheticImageResource.IMAGE_RESOURCE_TYPE,
                    imageUrl);
                children.add(0, imageResource);
            }

            return children.iterator();
        } catch (Exception e) {
            LOGGER.error("Error while fetching variants for product " + sku, e);
            return null;
        }
    }

    private Pair<String, Long> toEpoch(String path) {
        LOGGER.info("Trying to extract epoch from {}", path);
        if (path.startsWith("_")) {
            String[] parts = path.split("/");
            try {
                String newPath = StringUtils.substringAfter(path, "/");
                Long epoch = Long.parseLong(parts[0].substring(1));
                LOGGER.info("Getting {} --> {}", newPath, epoch);
                return Pair.of(newPath, epoch);
            } catch (NumberFormatException e) {
                LOGGER.error("Cannot parse expected epoch long value", e);
                return Pair.of(path, null);
            }
        }
        return Pair.of(path, null);
    }

}
