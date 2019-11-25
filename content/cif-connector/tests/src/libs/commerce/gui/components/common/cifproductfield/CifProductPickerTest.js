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

describe('CifProductPickerTest', () => {
    var clickActivator = window.CIF.CifProductPicker.clickActivator;
    var event = { preventDefault: function() {} };
    var dollar = Granite.$;
    var button;

    before(() => {
        var body = document.querySelector('body');
        body.insertAdjacentHTML(
            'afterbegin',
            `<button class="js-editor-PageInfo-closePopover cq-commerce-cifproductpicker-activator cq-commerce-pdp-preview-activator coral3-Button coral3-Button--secondary" 
                          title="View with Product" 
                          type="button" 
                          autocomplete="off" 
                          is="coral-button" 
                          trackingfeature="" 
                          trackingelement="view with product" 
                          tracking="ON" 
                          size="M" 
                          variant="secondary"><coral-button-label>View with Product</coral-button-label></button>`
        );
        button = document.querySelector('.cq-commerce-cifproductpicker-activator');

        dollar.ajax = function(asd) {
            return {
                then: function() {
                    return {
                        then: function() {
                            return {
                                then: function() {}
                            };
                        }
                    };
                }
            };
        };
    });

    beforeEach(() => {
        sinon.spy(dollar, 'ajax');
    });

    afterEach(() => {
        dollar.ajax.restore();
    });

    it('test picker defaults', () => {
        clickActivator(event);
        verifyCall(
            '/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=/var/commerce/products&filter=folderOrProduct&selectionCount=single&selectionId=id'
        );
    });

    it('test pickersrc', () => {
        button.setAttribute('data-pickersrc', 'mypickersrc');
        clickActivator(event);
        verifyCall('mypickersrc');
        button.removeAttribute('data-pickersrc');
    });

    it('test picker root', () => {
        button.setAttribute('data-root', '/my/path');
        clickActivator(event);
        verifyCall(
            '/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=/my/path&filter=folderOrProduct&selectionCount=single&selectionId=id'
        );
        button.removeAttribute('data-root');
    });

    it('test selectioncount', () => {
        button.setAttribute('data-selectioncount', 'multiple');
        clickActivator(event);
        verifyCall(
            '/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=/var/commerce/products&filter=folderOrProduct&selectionCount=multiple&selectionId=id'
        );
        button.removeAttribute('data-selectioncount');
    });

    it('test selectionid', () => {
        button.setAttribute('data-selectionid', 'slug');
        clickActivator(event);
        verifyCall(
            '/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=/var/commerce/products&filter=folderOrProduct&selectionCount=single&selectionId=slug'
        );
        button.removeAttribute('data-selectionid');
    });

    it('test filter', () => {
        button.setAttribute('data-filter', 'myfilter');
        clickActivator(event);
        verifyCall(
            '/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html?root=/var/commerce/products&filter=myfilter&selectionCount=single&selectionId=id'
        );
        button.removeAttribute('data-filter');
    });

    it('test getState()', () => {
        var getState = window.CIF.CifProductPicker.getState;
        var control = Granite.$(document).find(window.CIF.CifProductPicker.relActivator);

        var state = getState(control);

        assert.isNotNull(state);
        assert.isNull(state.el);
        assert.isFalse(state.open);
        assert.isFalse(state.loading);

        state.loading = true;

        var state = getState(control);
        assert.isTrue(state.loading);
    });

    it('test cifProductPicker()', () => {
        var getState = window.CIF.CifProductPicker.getState;
        var cifProductPicker = window.CIF.CifProductPicker.cifProductPicker;
        var control = Granite.$(document).find(window.CIF.CifProductPicker.relActivator);
        var state = getState(control);

        cifProductPicker(control, 'pickersrc', null);

        verifyCall('pickersrc');

        dollar.ajax.restore();
        sinon.spy(dollar, 'ajax');
        state.loading = true;

        cifProductPicker(control, 'pickersrc', null);

        assert.isFalse(dollar.ajax.calledOnce);

        state.loading = false;

        cifProductPicker(control, null, null);

        assert.isFalse(dollar.ajax.calledOnce);

        state.el = control;
        state.open = true;

        cifProductPicker(control, 'pickersrc', null);

        assert.isFalse(dollar.ajax.calledOnce);
    });

    it('test close()', () => {
        var getState = window.CIF.CifProductPicker.getState;
        var close = window.CIF.CifProductPicker.close;
        var control = Granite.$(document).find(window.CIF.CifProductPicker.relActivator);
        var state = getState(control);
        state.api = {
            detach: function() {}
        };
        sinon.spy(state.api, 'detach');

        close(control, state);

        assert.isTrue(state.api.detach.calledOnce);
        assert.isFalse(state.open);

        state.api.detach.restore();

        var callback = function() {};
        var callbackSpy = sinon.spy(callback);

        close(control, state, callbackSpy);

        assert.isTrue(callbackSpy.calledOnce);
    });

    it('test show()', () => {
        var getState = window.CIF.CifProductPicker.getState;
        var show = window.CIF.CifProductPicker.show;
        var control = Granite.$(document).find(window.CIF.CifProductPicker.relActivator);
        var state = getState(control);
        state.api = {
            attach: function() {},
            pick: function() {
                return { then: function() {} };
            }
        };

        state.el = { focus: function() {} };
        sinon.spy(state.api, 'attach');
        sinon.spy(state.api, 'pick');
        sinon.spy(state.el, 'focus');

        show(control, state);
        assert.isTrue(state.api.attach.calledOnce);
        assert.isTrue(state.api.pick.calledOnce);
        assert.isTrue(state.el.focus.calledOnce);

        assert.isFalse(state.loading);
        assert.isTrue(state.open);

        state.api.focus = function() {};
        sinon.spy(state.api, 'focus');

        show(control, state);

        assert.isTrue(state.api.focus.calledOnce);
    });

    function verifyCall(url) {
        assert.isTrue(dollar.ajax.calledOnce);
        assert.equal(url, dollar.ajax.getCall(0).args[0].url);
    }
});
