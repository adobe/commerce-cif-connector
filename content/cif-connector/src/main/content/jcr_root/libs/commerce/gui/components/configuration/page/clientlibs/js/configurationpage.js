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
     * Coral Select component used for selecting the graphQL client.
     */
    let graphQlClient;

    /**
     * Textfield component used for providing a Store View identifier.
     */
    let language;

    /**
     * Textfield component used for providing a GraphQL Proxy Path.
     */
    let graphQlProxyPath;

    /**
     * Textfield component used for providing a Catalog Root Category Idr.
     */
    let magentoRootCategoryId;

    /**
     * Textfield component used for providing a Language.
     */
    let storeView;

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
     * The properties map JCR key -> HTML element.
     */
    let propertiesMap;

    /**
     * The properties map of inherited resource.
     */
    let inheritedValues;

    /**
     * Inheritance handling state. 0 - not started, 1 - in progress, 2 - Done.
     * @type {number}
     */
    let inheritanceHandled = 0;

    /**
     * Collects references to DOM elements and Coral components. Meant to be called after foundation-contentloaded
     * event is triggered.
     */
    function init() {
        formElement = $('#cq-commerce-products-bindproducttree-form');
        catalogIdentifierCoralSelectComponent = $('#cq-commerce-products-bindproducttree-catalog-select').get(0);
        catalogIdentifierDataCoralSelectComponent = $('#cq-commerce-products-bindproducttree-catalog-select-data').get(0);
        commerceProviderCoralSelectComponent = $('#cq-commerce-products-bindproducttree-provider-select').get(0);
        graphQlClient = $("#cq-commerce-graphql-client").get(0);
        storeView = $('input[name="./magentoStore"]').get(0);
        graphQlProxyPath = $('input[name="./magentoGraphqlEndpoint"]').get(0);
        magentoRootCategoryId = $('input[name="./magentoRootCategoryId"]').get(0);
        language = $('coral-select[name="./jcr:language"]').get(0);
        nameCoralTextfieldComponent = $('#cq-commerce-products-bindproducttree-name').get(0);
        redirectLocation = $('#cq-commerce-products-bindproducttree-form')
            .find('[data-foundation-wizard-control-action=cancel]')
            .attr('href');
        catalogIdentifierHidden = $('#cq-commerce-products-bindproducttree-catalog-hidden').get(0);

        propertiesMap = {
            "cq:catalogDataResourceProviderFactory": {
                element: commerceProviderCoralSelectComponent,
                locked: false
            },
            "cq:catalogIdentifier": {
                element: catalogIdentifierCoralSelectComponent,
                locked: false
            },
            "cq:graphqlClient": {
                element: graphQlClient,
                locked: false
            },
            "magentoStore": {
                element: storeView,
                locked: false
            },
            "magentoGraphqlEndpoint": {
                element: graphQlProxyPath,
                locked: false
            },
            "magentoRootCategoryId": {
                element: magentoRootCategoryId,
                locked: false
            },
            "jcr:language": {
                element: language,
                locked: false
            }
        }
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
     * @param {string} selected value of Coral Select.
     * @returns {Object[]} The list of Coral Select items.
     */
    function buildSelectItems(values, selected) {
        return values.map(catalog => {
            return {
                value: catalog,
                content: {
                    textContent: catalog
                },
                selected: (catalog === selected)
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
     * Returns the lock status of a field by it's name
     * @param name
     * @returns {boolean|locked}
     */
    function getLockStatus(name) {
        return propertiesMap[name].locked;
    }

    /**
     * Populates the commerce identifier select component after the commerce provider is selected.
     */
    function commerceProviderSelectedHandler() {
        const catalogIdentifierLocked = getLockStatus("cq:catalogIdentifier");
        const commerceProvider = getSelectedCommerceProvider();

        if (!catalogIdentifierLocked && commerceProvider !== '') {
            updateCatalogIdentifier();
        }

        if (inheritanceHandled === 0) {
            catalogIdentifierCoralSelectComponent.disabled = (commerceProvider === '');
        }
    }

    /**
     * Handles change events for Catalog Identifier field
     */
    function catalogIdentifierSelectedHandler() {
        const catalogIdentifier = catalogIdentifierCoralSelectComponent.selectedItem.value;
        catalogIdentifierHidden.value = catalogIdentifier;
    }

    /**
     * Updates the options and selected value of the Catalog Identifier Select
     * If a parameter is passed and matches any of the fields being added to the select options, that option will be selected
     * @param selected
     */
    function updateCatalogIdentifier(selected) {
        const commerceProvider = getSelectedCommerceProvider();
        const catalogs = getCatalogIdentifiers(commerceProvider);
        catalogIdentifierCoralSelectComponent.items.clear();
        const items = buildSelectItems(catalogs, selected);
        items.forEach(item => {
            catalogIdentifierCoralSelectComponent.items.add(item);
        });
    }

    /**
     * Function for displaying the inheritance break/revert dialog for confirmation
     * @param linkElement
     */
    function showInheritanceDialog(linkElement) {
        const dialog = document.getElementById('inheritance-dialog');
        dialog.header.innerHTML = linkElement.title;
        dialog.content.innerHTML = '<p>Do you really want to '+linkElement.title.split(" ")[0].toLowerCase()+' the inheritance?</p>';

        const confirmButton = dialog.footer.querySelector("#inheritance-change");
        confirmButton.dataset.targetProperty = linkElement.dataset.togglePropertyInheritance;
        confirmButton.dataset.locked = linkElement.dataset.inheritanceLocked;

        dialog.show();
    }

    /**
     * Function used to generate a link icon for toggling the inheritance
     * inheritance status is passed via `data` param
     * @param data
     * @returns {*|jQuery}
     */
    function generateInheritanceControl(data) {
        const iconSuffix = data.locked ? '' : 'Off';
        return $(
            '<a class="cif-toggle-inheritance" ' +
                    'data-toggle-property-inheritance="' + data.property + '" ' +
                    'data-inheritance-locked="' + data.locked + '" ' +
                    'title="' + (data.locked ? 'Cancel inheritance' : 'Revert inheritance') + '" href="#">' +
                '<coral-icon class="coral3-Icon coral3-Icon--link' + iconSuffix + ' coral3-Icon--sizeS" ' +
                        'icon="link' + iconSuffix + '" size="S" role="img" aria-label="link ' + iconSuffix.toLowerCase() + '">' +
                '</coral-icon>' +
            '</a>').get(0);
    }

    /**
     * Sets the lock status and updates value for a given fields's inheritance toggle link
     * @param linkElement
     * @param status
     */
    function setLockStatus(linkElement, status) {
        const propertyName = linkElement.dataset.togglePropertyInheritance;
        document.querySelector('input[name="./' + propertyName + '@Delete"]').disabled = !status;
        propertiesMap[propertyName].element.disabled = status;
        propertiesMap[propertyName].locked = status;

        if (propertyName === 'cq:catalogIdentifier') {
            catalogIdentifierHidden.disabled = status;
        }

        linkElement.dataset.inheritanceLocked = status.toString();
        linkElement.title = (!status ? 'Cancel inheritance' : 'Revert inheritance');

        const iconElement = linkElement.querySelector('coral-icon');

        iconElement.classList.remove((status ? "coral3-Icon--linkOff" : "coral3-Icon--link"));
        iconElement.classList.add((status ? "coral3-Icon--link" : "coral3-Icon--linkOff"));
        iconElement.setAttribute("aria-label", (status ? "link" : "link off"));
        iconElement.setAttribute("icon", (status ? "link" : "linkOff"));

        if (inheritanceHandled === 2 && status) {
            if (propertyName === 'cq:catalogIdentifier') {
                updateCatalogIdentifier(inheritedValues[propertyName].value);
            } else {
                propertiesMap[propertyName].element.value = inheritedValues[propertyName].value;
            }

            propertiesMap[propertyName].element.trigger('change');
        }
    }

    /**
     * Sets the initial field value based on the inheritance status
     * @param key
     */
    function setInitialValue(key) {
        const fieldData = propertiesMap[key];
        if (key === 'cq:catalogIdentifier') {
            const value = fieldData.locked ? inheritedValues[key].value : catalogIdentifierHidden.value;
            updateCatalogIdentifier(value);
            catalogIdentifierCoralSelectComponent.trigger('change');
        } else {
            fieldData.element.value = fieldData.locked ? inheritedValues[key].value : fieldData.element.value;
        }
    }

    /**
     * Handles the response from the inheritance status servlet
     * Generates inheritance toggle links, sets initial field values, sets the lock status and adds event handlers
     * @param data
     */
    function handleInheritanceReceieved(data) {
        if (inheritanceHandled === 0 && data.inherited) {
            inheritanceHandled = 1;
            inheritedValues = data.inheritedProperties;
            Object.keys(propertiesMap).forEach(function(key) {
                const elem = propertiesMap[key].element;
                elem.classList.add("cif-lockable-field");
                propertiesMap[key].locked = data.overriddenProperties.indexOf(key) === -1;
                setInitialValue(key);
                const lockControl = generateInheritanceControl({
                    locked: propertiesMap[key].locked,
                    property: key
                });
                lockControl.addEventListener('click', function (event) {
                    showInheritanceDialog(event.currentTarget);
                });

                elem.parentElement.appendChild(lockControl);
                setLockStatus(lockControl, propertiesMap[key].locked);
            });

            const dialog = new Coral.Dialog().set({
                id: "inheritance-dialog",
                header: {
                    innerHTML: ""
                },
                content: {
                    innerHTML: ""
                },
                footer: {
                    innerHTML: "<button is=\"coral-button\" coral-close=\"\" class=\"coral3-Button coral3-Button--secondary\" size=\"M\" variant=\"secondary\"><coral-button-label>No</coral-button-label></button>"
                },
                variant: "warning"
            });

            const actionButton = new Coral.Button().set({
                id: "inheritance-change",
                variant: "primary",
                label: {
                    innerHTML: '<coral-button-label>Yes</coral-button-label>'
                }
            })

            actionButton.on('click', function () {
                const linkElement = document.querySelector('a[data-toggle-property-inheritance="'+this.dataset.targetProperty+'"]');
                setLockStatus(linkElement, (this.dataset.locked !== 'true'));
                dialog.hide();
            })

            dialog.footer.appendChild(actionButton);
            document.body.appendChild(dialog);

            inheritanceHandled = 2;
        }
    }

    /**
     * Builds the URL and performs an AJAX call to the inheritance status servlet
     */
    function fetchInheritance() {
        const urlParams = new URLSearchParams(window.location.search);
        var resourcePath = urlParams.has("item") ?
            decodeURIComponent(urlParams.get("item")) : document.querySelector('input[name="parentPath"]').value;
        resourcePath += '.cifconfig.json';

        $.get(resourcePath).done(handleInheritanceReceieved)
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

                        updateCatalogIdentifier(catalogIdentifier);
                    }
                }
                commerceProviderCoralSelectComponent.on('change', commerceProviderSelectedHandler);
                catalogIdentifierCoralSelectComponent.on('change', catalogIdentifierSelectedHandler);

                Object.keys(propertiesMap).forEach(function (key) {
                    document.querySelector('input[name="./' + key + '@Delete"]').disabled = true;
                })

                fetchInheritance();
            });
        }

        formElement.on('submit', formSubmitHandler);
    });
})(window, document, Granite, Granite.$);
