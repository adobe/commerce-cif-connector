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

describe('CifPicker', () => {
    var relActivator = 'relActivator';
    var pickerSrc = 'pickersrc';
    var dollar = Granite.$;
    var control;
    var getState;

    before(() => {
        var promise = {
            then: f => {
                f('elem');
                return promise;
            }
        };
        dollar.ajax = function(asd) {
            return promise;
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

    it('does nothing if pickersrc is empty', () => {
        var cifPicker = window.CIF.CifPicker.cifPicker;

        cifPicker(control, relActivator, null, null);

        assert.isFalse(dollar.ajax.calledOnce);
    });

    it('shows the picker', () => {
        var cifPicker = window.CIF.CifPicker.cifPicker;

        cifPicker(control, relActivator, pickerSrc, null);

        assert.isTrue(dollar.ajax.calledOnce);
        assert.equal(pickerSrc, dollar.ajax.getCall(0).args[0].url);
    });

    it("does't show the picker again if it's already showing", () => {
        var cifPicker = window.CIF.CifPicker.cifPicker;

        cifPicker(control, relActivator, pickerSrc, null);

        assert.isTrue(dollar.ajax.calledOnce);
        assert.equal(pickerSrc, dollar.ajax.getCall(0).args[0].url);

        cifPicker(control, relActivator, pickerSrc, null);
        assert.isFalse(dollar.ajax.calledTwice);
    });

    it('shows the picker which was used and closed earlier', () => {
        var cifPicker = window.CIF.CifPicker.cifPicker;

        cifPicker(control, relActivator, pickerSrc, null);

        assert.isTrue(dollar.ajax.calledOnce);
        assert.equal(pickerSrc, dollar.ajax.getCall(0).args[0].url);

        var close = window.CIF.CifPicker.close;
        var state = getState(control, relActivator);
        state.api = {
            attach: function() {},
            detach: function() {},
            pick: function() {
                return { then: function() {} };
            },
            focus: function() {}
        };
        state.el = { any: 'thing' };
        state.loading = false;

        var showSpy = sinon.spy(window.CIF.CifPicker.show);

        close(control, state);

        cifPicker(control, relActivator, pickerSrc, null);

        showSpy.calledOnce;

        cifPicker(control, relActivator, pickerSrc, null);

        showSpy.calledOnce;
    });

    it('transfers focus to the picker after showing', () => {
        var show = window.CIF.CifPicker.show;
        var state = getState(control);
        state.api = {
            attach: function() {},
            pick: function() {
                return { then: function() {} };
            }
        };

        state.el = { focus: function() {} };
        sinon.spy(state.el, 'focus');

        show(control, state);
        assert.isTrue(state.el.focus.calledOnce);

        assert.isFalse(state.loading);
        assert.isTrue(state.open);

        state.el.focus.restore();

        state.api.focus = function() {};
        sinon.spy(state.api, 'focus');

        show(control, state);

        assert.isTrue(state.api.focus.calledOnce);
        state.api.focus.restore();
    });

    it('close() puts the picker in closed state and invokes callback if available', () => {
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

    it('show picker and call selection handler', () => {
        var show = window.CIF.CifPicker.show;
        var state = getState(control);
        state.api = {
            attach: function() {},
            detach: function() {},
            pick: function() {
                return {
                    then: function(resolve, reject) {
                        resolve();
                    }
                };
            }
        };

        state.el = { focus: function() {} };

        var selectionHandler = function() {};
        var selectionHandlerSpy = sinon.spy(selectionHandler);

        show(control, state, selectionHandlerSpy);
        assert.isTrue(selectionHandlerSpy.calledOnce);
    });

    function verifyCall(url) {
        assert.isTrue(dollar.ajax.calledOnce);
        assert.equal(url, dollar.ajax.getCall(0).args[0].url);
    }
});
