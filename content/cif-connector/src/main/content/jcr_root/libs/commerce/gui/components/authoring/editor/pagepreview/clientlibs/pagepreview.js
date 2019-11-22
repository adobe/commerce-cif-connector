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

    var handlePdpPreview = function(e, data) {
        if (!data) {
            return;
        }

        var selections = data.selections;
        var productSlug = null;
        if (Array.isArray(selections)) {
            if (selections.length > 0 && selections[0]) {
                productSlug = selections[0].value;
            }
        } else if (selections) {
            productSlug = selections.value;
        }

        var url = Granite.author.ContentFrame.location;
        var previewUrl = createPreviewUrl(url, productSlug);

        if (previewUrl) {
            window.open(previewUrl);
        }
    };

    var createPreviewUrl = function(url, slug) {
        if (!url || !slug) {
            return null;
        }

        var indLastSlash = url.lastIndexOf('/');
        if (indLastSlash < 0 || indLastSlash === url.length - 1) {
            return null;
        }

        var urlPath = url.slice(0, indLastSlash + 1);
        var urlName = url.substring(indLastSlash + 1);
        var indHtml = urlName.lastIndexOf('.html');
        var previewUrl = null;
        if (indHtml > -1) {
            var indSelector = urlName.slice(0, indHtml).lastIndexOf('.');
            if (indSelector > -1) {
                previewUrl = urlPath + urlName.slice(0, indSelector) + '.' + slug + '.html';
            } else {
                previewUrl = urlPath + urlName.slice(0, indHtml) + '.' + slug + '.html';
            }
        }

        return previewUrl;
    };

    Granite.$(document).on('cifProductPickerSelection', relPdpPreview, handlePdpPreview);

    window.CIF.PagePreview.handlePdpPreview = handlePdpPreview;
    window.CIF.PagePreview.createPreviewUrl = createPreviewUrl;
})(window, document, window.CIF.Granite ? window.CIF.Granite : Granite);
