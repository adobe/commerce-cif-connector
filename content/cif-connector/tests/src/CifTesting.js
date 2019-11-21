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

window.CifTesting = {
    Granite: {
        author: { ContentFrame: { location: '' } },
        $: function() {
            return {
                on: function() {},
                find: function(selector) {
                    let el = document.querySelector(selector);
                    return {
                        data: function() {
                            var data = {};
                            for (var i = 0, atts = el.attributes, n = atts.length; i < n; i++) {
                                let name = atts[i].nodeName;
                                if (name.startsWith('data-')) {
                                    data[name.substring('data-'.length)] = atts[i].value;
                                }
                            }

                            return data;
                        }
                    };
                }
            };
        }
    }
};
