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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.cq.commerce.graphql.core.MagentoProduct;
import com.adobe.cq.commerce.magento.graphql.Money;
import com.adobe.cq.commerce.magento.graphql.ProductInterface;
import com.day.cq.commons.jcr.JcrConstants;

import static com.adobe.cq.commerce.graphql.resource.Constants.PRODUCT;
import static com.adobe.cq.commerce.graphql.resource.Constants.SKU;
import static com.adobe.cq.commerce.graphql.resource.Constants.SLUG;
import static com.adobe.cq.commerce.graphql.resource.Constants.VARIANT;

class ProductResource extends SyntheticResource {

    protected static final String PRODUCT_RESOURCE_TYPE = "commerce/components/product";
    protected static final String PRODUCT_IDENTIFIER = "identifier";
    protected static final String PRODUCT_SUMMARY = "summary";
    protected static final String PRODUCT_FEATURES = "features";

    private ProductInterface magentoProduct;
    private String activeVariantSku;
    private ValueMap values;

    /**
     * Creates a SyntheticResource for the given Magento GraphQL product.
     * 
     * @param resourceResolver The resource resolver.
     * @param path The path of the resource.
     * @param product The Magento GraphQL product.
     */
    ProductResource(ResourceResolver resourceResolver, String path, ProductInterface product) {
        this(resourceResolver, path, product, null);
    }

    /**
     * Creates a SyntheticResource for the given Magento GraphQL product. The <code>activeVariantSku</code> parameter
     * must be set if the product is a leaf variant not having any child nodes.
     * 
     * @param resourceResolver The resource resolver.
     * @param path The path of the resource.
     * @param product The Magento GraphQL product.
     * @param activeVariantSku The SKU of the "active" variant product or null if this product represents the base product.
     */
    ProductResource(ResourceResolver resourceResolver, String path, ProductInterface product, String activeVariantSku) {

        super(resourceResolver, path, PRODUCT_RESOURCE_TYPE);
        this.magentoProduct = product;
        this.activeVariantSku = activeVariantSku;

        Map<String, Object> map = new HashMap<>();
        map.put(JcrConstants.JCR_TITLE, product.getName());
        map.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
        map.put(JcrConstants.JCR_DESCRIPTION, product.getDescription().getHtml());
        map.put(PRODUCT_IDENTIFIER, product.getId());
        map.put(SKU, activeVariantSku != null ? activeVariantSku : product.getSku());
        map.put(SLUG, product.getUrlKey());
        map.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, PRODUCT_RESOURCE_TYPE);
        map.put(JcrConstants.JCR_LASTMODIFIED, convertToDate(product.getUpdatedAt()));
        map.put(CommerceConstants.PN_COMMERCE_PROVIDER, Constants.MAGENTO_GRAPHQL_PROVIDER);

        String formattedPrice = toFormattedPrice(product);
        if (StringUtils.isNotBlank(formattedPrice)) {
            map.put(Constants.PRODUCT_FORMATTED_PRICE, formattedPrice);
        }

        map.put(CommerceConstants.PN_COMMERCE_TYPE, activeVariantSku != null ? VARIANT : PRODUCT);

        values = new ValueMapDecorator(map);
    }

    private Date convertToDate(String magentoDate) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return inputFormat.parse(magentoDate);
        } catch (ParseException e) {
            return null;
        }
    }

    private String toFormattedPrice(ProductInterface product) {
        Money money = product.getPrice().getRegularPrice().getAmount();
        return money.getCurrency() + " " + money.getValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        if (type == ValueMap.class) {
            return (AdapterType) values;
        } else if (type == Product.class) {
            return (AdapterType) new MagentoProduct(getResourceResolver(), getPath(), magentoProduct, activeVariantSku);
        }

        return super.adaptTo(type);
    }
}