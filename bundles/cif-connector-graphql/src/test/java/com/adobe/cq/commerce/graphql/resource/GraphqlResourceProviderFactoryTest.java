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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.cq.commerce.graphql.magento.GraphqlAemContext;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceImpl;
import com.adobe.cq.commerce.graphql.magento.MockGraphqlDataServiceConfiguration;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.mockito.Matchers.any;
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
        when(mockConfigurationResource.getValueMap()).thenReturn(new ValueMapDecorator(ImmutableMap.<String, Object>of("cq:graphqlClient", "my-catalog")));
        when(configurationResourceResolver.getResource(any(Resource.class), any(String.class), any(String.class))).thenReturn(mockConfigurationResource);
        context.registerService(configurationResourceResolver);
        factory = new GraphqlResourceProviderFactory<>();

        context.registerInjectActivateService(factory);

        client = Mockito.mock(GraphqlDataServiceImpl.class);
        MockGraphqlDataServiceConfiguration config = Mockito.spy(new MockGraphqlDataServiceConfiguration());
        Mockito.when(config.catalogCachingSchedulerEnabled()).thenReturn(false);
        Mockito.when(client.getConfiguration()).thenReturn(config);
        Mockito.when(client.getIdentifier()).thenReturn("my-catalog");

        factory.bindGraphqlDataService(client, null);
    }

    @Test
    public void testGetClientForPageWithIdentifier() throws Exception {
        // Get page which has the catalog identifier in its jcr:content node
        Resource root = context.resourceResolver().getResource("/content/pageA");

        ResourceProvider<?> provider = factory.createResourceProvider(root);
        Assert.assertNotNull(provider);
    }

    @Test
    public void testGetClientForPageWithInheritedIdentifier() {
        // Get page whose parent has the catalog identifier in its jcr:content node
        Resource root = context.resourceResolver().getResource("/content/pageB/pageC");

        ResourceProvider<?> provider = factory.createResourceProvider(root);
        Assert.assertNotNull(provider);
    }

    @Test
    public void testReturnNullForPageWithoutIdentifier() {
        // Get page without catalog identifier
        Resource root = context.resourceResolver().getResource("/content/pageD");

        ResourceProvider<?> provider = factory.createResourceProvider(root);
        Assert.assertNull(provider);
    }

    @Test
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
