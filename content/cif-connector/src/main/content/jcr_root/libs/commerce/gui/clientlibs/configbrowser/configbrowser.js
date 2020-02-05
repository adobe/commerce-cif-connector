 /******************************************************************************
  *
  *    Copyright 2020 Adobe. All rights reserved.
  *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
  *    you may not use this file except in compliance with the License. You may obtain a copy
  *    of the License at http://www.apache.org/licenses/LICENSE-2.0
  *
  *    Unless required by applicable law or agreed to in writing, software distributed under
  *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  *    OF ANY KIND, either express or implied. See the License for the specific language
  *    governing permissions and limitations under the License.
  *
  *****************************************************************************/

 (function(document, Granite, $) {
     "use strict";

     const ui = $(window).adaptTo("foundation-ui");

     function _validate() {
         const configurationTitle = $("[name='configTitle']").val();

         if (!configurationTitle || configurationTitle === "") {
             $("#create-configuration-button-confirm").prop("disabled", true);
         } else {
             $("#create-configuration-button-confirm").prop("disabled", false);
         }
     }

     $(document).off("foundation-validation-valid.create-config-form")
         .on("foundation-validation-valid.create-config-form", function(e) {
             _validate();
         });

     $(document).off("foundation-validation-invalid.create-config-form")
         .on("foundation-validation-invalid.create-config-form", function(e) {
             _validate();
         });

     const foundationRegistry = $(window).adaptTo("foundation-registry");
     foundationRegistry.register("foundation.collection.action.action", {
         name: "create.conf.action",
         handler: function (name, el, config, collection, selections) {
             // show the configuration creation dialog
             const createConfigDialog = $("#create-config-dialog");
             if (createConfigDialog.length > 0) {
                 const configParent = collection.activeItem
                     ? collection.activeItem.dataset.foundationCollectionItemId
                     : collection.dataset.foundationCollectionId;

                 createConfigDialog.find("[name='configParent']").val(configParent);

                 // clear the fields
                 createConfigDialog.find("[name='configTitle']").val("");
                 $("#create-configuration-button-confirm").prop("disabled", true);

                 createConfigDialog[0].show();
             }
         }
     });

     $(document).on("click.create-config", "#create-configuration-button-confirm", function(event) {
         const createConfigurationForm = $("#create-config-form");

         ui.wait();

         $.ajax({
             type: "POST",
             url: createConfigurationForm.attr("action"),
             data: createConfigurationForm.serialize()
         }).always(function() {
             ui.clearWait();
         }).done(function() {
             ui.notify(null, Granite.I18n.get("Successfully created a new configuration."), 'success');
             const createConfigDialog = $("#create-config-dialog");
             createConfigDialog[0].hide();

             const configParent = createConfigDialog.find("[name='configParent']").val();

             $("[data-foundation-collection-item-id='" + configParent + "']").attr("variant", "drilldown");

             const foundationCollectionAPI = $(".foundation-collection").adaptTo("foundation-collection");
             if (configParent === "/conf") {
                 window.location.reload();
             } else {
                 foundationCollectionAPI.load(configParent);
             }
         }).fail(function(error) {
             ui.notify(null, Granite.I18n.get("There was an error while creating a new configuration."), 'error');
         });
     });


 })(document, Granite, Granite.$);