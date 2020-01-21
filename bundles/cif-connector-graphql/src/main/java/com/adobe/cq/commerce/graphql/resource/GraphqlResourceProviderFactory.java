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

package com.adobe.cq.commerce.graphql.resource;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.graphql.magento.GraphqlDataService;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceConfiguration;
import com.adobe.cq.commerce.virtual.catalog.data.CatalogDataResourceProviderFactory;
import com.adobe.cq.commerce.virtual.catalog.data.CatalogIdentifier;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import static com.adobe.cq.commerce.graphql.resource.Constants.MAGENTO_GRAPHQL_PROVIDER;

/**
 * Factory class for CIF integration data resource provider.
 */
@Component(
    service = { CatalogDataResourceProviderFactory.class, CatalogIdentifier.class },
    immediate = true,
    property = {
        CatalogDataResourceProviderFactory.PROPERTY_FACTORY_SERVICE_ID + "=" + MAGENTO_GRAPHQL_PROVIDER
    })
public class GraphqlResourceProviderFactory<T> implements CatalogDataResourceProviderFactory<T>, CatalogIdentifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlResourceProviderFactory.class);

    protected Map<String, GraphqlDataService> clients = new ConcurrentHashMap<>();

    @Reference
    ConfigurationResourceResolver configurationResourceResolver;

    @Reference(
        service = GraphqlDataService.class,
        bind = "bindGraphqlDataService",
        unbind = "unbindGraphqlDataService",
        cardinality = ReferenceCardinality.AT_LEAST_ONE,
        policy = ReferencePolicy.DYNAMIC)
    protected void bindGraphqlDataService(GraphqlDataService client, Map<?, ?> properties) {
        String identifier = client.getIdentifier();
        LOGGER.info("Registering GraphqlDataService '{}'", identifier);
        clients.put(identifier, client);
    }

    protected void unbindGraphqlDataService(GraphqlDataService client, Map<?, ?> properties) {
        String identifier = client.getIdentifier();
        LOGGER.info("De-registering GraphqlDataService '{}'", identifier);
        clients.remove(identifier);
    }

    @Reference
    protected Scheduler scheduler;

    @Override
    public ResourceProvider<T> createResourceProvider(Resource root) {
        LOGGER.debug("Creating resource provider for resource at path {}", root.getPath());
        // Get cq:catalogIdentifier property from ancestor pages
        Page page = root.getResourceResolver().adaptTo(PageManager.class).getContainingPage(root);

        Resource config = configurationResourceResolver.getResource(root, "settings", "commerce/default");
        ValueMap properties = config.getValueMap();
        String catalogIdentifier = properties.get(GraphqlDataServiceConfiguration.CQ_CATALOG_IDENTIFIER, "");
        if (StringUtils.isEmpty(catalogIdentifier)) {
            LOGGER.warn("Could not find cq:catalogIdentifier property for given resource at " + root.getPath());
            return null;
        }

        // Check Magento root category id
        String rootCategoryId = properties.get(Constants.MAGENTO_ROOT_CATEGORY_ID_PROPERTY, "");
        try {
            Integer.valueOf(rootCategoryId);
        } catch (NumberFormatException x) {
            LOGGER.warn("Invalid {} {} at {}", Constants.MAGENTO_ROOT_CATEGORY_ID_PROPERTY, rootCategoryId, root.getPath());
            return null;
        }

        GraphqlDataService client = clients.get(catalogIdentifier);
        if (client == null) {
            LOGGER.warn("No MagentoGraphqlClient instance available for catalog identifier " + catalogIdentifier);
            return null;
        }

        ResourceProvider<T> resourceProvider = new GraphqlResourceProvider<T>(root.getPath(), client, scheduler, properties);
        return resourceProvider;
    }

    @Override
    public Collection<String> getAllCatalogIdentifiers() {
        return clients.keySet();
    }

    @Override
    public String getCommerceProviderName() {
        return MAGENTO_GRAPHQL_PROVIDER;
    }
}
