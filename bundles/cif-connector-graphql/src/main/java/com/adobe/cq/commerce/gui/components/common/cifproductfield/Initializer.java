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

package com.adobe.cq.commerce.gui.components.common.cifproductfield;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.commerce.api.conf.CommerceBasePathsService;
import com.adobe.cq.commerce.graphql.search.CatalogSearchSupport;
import com.adobe.cq.sightly.WCMUsePojo;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ValueMapResourceWrapper;
import com.day.cq.i18n.I18n;

public class Initializer extends WCMUsePojo {

    static final String DEFAULT_FILTER = "folderOrProduct";
    static final String DEFAULT_PICKER_SRC = "/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html";
    static final String DEFAULT_SUGGESTION_SRC = "/mnt/overlay/commerce/gui/content/common/cifproductfield/suggestion{.offset,limit}.html";
    static final String FIELD_SUPER_TYPE = "commerce/gui/components/common/productfield";
    static final boolean DEFAULT_SELECTION_MULTIPLE = false;
    static final String DEFAULT_SELECTION_TYPE = "id";

    @Override
    public void activate() {
        final ValueMap properties = getProperties();
        final I18n i18n = new I18n(getRequest());
        final ExpressionHelper ex = new ExpressionHelper(getSlingScriptHelper().getService(ExpressionResolver.class), getRequest());
        final CommerceBasePathsService cbps = getSlingScriptHelper().getService(CommerceBasePathsService.class);

        // configure default properties for productfield
        String defaultRootPath = new CatalogSearchSupport(getResourceResolver()).findCatalogPathForPicker(getRequest());
        if (StringUtils.isBlank(defaultRootPath)) {
            defaultRootPath = cbps.getProductsBasePath();
        }
        final String rootPath = ex.getString(properties.get("rootPath", defaultRootPath));
        final String filter = properties.get("filter", DEFAULT_FILTER);
        final boolean multiple = properties.get("multiple", DEFAULT_SELECTION_MULTIPLE);
        final String selectionId = properties.get("selectionId", DEFAULT_SELECTION_TYPE);
        final String defaultEmptyText;
        if ("path".equals(selectionId)) {
            defaultEmptyText = "Product path";
        } else if ("sku".equals(selectionId)) {
            defaultEmptyText = "Product SKU";
        } else if ("slug".equals(selectionId)) {
            defaultEmptyText = "Product slug";
        } else if ("combinedSku".equals(selectionId)) {
            defaultEmptyText = "Product SKU(s) separated by # character";
        } else {
            defaultEmptyText = "Product ID";
        }
        final String emptyText = i18n.getVar(properties.get("emptyText", i18n.get(defaultEmptyText)));

        final String selectionCount = multiple ? "multiple" : "single";
        String pickerSrc = DEFAULT_PICKER_SRC + "?root=" + Text.escape(rootPath) + "&filter=" + Text.escape(filter) + "&selectionCount="
            + selectionCount + "&selectionId=" + Text.escape(selectionId);
        String suggestionSrc = DEFAULT_SUGGESTION_SRC + "?root=" + Text.escape(rootPath) + "&filter=product{&query}";
        pickerSrc = properties.get("pickerSrc", pickerSrc);
        suggestionSrc = properties.get("suggestionSrc", suggestionSrc);

        // suggestions disabled
        suggestionSrc = "";

        ValueMapResourceWrapper wrapper = new ValueMapResourceWrapper(getResource(), FIELD_SUPER_TYPE);
        ValueMap wrapperProperties = wrapper.adaptTo(ValueMap.class);
        wrapperProperties.putAll(properties);
        wrapperProperties.put("rootPath", rootPath);
        wrapperProperties.put("filter", filter);
        wrapperProperties.put("multiple", multiple);
        wrapperProperties.put("pickerSrc", pickerSrc);
        wrapperProperties.put("suggestionSrc", suggestionSrc);
        wrapperProperties.put("emptyText", emptyText);
        wrapperProperties.put("forceselection", true);

        getSlingScriptHelper().include(wrapper);
    }

}
