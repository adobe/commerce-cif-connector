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

'use strict';

describe('CifProductPicker', () => {
    var clickActivator = window.CIF.CifProductPicker.clickActivator;
    var event = { preventDefault: function() {} };
    var dollar = Granite.$;
    var button;

    before(() => {
        var body = document.querySelector('body');
        body.insertAdjacentHTML(
            'afterbegin',
            `<button class="cq-commerce-cifproductpicker-activator cq-commerce-pdp-preview-activator" 
                          type="button" 
                          is="coral-button" 
                          variant="secondary"><coral-button-label>View with Product</coral-button-label></button>`
        );
        button = document.querySelector('.cq-commerce-cifproductpicker-activator');
        dollar.ajax = function(asd) {
            return Promise.resolve(asd);
        };
    });

    beforeEach(() => {
        sinon.spy(dollar, 'ajax');
    });

    afterEach(() => {
        dollar.ajax.restore();
    });

    it('provides defaults settings for the picker', () => {
        clickActivator(event);
        verifyCall(
            '/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=/var/commerce/products&filter=folderOrProduct&selectionCount=single&selectionId=id'
        );
    });

    it('supports custom pickersrc', () => {
        button.setAttribute('data-pickersrc', 'mypickersrc');
        clickActivator(event);
        verifyCall('mypickersrc');
        button.removeAttribute('data-pickersrc');
    });

    it('supports custom root path', () => {
        button.setAttribute('data-root', '/my/path');
        clickActivator(event);
        verifyCall(
            '/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=/my/path&filter=folderOrProduct&selectionCount=single&selectionId=id'
        );
        button.removeAttribute('data-root');
    });

    it('supports custom selectioncount', () => {
        button.setAttribute('data-selectioncount', 'multiple');
        clickActivator(event);
        verifyCall(
            '/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=/var/commerce/products&filter=folderOrProduct&selectionCount=multiple&selectionId=id'
        );
        button.removeAttribute('data-selectioncount');
    });

    it('supports custom selectionid', () => {
        button.setAttribute('data-selectionid', 'slug');
        clickActivator(event);
        verifyCall(
            '/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=/var/commerce/products&filter=folderOrProduct&selectionCount=single&selectionId=slug'
        );
        button.removeAttribute('data-selectionid');
    });

    it('supports custom filter', () => {
        button.setAttribute('data-filter', 'myfilter');
        clickActivator(event);
        verifyCall(
            '/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=/var/commerce/products&filter=myfilter&selectionCount=single&selectionId=id'
        );
        button.removeAttribute('data-filter');
    });

    function verifyCall(url) {
        assert.isTrue(dollar.ajax.calledOnce);
        assert.equal(url, dollar.ajax.getCall(0).args[0].url);
    }
});
