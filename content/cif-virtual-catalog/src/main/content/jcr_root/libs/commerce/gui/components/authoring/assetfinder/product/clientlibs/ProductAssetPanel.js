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

(function ($, ns, channel, window, undefined) {

    var self = {},
        name = 'Products',
        insertAfter = 'Documents';

    // make the loadAssets function more flexible
    self.searchRoot = '/var/commerce/products';

    var searchPath = self.searchRoot,
        productServlet = '/bin/wcm/contentfinder/cifproduct/view.html',
        itemResourceType = 'commerce/gui/components/authoring/assetfinder/product';

    /**
     *
     * @param query {String} search query
     * @param lowerLimit {Number} lower bound for paging
     * @param upperLimit {Number} upper bound for paging
     * @returns {jQuery.Promise}
     */
    self.loadAssets = function (query, lowerLimit, upperLimit) {
        var param = {
            '_dc': new Date().getTime(),  // cache killer
            'query': query,
            'itemResourceType': itemResourceType, // single item rendering (cards)
            'limit': lowerLimit + ".." + upperLimit,
            '_charset_': 'utf-8'
        };

        return $.ajax({
            type: 'GET',
            dataType: 'html',
            url: Granite.HTTP.externalize(productServlet) + searchPath,
            data: param
        });
    };

    /**
     * Set URL to servlet
     * @param servlet {String} URL to product servlet
     */
    self.setServlet = function (servlet) {
        productServlet = servlet;
    };

    self.setSearchPath = function (path) {
        searchPath = path.replace(/\/$/, ""); // Strip trailing slash
    };

    self.setItemResourceType = function (resourceType) {
        itemResourceType = resourceType;
    };

    self.resetSearchPath = function () {
        searchPath = self.searchRoot;
    };

    // register as a asset tab
    ns.ui.assetFinder.register(name, self, insertAfter);

}(jQuery, Granite.author, jQuery(document), this));
