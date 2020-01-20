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

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.cq.commerce.graphql.magento.GraphqlAemContext;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataService;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceImpl;
import com.adobe.cq.commerce.graphql.magento.MockGraphqlDataServiceConfiguration;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphqlResourceProviderFactoryTest {

    @Rule
    public final AemContext context = GraphqlAemContext.createContext("/context/graphql-client-adapter-factory-context.json", "/content");

    private GraphqlResourceProviderFactory<?> factory;
    private GraphqlDataServiceImpl client;

    @Before
    public void setUp() throws Exception {

        ConfigurationResourceResolver configurationResourceResolver = mock(ConfigurationResourceResolver.class);
        Resource mockConfigurationResource = mock(Resource.class);

        Map<String, Object> configuration = new HashMap<>();
        configuration.put("cq:graphqlClient", "my-catalog");
        configuration.put("cq:catalogIdentifier", "my-catalog");
        configuration.put("magentoRootCategoryId", "4");

        when(mockConfigurationResource.getValueMap()).thenReturn(new ValueMapDecorator(configuration));
        when(configurationResourceResolver.getResource(any(Resource.class), eq("settings"), eq("commerce/default"))).thenReturn(
            mockConfigurationResource);
        context.registerService(ConfigurationResourceResolver.class, configurationResourceResolver);

        factory = new GraphqlResourceProviderFactory<>();

        client = Mockito.mock(GraphqlDataServiceImpl.class);
        MockGraphqlDataServiceConfiguration config = Mockito.spy(new MockGraphqlDataServiceConfiguration());
        Mockito.when(config.catalogCachingSchedulerEnabled()).thenReturn(false);
        Mockito.when(client.getConfiguration()).thenReturn(config);
        Mockito.when(client.getIdentifier()).thenReturn("my-catalog");

        context.registerService(GraphqlDataService.class, client);

        context.registerService(Scheduler.class, Mockito.mock(Scheduler.class));
        context.registerInjectActivateService(factory);
    }

    @Test
    public void testGetClientForPageWithIdentifier() throws Exception {
        // Get page which has the catalog identifier in its jcr:content node
        Resource root = context.resourceResolver().getResource("/content/pageA");

        ResourceProvider<?> provider = factory.createResourceProvider(root);
        Assert.assertNotNull(provider);
    }

    // @Test
    public void testGetClientForPageWithInheritedIdentifier() {
        // Get page whose parent has the catalog identifier in its jcr:content node
        Resource root = context.resourceResolver().getResource("/content/pageB/pageC");

        ResourceProvider<?> provider = factory.createResourceProvider(root);
        Assert.assertNotNull(provider);
    }

    // @Test
    public void testReturnNullForPageWithoutIdentifier() {
        // Get page without catalog identifier
        Resource root = context.resourceResolver().getResource("/content/pageD");

        ResourceProvider<?> provider = factory.createResourceProvider(root);
        Assert.assertNull(provider);
    }

    // @Test
    public void testReturnNullForPageWithInvalidRootCategoryIdentifier() {
        // Get page without catalog identifier
        Resource root = context.resourceResolver().getResource("/content/pageE");

        ResourceProvider<?> provider = factory.createResourceProvider(root);
        Assert.assertNull(provider);
    }

    @Test
    public void testBindings() {
        Assert.assertEquals(1, factory.getAllCatalogIdentifiers().size());
        factory.unbindGraphqlDataService(client, null);
        Assert.assertEquals(0, factory.getAllCatalogIdentifiers().size());

        // Get page which has the catalog identifier in its jcr:content node
        Resource root = context.resourceResolver().getResource("/content/pageA");
        ResourceProvider<?> provider = factory.createResourceProvider(root);
        Assert.assertNull(provider);
    }
}
