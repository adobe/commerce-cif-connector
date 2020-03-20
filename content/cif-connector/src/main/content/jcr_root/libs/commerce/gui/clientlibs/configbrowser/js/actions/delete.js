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
    const COMMAND_URL = Granite.HTTP.externalize('/bin/wcmcommand');

    const deleteConfig = path => {
        $.ajax({
            url: COMMAND_URL,
            type: 'POST',
            data: {
                _charset: 'UTF-8',
                cmd: 'deletePage',
                path,
                force: false,
                checkChildren: true
            },
            success: () => {
                location.reload();
            }
        });
    };

    const deleteHandler = (name, el, config, collectionm, selections) => {
        const itemPath = selections[0].dataset['foundationCollectionItemId'];

        if (!itemPath) {
            console.error('Cannot determine the item path, not doing anything');
            return;
        }

        const ui = $(window).adaptTo('foundation-ui');

        const messageEl = document.createElement('div');
        messageEl.innerHTML = `${Granite.I18n.get(
            'Are you sure you want to delete this configuration: '
        )}<br> ${itemPath}`;
        ui.prompt(Granite.I18n.get('Delete'), messageEl.innerHTML, 'notice', [
            {
                text: Granite.I18n.get('OK'),
                handler: () => {
                    deleteConfig(itemPath);
                },
                warning: 'true'
            },
            {
                text: Granite.I18n.get('Cancel')
            }
        ]);
    };

    $(window)
        .adaptTo('foundation-registry')
        .register('foundation.collection.action.action', {
            name: 'cq.wcm.commerce.configuration.delete',
            handler: deleteHandler
        });
})(Granite.$);
