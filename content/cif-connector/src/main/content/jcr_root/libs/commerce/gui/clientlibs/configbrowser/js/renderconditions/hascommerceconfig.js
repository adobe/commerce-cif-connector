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

(function ($) {

    const CREATE_CONFIG_SELECTOR = ".cif-create-config";
    const REL_CREATE_CONFIG_NONE = "none";
    const REL_CREATE_CONFIG = "cq-confadmin-actions-create-activator";

    $(document).on("foundation-contentloaded", (event) => {
        initCreateConfig()
    });
    $(document).on("foundation-selections-change", function (e) {
        const createConfigButton = document.querySelector(CREATE_CONFIG_SELECTOR);
        hide(createConfigButton);
        if (e.target.activeItem) {
            const activeItem = e.target.activeItem;
            checkCreateConfig(activeItem);
        }
    });

    function hide(element) {
        element.style.display = 'none'
    }

    function show(element) {
        element.style.display = 'block'
    }

    function checkCreateConfig(activeItem) {

        const activeItemMeta = activeItem.querySelector('.foundation-collection-quickactions');
        let relAction;
        if (activeItemMeta) {
            relAction = activeItemMeta.dataset['foundationCollectionQuickactionsRel'];
        }
        const createConfigButton = document.querySelector(CREATE_CONFIG_SELECTOR);
        hide(createConfigButton);
        if (relAction && createConfigButton && relAction === REL_CREATE_CONFIG_NONE)
            hide(createConfigButton);
        if (relAction && createConfigButton && relAction === REL_CREATE_CONFIG)
            show(createConfigButton);
    }


    function initCreateConfig() {
        //Default content path
        const activeItem = document.querySelector(".cq-confadmin-admin-childpages.foundation-collection");
        if (activeItem) {
            contentPath = activeItem.dataset["foundationCollectionId"];
            activeItem.addEventListener("coral-columnview:loaditems", function () {
                const activeColumnItem = document.querySelectorAll(".cq-confadmin-admin-childpages.foundation-collection .is-active");
                if (activeColumnItem.length > 0) {
                    activeColumnItem.forEach(function (objElement) {
                        if (contentPath === objElement.dataset['foundationCollectionId'] || contentPath === objElement.dataset['foundationCollectionId'] + "/")
                            checkCreateConfig(objElement);
                    });}
                else {
                    checkCreateConfig(activeItem);
                }
            });

        }

    }

})(Granite.$);