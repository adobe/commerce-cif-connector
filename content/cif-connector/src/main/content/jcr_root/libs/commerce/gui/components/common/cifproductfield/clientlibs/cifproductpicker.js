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
window.CIF.CifProductPicker = {};

(function(window, document, Granite) {
    'use strict';
    /**
     * The context component can specify the following properties for the picker:
     * pickersrc, root, filter, selectioncount, selectionid.
     * If pickersrc is specified then the root, filter, selectioncount and selectionid is ignored, they should be provided in
     * the pickersrc.
     */

    /**
     * Selector for CIF product picker context component
     * @type {string}
     */
    var relActivator = '.cq-commerce-cifproductpicker-activator';

    /**
     * Event type for picker selection.
     * @type {string}
     */
    var eventType = 'cifProductPickerSelection';

    /**
     * Default picker URL.
     * @type {string}
     */
    var defaultPickerSrc = '/mnt/overlay/commerce/gui/content/common/cifproductfield/picker.html';
    /**
     * Default root path shown by the picker when opened.
     * @type {string}
     */
    var defaultRootPath = '/var/commerce/products';
    var defaultSelectionCount = 'single';
    var defaultFilter = 'folderOrProduct';
    var defaultSelectionId = 'id';
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

    var getState = function(control) {
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

    var cifProductPicker = function(control, pickerSrc, handleSelections) {
        var state = getState(control);
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
            Granite.$.ajax({
                url: pickerSrc,
                cache: false
            })
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

    var clickActivator = function(e) {
        e.preventDefault();

        var control = Granite.$(document).find(relActivator);
        var pickerConfig = control.data();
        var pickerSrc = pickerConfig.pickersrc;
        if (!pickerSrc) {
            var root = pickerConfig.root;
            if (!root) {
                root = defaultRootPath;
            }
            var filter = pickerConfig.filter;
            if (!filter) {
                filter = defaultFilter;
            }
            var selectionCount = pickerConfig.selectioncount;
            if (!selectionCount) {
                selectionCount = defaultSelectionCount;
            }
            var selectionId = pickerConfig.selectionid;
            if (!selectionId) {
                selectionId = defaultSelectionId;
            }

            pickerSrc =
                defaultPickerSrc +
                '?root=' +
                root +
                '&filter=' +
                filter +
                '&selectionCount=' +
                selectionCount +
                '&selectionId=' +
                selectionId;
        }

        var handleSelections = function(selections) {
            control.trigger(eventType, { selections: selections });
        };

        cifProductPicker(control, pickerSrc, handleSelections);
    };

    Granite.$(document).on('click', relActivator, clickActivator);

    window.CIF.CifProductPicker = { clickActivator, getState, relActivator, cifProductPicker, close, show };
})(window, document, Granite);
