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

describe('CifPickerTest', () => {
    var relActivator = 'relActivator';
    var pickerSrc = 'pickersrc';
    var event = { preventDefault: function() {} };
    var dollar = Granite.$;
    var button;
    var control;
    var getState;

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
            return new Promise(function(resolve, reject) {
                return resolve(asd);
            });
        };
        getState = window.CIF.CifPicker.getState;
    });

    beforeEach(() => {
        sinon.spy(dollar, 'ajax');
        control = {
            data: function(key, value) {
                if (!key && !value) {
                    var data = {};
                    for (var i = 0, atts = el.attributes, n = atts.length; i < n; i++) {
                        let name = atts[i].nodeName;
                        if (name.startsWith('data-')) {
                            data[name.substring('data-'.length)] = atts[i].value;
                        }
                    }

                    return data;
                } else if (key && !value) {
                    return control[key];
                } else if (key && value) {
                    return (control[key] = value);
                }
            }
        };
    });

    afterEach(() => {
        dollar.ajax.restore();
    });

    it('test cifPicker()', () => {
        var cifPicker = window.CIF.CifPicker.cifPicker;

        var state = getState(control, relActivator);
        state.test = 'test';

        cifPicker(control, relActivator, pickerSrc, null);

        verifyCall(pickerSrc);

        dollar.ajax.restore();
        sinon.spy(dollar, 'ajax');
        state.loading = true;

        cifPicker(control, relActivator, pickerSrc, null);

        assert.isFalse(dollar.ajax.calledOnce);

        state.loading = false;

        cifPicker(control, relActivator, null, null);

        assert.isFalse(dollar.ajax.calledOnce);

        state.el = control;
        state.open = true;

        cifPicker(control, relActivator, pickerSrc, null);

        assert.isFalse(dollar.ajax.calledOnce);

        state.open = false;
        state.api = {
            attach: function() {},
            pick: function() {
                return { then: function() {} };
            },
            focus: function() {}
        };

        cifPicker(control, relActivator, pickerSrc, null);

        assert.isFalse(dollar.ajax.calledOnce);
    });

    it('test getState()', () => {
        var state = getState(control, relActivator);

        assert.isNotNull(state);
        assert.isNull(state.el);
        assert.isFalse(state.open);
        assert.isFalse(state.loading);

        state.loading = true;

        var state = getState(control, relActivator);
        assert.isTrue(state.loading);
    });

    it('test show()', () => {
        var show = window.CIF.CifPicker.show;
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

    it('test close()', () => {
        var close = window.CIF.CifPicker.close;
        var state = getState(control, relActivator);
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

    function verifyCall(url) {
        assert.isTrue(dollar.ajax.calledOnce);
        assert.equal(url, dollar.ajax.getCall(0).args[0].url);
    }
});
