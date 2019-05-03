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

package com.adobe.cq.commerce.virtual.catalog.data.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import com.adobe.cq.commerce.virtual.catalog.data.CatalogIdentifier;
import com.adobe.cq.commerce.virtual.catalog.data.CatalogIdentifierService;

/**
 * Collects all cloud commerce providers and builds the list of available catalog identifiers (project in UI) for each
 * of them.
 */
@Component(service = CatalogIdentifierService.class)
public class CatalogIdentifierServiceImpl implements CatalogIdentifierService {

    private Map<String, Collection<String>> map = new ConcurrentHashMap<>();

    @Reference(
            service = CatalogIdentifier.class,
            bind = "bindCatalogIdentifier",
            unbind = "unbindCatalogIdentifier",
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC)
    protected void bindCatalogIdentifier(CatalogIdentifier identifier, Map<?, ?> properties) {
        map.put(identifier.getCommerceProviderName(), identifier.getAllCatalogIdentifiers());
    }

    protected void unbindCatalogIdentifier(CatalogIdentifier identifier, Map<?, ?> properties) {
        map.remove(identifier.getCommerceProviderName());
    }


    @Override
    public Map<String, Collection<String>> getCatalogIdentifiersForAllCommerceProviders() {
        return map;
    }
}
