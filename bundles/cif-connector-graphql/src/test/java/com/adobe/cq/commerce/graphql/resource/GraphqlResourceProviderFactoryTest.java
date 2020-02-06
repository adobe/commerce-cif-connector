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
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.graphql.magento.GraphqlAemContext;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceImpl;
import com.adobe.cq.commerce.graphql.magento.MockGraphqlDataServiceConfiguration;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GraphqlResourceProviderFactoryTest {

    @Rule
    public final AemContext context = GraphqlAemContext.createContext(ImmutableMap.<String, String>of(
        "/content", "/context/graphql-client-adapter-factory-context.json",
        "/conf/test-config/settings", "/context/jcr-conf.json"));

    private GraphqlResourceProviderFactory<?> factory;
    private GraphqlDataServiceImpl client;

    @Before
    public void setUp() throws Exception {

        factory = new GraphqlResourceProviderFactory<>();

        client = Mockito.mock(GraphqlDataServiceImpl.class);
        MockGraphqlDataServiceConfiguration config = Mockito.spy(new MockGraphqlDataServiceConfiguration());
        Mockito.when(config.catalogCachingSchedulerEnabled()).thenReturn(false);
        Mockito.when(client.getConfiguration()).thenReturn(config);
        Mockito.when(client.getIdentifier()).thenReturn("my-catalog");

        factory.bindGraphqlDataService(client, null);
    }

    @Test
    public void testGetClientForPageWithContextConfiguration() {
        /*
         * The content for this test looks slightly different than it does in AEM:
         * In AEM there the tree structure is /conf/<config>/settings/cloudconfigs/commerce/jcr:content
         * In our test content it's /conf/<config>/settings/cloudconfigs/commerce
         * The reason is that AEM has a specific CaConfig API implementation that reads the configuration
         * data from the jcr:content node of the configuration page, something which we cannot reproduce in
         * a unit test scenario.
         */
        Resource root = context.resourceResolver().getResource("/content/pageF");
        ResourceProvider<?> provider = factory.createResourceProvider(root);

        Assert.assertNotNull(provider);
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
