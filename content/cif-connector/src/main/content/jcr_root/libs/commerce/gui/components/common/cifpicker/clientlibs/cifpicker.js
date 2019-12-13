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

window.CIF = window.CIF || {};
window.CIF.CifPicker = {};

(function(window, document, Granite) {
    'use strict';

    var last = false;
    var show = function(control, state, handleSelections) {
        state.api.attach(this);
        state.api.pick(control[0], []).then(
            function(selections) {
                handleSelections(selections);
                close(control, state);
            },
            function() {
                close(control, state, handleSelections);
            }
        );

        if ('focus' in state.api) {
            state.api.focus(last);
        } else {
            state.el.focus();
        }

        state.loading = false;
        state.open = true;
    };

    var close = function(control, state, callback) {
        state.api.detach();
        state.open = false;
        if (callback) {
            callback();
        }
    };

    var getState = function(control, relActivator) {
        var KEY_STATE = relActivator + '.internal.state';
        var state = control.data(KEY_STATE);
        if (!state) {
            state = {
                el: null,
                open: false,
                loading: false
            };
            control.data(KEY_STATE, state);
        }

        return state;
    };

    var cifPicker = function(control, relActivator, pickerSrc, handleSelections) {
        var state = getState(control, relActivator);
        if (state.loading) {
            return;
        }

        if (state.el) {
            if (!state.open) {
                show(control, state, handleSelections);
            }
        } else {
            if (!pickerSrc) {
                return;
            }

            state.loading = true;
            Granite.$.ajax({ url: pickerSrc, cache: false })
                .then(function(html) {
                    return Granite.$(window)
                        .adaptTo('foundation-util-htmlparser')
                        .parse(html);
                })
                .then(function(fragment) {
                    return Granite.$(fragment).children()[0];
                })
                .then(
                    function(pickerEl) {
                        state.el = pickerEl;
                        state.api = Granite.$(pickerEl).adaptTo('foundation-picker');

                        show(control, state, handleSelections);
                    },
                    function() {
                        state.loading = false;
                    }
                );
        }
    };

    window.CIF.CifPicker = { cifPicker: cifPicker, getState: getState, show: show, close: close };
})(window, document, Granite);
