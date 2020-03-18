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

(function(window, document, Granite, $) {
    'use strict';

    /**
     * Coral Select component used for selecting the catalog identifier (project in UI).
     */
    let catalogIdentifierCoralSelectComponent;

    /**
     * The field used to store the actual value of the catalog identifier property
     */
    let catalogIdentifierHidden;

    /**
     * The hidden Coral Select component used for getting all available catalog identifiers for all commerce providers.
     */
    let catalogIdentifierDataCoralSelectComponent;

    /**
     * Coral Select component used for selecting the commerce provider.
     */
    let commerceProviderCoralSelectComponent;

    /**
     * Coral Textfield component user for providing a name for the new node.
     */
    let nameCoralTextfieldComponent;

    /**
     * DOM element of the form. Used to listen for events.
     */
    let formElement;

    /**
     * The location where the user will be redirected after the product tree is bound.
     */
    let redirectLocation;

    /**
     * Collects references to DOM elements and Coral components. Meant to be called after foundation-contentloaded
     * event is triggered.
     */
    function init() {
        formElement = $('#cq-commerce-products-bindproducttree-form');
        catalogIdentifierCoralSelectComponent = $('#cq-commerce-products-bindproducttree-catalog-select').get(0);
        catalogIdentifierDataCoralSelectComponent = $('#cq-commerce-products-bindproducttree-catalog-select-data').get(
            0
        );
        commerceProviderCoralSelectComponent = $('#cq-commerce-products-bindproducttree-provider-select').get(0);
        nameCoralTextfieldComponent = $('#cq-commerce-products-bindproducttree-name').get(0);
        redirectLocation = $('#cq-commerce-products-bindproducttree-form')
            .find('[data-foundation-wizard-control-action=cancel]')
            .attr('href');
        catalogIdentifierHidden = $('#cq-commerce-products-bindproducttree-catalog-hidden').get(0);
    }

    /**
     * Inspects the DOM to get the selected commerce provider from the Coral Select component.
     *
     * @returns {string} The name of the selected commerce provider.
     */
    function getSelectedCommerceProvider() {
        const selectedElement = commerceProviderCoralSelectComponent.selectedItem;
        return selectedElement ? selectedElement.value : '';
    }

    /**
     * Gets a list of available catalog identifiers for a specific commerce provider.
     *
     * @param {string} commerceProvider The commerce provider name.
     * @returns {string[]} A list of catalog identifiers.
     */
    function getCatalogIdentifiers(commerceProvider) {
        return catalogIdentifierDataCoralSelectComponent.items
            .getAll()
            .filter(item => item.value.startsWith(`${commerceProvider}:`))
            .map(item => item.value.substr(commerceProvider.length + 1));
    }

    /**
     * Given a list of available values, builds a list of Coral Select items.
     *
     * @param {string[]} values The list of values which will be converted to a list of Coral Select items.
     * @returns {Object[]} The list of Coral Select items.
     */
    function buildSelectItems(values) {
        return values.map(catalog => {
            return {
                value: catalog,
                content: {
                    textContent: catalog
                }
            };
        });
    }

    /**
     * Submits the form data and redirects the user back to products page after the form is submitted.
     */
    function formSubmitHandler() {
        // If we let the form mechanism to submit data and we redirect here directly, the redirect happens too fast
        // and sometimes the newly created item is not visible in the console. Submitting the form using ajax allows
        // us to know when the server finished processing the request and redirect after that happens. This way the
        // newly created item is always visible in the console after redirect.
        $.ajax({
            url: formElement.attr('action'),
            type: 'POST',
            data: formElement.serialize(),
            success: () => {
                document.location.href = Granite.HTTP.externalize(redirectLocation);
            },
            error: () => {
                nameCoralTextfieldComponent.invalid = true;
            }
        });
        return false; // prevent the form from submitting because we do that from JS.
    }

    /**
     * Populates the commerce identifier select component after the commerce provider is selected.
     */
    function commerceProviderSelectedHandler() {
        console.log(`Commerce provider field changed`);
        const commerceProvider = getSelectedCommerceProvider();
        if (commerceProvider !== '') {
            const catalogs = getCatalogIdentifiers(commerceProvider);

            catalogIdentifierCoralSelectComponent.items.clear();
            const items = buildSelectItems(catalogs);
            items.forEach(item => {
                catalogIdentifierCoralSelectComponent.items.add(item);
            });
            catalogIdentifierCoralSelectComponent.disabled = false;
        } else {
            catalogIdentifierCoralSelectComponent.disabled = true;
        }
    }

    function catalogIdentifierSelectedHandler() {
        const catalogIdentifier = catalogIdentifierCoralSelectComponent.selectedItem.value;
        catalogIdentifierHidden.value = catalogIdentifier;
        console.log(`Setting the hidden field value to ${catalogIdentifier}`);
    }

    $(document).on('foundation-contentloaded', () => {
        init();

        if (commerceProviderCoralSelectComponent) {
            // Register events listeners
            Coral.commons.ready(commerceProviderCoralSelectComponent, function() {
                if (!catalogIdentifierCoralSelectComponent.selectedItem) {
                    let commerceProvider = commerceProviderCoralSelectComponent.selectedItem
                        ? commerceProviderCoralSelectComponent.selectedItem.value
                        : '';
                    if (commerceProvider && commerceProvider.length > 0) {
                        const catalogIdentifiers = getCatalogIdentifiers(commerceProvider);
                        const catalogIdentifier = catalogIdentifiers.find(
                            item => item === catalogIdentifierHidden.value
                        );
                        catalogIdentifierCoralSelectComponent.items.clear();
                        catalogIdentifierCoralSelectComponent.items.add({
                            value: catalogIdentifier,
                            content: {
                                textContent: catalogIdentifier
                            },
                            selected: true
                        });
                    }
                }
                commerceProviderCoralSelectComponent.on('change', commerceProviderSelectedHandler);
                catalogIdentifierCoralSelectComponent.on('change', catalogIdentifierSelectedHandler);
            });
        }
        formElement.on('submit', formSubmitHandler);
    });
})(window, document, Granite, Granite.$);
