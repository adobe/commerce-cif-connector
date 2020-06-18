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

/*
 * Actions for the preview of product detail pages.
 */

window.CIF = window.CIF || {};
window.CIF.PagePreview = {};

(function(window, document, Granite, $) {
    'use strict';

    var relPdpPreview = '.cq-commerce-pdp-preview-activator';
    var relPlpPreview = '.cq-commerce-plp-preview-activator';
    var productPreviewServletUrl = '/bin/wcm/cif.previewproduct.html';
    var categoryPreviewServletUrl = '/bin/wcm/cif.previewcategory.html';

    var handleProductPreview = function(e, data) {
        handlePreview(data, productPreviewServletUrl);
    };

    var handleCategoryPreview = function(e, data) {
        handlePreview(data, categoryPreviewServletUrl);
    };

    var handlePreview = function(data, previewServletUrl) {
        if (!data) {
            return;
        }

        var selections = data.selections;
        var slugValue = null;
        if (Array.isArray(selections)) {
            if (selections.length > 0 && selections[0]) {
                slugValue = selections[0].value;
            }
        } else if (selections) {
            slugValue = selections.value;
        }

        // slugValue can be: <id>, <slug>, <path>, <sku>, <sku>#<variant_sku>
        // depending on picker's selectionId & filter parameters.
        // However the selection data should contain all available attributes of the selected item
        var [slug, variantSku] = slugValue ? slugValue.split('#') : [];
        if (!slug) {
            return null;
        }

        // prepare all possible parameters
        var params = previewServletUrl === productPreviewServletUrl ? { url_key: slug, sku: slug } : { id: slug };
        if (variantSku) {
            params.variant_sku = variantSku;
        }
        const qs = new URLSearchParams(params);

        window.open(previewServletUrl + '?' + qs.toString());
    };

    Granite.$(document).on('cifProductPickerSelection', relPdpPreview, handleProductPreview);
    Granite.$(document).on('cifCategoryPickerSelection', relPlpPreview, handleCategoryPreview);

    window.CIF.PagePreview = {
        handleProductPreview: handleProductPreview,
        handleCategoryPreview: handleCategoryPreview
    };
})(window, document, Granite);
