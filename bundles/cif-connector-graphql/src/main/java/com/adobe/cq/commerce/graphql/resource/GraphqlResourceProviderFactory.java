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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.ConfigurationBuilder;
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
import com.day.cq.commons.inherit.ComponentInheritanceValueMap;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
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
public class GraphqlResourceProviderFactory implements CatalogDataResourceProviderFactory<Object>, CatalogIdentifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphqlResourceProviderFactory.class);

    private static final String CONFIGURATION_NAME = "cloudconfigs/commerce";

    protected Map<String, GraphqlDataService> clients = new ConcurrentHashMap<>();

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

    @Override
    public GraphqlResourceProvider createResourceProvider(Resource root) {
        LOGGER.debug("Creating resource provider for resource at path {}", root.getPath());

        ConfigurationBuilder configurationBuilder = root.adaptTo(ConfigurationBuilder.class);
        ValueMap properties = configurationBuilder.name(CONFIGURATION_NAME)
            .asValueMap();

        Map<String, String> collectedProperties = new HashMap<>();
        if (properties.size() == 0) {
            collectedProperties = readFallbackConfiguration(root);
        } else {
            for (String key : properties.keySet()) {
                collectedProperties.put(key, properties.get(key, ""));
            }
        }

        String catalogIdentifier = collectedProperties.get(GraphqlDataServiceConfiguration.CQ_CATALOG_IDENTIFIER);
        if (StringUtils.isEmpty(catalogIdentifier)) {
            LOGGER.warn("Could not find cq:catalogIdentifier property for given resource at " + root.getPath());
            return null;
        }

        // Check Magento root category id
        String rootCategoryId = collectedProperties.get(Constants.MAGENTO_ROOT_CATEGORY_ID_PROPERTY);
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

        GraphqlResourceProvider resourceProvider = new GraphqlResourceProvider(root.getPath(), client, collectedProperties);
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

    private Map<String, String> readFallbackConfiguration(Resource res) {
        InheritanceValueMap ivm;
        Page page = res.getResourceResolver()
            .adaptTo(PageManager.class)
            .getContainingPage(res);
        if (page != null) {
            ivm = new HierarchyNodeInheritanceValueMap(page.getContentResource());
        } else {
            ivm = new ComponentInheritanceValueMap(res);
        }

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(GraphqlDataServiceConfiguration.CQ_CATALOG_IDENTIFIER, ivm.getInherited(
            GraphqlDataServiceConfiguration.CQ_CATALOG_IDENTIFIER, String.class));
        properties.put(Constants.MAGENTO_ROOT_CATEGORY_ID_PROPERTY, ivm.getInherited(Constants.MAGENTO_ROOT_CATEGORY_ID_PROPERTY,
            String.class));
        properties.put(Constants.MAGENTO_STORE_PROPERTY, ivm.getInherited("cq:" + Constants.MAGENTO_STORE_PROPERTY, String.class));

        return properties;
    }
}
