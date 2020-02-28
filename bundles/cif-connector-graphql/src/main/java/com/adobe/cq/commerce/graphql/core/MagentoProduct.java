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

package com.adobe.cq.commerce.graphql.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.api.VariantFilter;
import com.adobe.cq.commerce.graphql.resource.SyntheticImageResource;
import com.adobe.cq.commerce.magento.graphql.ComplexTextValue;
import com.adobe.cq.commerce.magento.graphql.ConfigurableProduct;
import com.adobe.cq.commerce.magento.graphql.ConfigurableVariant;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.adobe.cq.commerce.magento.graphql.SimpleProduct;
import com.day.cq.commons.ImageResource;

import static com.adobe.cq.commerce.graphql.resource.Constants.PRODUCT;
import static com.adobe.cq.commerce.graphql.resource.Constants.VARIANT;

public class MagentoProduct implements Product {

    private String path;
    private ProductInterface product;
    private SimpleProduct masterVariant;
    private String activeVariantSku;
    private ResourceResolver resourceResolver;

    /**
     * Creates an instance of a base product and all its variants.
     * 
     * @param resourceResolver The resource resolver for that resource.
     * @param path The resource path of this product.
     * @param product The Magento base product.
     */
    public MagentoProduct(ResourceResolver resourceResolver, String path, ProductInterface product) {
        this(resourceResolver, path, product, null);
    }

    /**
     * Creates an instance of a *variant* product and all its variants.<br>
     * All the methods that access properties (e.g. prices, descriptions, etc) defined in both the active variant
     * and the base product will first get the properties defined in the variant and will fallback to the
     * property of the base product if a variant property is null.
     * 
     * @param resourceResolver The resource resolver for that resource.
     * @param path The resource path of this product.
     * @param product The Magento base product.
     * @param activeVariantSku The SKU of the "active" variant product or null if this product represents the base product.
     */
    public MagentoProduct(ResourceResolver resourceResolver, String path, ProductInterface product, String activeVariantSku) {
        this.resourceResolver = resourceResolver;
        this.path = path;
        this.product = product;
        this.activeVariantSku = activeVariantSku;
        if (product instanceof ConfigurableProduct) {
            ConfigurableProduct cp = (ConfigurableProduct) product;
            if (cp.getVariants() != null && cp.getVariants().size() > 0) {
                if (activeVariantSku != null) {
                    masterVariant = cp.getVariants()
                        .stream()
                        .map(cv -> cv.getProduct())
                        .filter(sp -> activeVariantSku.equals(sp.getSku()))
                        .findFirst()
                        .orElse(null);
                }

                // fallback + default
                if (masterVariant == null) {
                    masterVariant = cp.getVariants().get(0).getProduct();
                }
            }
        }
    }

    private String getVariantOrBaseProperty(Supplier<String> productProvider, Supplier<String> masterVariantProvider) {
        String productValue = productProvider != null ? productProvider.get() : null;
        String masterVariantValue = masterVariantProvider != null ? masterVariantProvider.get() : null;
        if (activeVariantSku != null) {
            return StringUtils.isNotEmpty(masterVariantValue) ? masterVariantValue : productValue;
        } else {
            return StringUtils.isNotEmpty(productValue) ? productValue : masterVariantValue;
        }
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public String getPagePath() {
        return null;
    }

    @Override
    public String getSKU() {
        return getVariantOrBaseProperty(product::getSku, masterVariant != null ? masterVariant::getSku : null);
    }

    @Override
    public String getTitle() {
        return getTitle(null);
    }

    @Override
    public String getTitle(String selectorString) {
        return getVariantOrBaseProperty(product::getName, masterVariant != null ? masterVariant::getName : null);
    }

    @Override
    public String getDescription() {
        return getDescription(null);
    }

    @Override
    public String getDescription(String selectorString) {
        return getVariantOrBaseProperty(
            () -> {
                ComplexTextValue description = product.getDescription();
                return description == null ? null : description.getHtml();
            },
            masterVariant != null ? (() -> {
                ComplexTextValue description = masterVariant.getDescription();
                return description == null ? null : description.getHtml();
            }) : null);
    }

    @Override
    public String getThumbnailUrl() {
        return getVariantOrBaseProperty(
            () -> product.getThumbnail().getUrl(),
            masterVariant != null ? (() -> masterVariant.getThumbnail().getUrl()) : null);
    }

    @Override
    public String getThumbnailUrl(int width) {
        return getThumbnailUrl();
    }

    @Override
    public String getThumbnailUrl(String selectorString) {
        return getThumbnailUrl();
    }

    @Deprecated
    @Override
    public ImageResource getThumbnail() {
        return null;
    }

    @Override
    public Resource getAsset() {
        String url = getImageUrl();
        return new SyntheticResource(resourceResolver, url, SyntheticImageResource.IMAGE_RESOURCE_TYPE);
    }

    @Override
    public List<Resource> getAssets() {
        return Collections.singletonList(getAsset()); // TODO: add more Magento images?
    }

    @Deprecated
    @Override
    public String getImageUrl() {
        return getVariantOrBaseProperty(
            () -> product.getImage().getUrl(),
            masterVariant != null ? (() -> masterVariant.getImage().getUrl()) : null);
    }

    @Override
    public ImageResource getImage() {
        String url = getImageUrl();
        return new SyntheticImageResource(resourceResolver, path + "/image", SyntheticImageResource.IMAGE_RESOURCE_TYPE, url);
    }

    @Override
    public List<ImageResource> getImages() {
        return Collections.singletonList(getImage()); // TODO: add more Magento images?
    }

    @Override
    public <T> T getProperty(String name, Class<T> type) {
        return getProperty(name, null, type);
    }

    @SuppressWarnings("unchecked")
    private <T> T getProperty(String name, ProductInterface p) {
        Method[] methods = p.getClass().getDeclaredMethods();
        for (Method method : methods) {
            String methodName = method.getName().toLowerCase();
            String getName = "get" + name.toLowerCase();
            String isName = "is" + name.toLowerCase();
            if (methodName.equals(getName) || methodName.equals(isName)) {
                try {
                    return (T) method.invoke(p);
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public <T> T getProperty(String name, String selectorString, Class<T> type) {
        T productValue = getProperty(name, product);
        T masterVariantValue = masterVariant != null ? getProperty(name, masterVariant) : null;
        if (activeVariantSku != null) {
            return masterVariantValue != null ? masterVariantValue : productValue;
        } else {
            return productValue != null ? productValue : masterVariantValue;
        }
    }

    @Override
    public Iterator<String> getVariantAxes() {
        return Collections.emptyIterator();
    }

    @Override
    public boolean axisIsVariant(String s) {
        return false;
    }

    @Override
    public Iterator<Product> getVariants() throws CommerceException {
        return getVariants(null);
    }

    @Override
    public Iterator<Product> getVariants(VariantFilter variantFilter) throws CommerceException {
        if (product instanceof ConfigurableProduct) {
            ConfigurableProduct cp = (ConfigurableProduct) product;
            if (cp.getVariants() != null && cp.getVariants().size() > 0) {
                ArrayList<Product> variants = new ArrayList<>();
                for (ConfigurableVariant cv : cp.getVariants()) {
                    String variantSku = cv.getProduct().getSku();
                    String variantPath = path + "/" + variantSku;
                    Product variant = new MagentoProduct(resourceResolver, variantPath, product, variantSku);
                    if (variantFilter == null || variantFilter.includes(variant)) {
                        variants.add(variant);
                    }
                }
                return variants.iterator();
            }
        }

        return Collections.emptyIterator();
    }

    @Override
    public Product getBaseProduct() throws CommerceException {
        return this;
    }

    @Override
    public Product getPIMProduct() throws CommerceException {
        return this;
    }

    @Deprecated
    @Override
    public String getImagePath() {
        return null;
    }

    public static boolean isAProductOrVariant(Resource resource) {
        String commerceType = resource.getValueMap().get(CommerceConstants.PN_COMMERCE_TYPE, "");
        return commerceType.equals(PRODUCT) || commerceType.equals(VARIANT);
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> aClass) {
        return null;
    }
}
