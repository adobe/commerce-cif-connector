/*
    Copyright 2020 Adobe. All rights reserved.
    This file is licensed to you under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License. You may obtain a copy
    of the License at http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software distributed under
    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
    OF ANY KIND, either express or implied. See the License for the specific language
    governing permissions and limitations under the License.
 */

(function($) {
    const CREATE_PULLDOWN_SELECTOR = '.cif-create-pulldown';
    const CREATE_CONFIG_SELECTOR = '.cif-create-config';
    const CREATE_FOLDER_SELECTOR = '.cif-create-folder';
    const CREATE_PULLDOWN_REL = 'cq-confadmin-actions-create-pulldown-activator';
    const CREATE_CONFIG_REL = 'cq-confadmin-actions-create-config-activator';
    const CREATE_FOLDER_REL = 'cq-confadmin-actions-create-folder-activator';

    $(document).on('foundation-contentloaded', event => {
        init();
    });
    $(document).on('foundation-selections-change', function(e) {
        const createPulldown = document.querySelector(CREATE_PULLDOWN_SELECTOR);
        hide(createPulldown);
        if (e.target.activeItem) {
            const activeItem = e.target.activeItem;
            applyRenderConditions(activeItem);
        }
    });

    function hide(element) {
        if (element) {
            element.style.display = 'none';
        }
    }

    function show(element) {
        if (element) {
            element.style.display = 'block';
        }
    }

    function applyRenderConditions(activeItem) {
        const activeItemMeta = activeItem.querySelector('.foundation-collection-quickactions');
        let actionRels;
        if (activeItemMeta) {
            actionRels = activeItemMeta.dataset['foundationCollectionQuickactionsRel'];
        }

        const createPullDown = document.querySelector(CREATE_PULLDOWN_SELECTOR);
        hide(createPullDown);
        if (actionRels && createPullDown && actionRels.indexOf(CREATE_PULLDOWN_REL) >= 0) {
            show(createPullDown);
        }

        const createFolderButton = document.querySelector(CREATE_FOLDER_SELECTOR);
        hide(createFolderButton);
        if (actionRels && createFolderButton && actionRels.indexOf(CREATE_FOLDER_REL) >= 0) {
            show(createFolderButton);
        }

        const createConfigButton = document.querySelector(CREATE_CONFIG_SELECTOR);
        hide(createConfigButton);
        if (actionRels && createConfigButton && actionRels.indexOf(CREATE_CONFIG_REL) >= 0) {
            show(createConfigButton);
        }
    }

    function init() {
        //Default content path
        const activeItem = document.querySelector('.cq-confadmin-admin-childpages.foundation-collection');
        if (activeItem) {
            contentPath = activeItem.dataset['foundationCollectionId'];
            activeItem.addEventListener('coral-columnview:loaditems', function() {
                const activeColumnItem = document.querySelectorAll(
                    '.cq-confadmin-admin-childpages.foundation-collection .is-active'
                );
                if (activeColumnItem.length > 0) {
                    activeColumnItem.forEach(function(objElement) {
                        if (
                            contentPath === objElement.dataset['foundationCollectionId'] ||
                            contentPath === objElement.dataset['foundationCollectionId'] + '/'
                        )
                            applyRenderConditions(objElement);
                    });
                } else {
                    applyRenderConditions(activeItem);
                }
            });
        }
    }
})(Granite.$);
