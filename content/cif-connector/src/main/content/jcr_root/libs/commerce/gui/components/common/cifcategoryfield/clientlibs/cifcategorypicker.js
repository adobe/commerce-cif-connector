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
window.CIF.CifCategoryPicker = {};

(function(window, document, Granite) {
    'use strict';
    /**
     * The context component can specify the following properties for the picker:
     * pickersrc, root, filter, selectioncount, selectionid.
     * If pickersrc is specified then the root, filter, selectioncount and selectionid is ignored, they should be provided in
     * the pickersrc.
     */

    /**
     * Selector for CIF category picker context component
     * @type {string}
     */
    var relActivator = '.cq-commerce-cifcategorypicker-activator';

    /**
     * Event type for picker selection.
     * @type {string}
     */
    var eventType = 'cifCategoryPickerSelection';

    /**
     * Default picker URL.
     * @type {string}
     */
    var defaultPickerSrc = '/mnt/overlay/commerce/gui/content/common/cifcategoryfield/picker.html';
    /**
     * Default root path shown by the picker when opened.
     * @type {string}
     */
    var defaultRootPath = '/var/commerce/products';
    var defaultSelectionCount = 'single';
    var defaultFilter = 'folderOrCategory';
    var defaultSelectionId = 'id';
    var last = false;

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

        window.CIF.CifPicker.cifPicker(control, relActivator, pickerSrc, handleSelections);
    };

    Granite.$(document).on('click', relActivator, clickActivator);

    window.CIF.CifCategoryPicker = { clickActivator, relActivator };
})(window, document, Granite);
