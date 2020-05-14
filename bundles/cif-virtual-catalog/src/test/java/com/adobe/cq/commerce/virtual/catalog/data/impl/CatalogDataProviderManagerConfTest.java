/*******************************************************************************
 *
 *
 *      Copyright 2020 Adobe. All rights reserved.
 *      This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License. You may obtain a copy
 *      of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software distributed under
 *      the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *      OF ANY KIND, either express or implied. See the License for the specific language
 *      governing permissions and limitations under the License.
 *
 ******************************************************************************/

package com.adobe.cq.commerce.virtual.catalog.data.impl;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.adobe.cq.commerce.virtual.catalog.data.CatalogDataResourceProviderFactory;
import com.adobe.cq.commerce.virtual.catalog.data.CatalogDataResourceProviderManager;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextBuilder;

import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;

public class CatalogDataProviderManagerConfTest {

    private static final String TEST_PROVIDER_FACTORY_ID = "TestProviderFactory";

    private static final String COMMERCE_ROOT = "/var/commerce";
    private static final String NN_DATA_ROOT = "testing";

    private CatalogDataResourceProviderManager manager;
    @Rule
    public AemContext context = new AemContextBuilder(ResourceResolverType.JCR_OAK).plugin(CACONFIG).beforeSetUp(ctx -> {
        ConfigurationAdmin configurationAdmin = ctx.getService(ConfigurationAdmin.class);
        Configuration serviceConfiguration = configurationAdmin.getConfiguration(
            "org.apache.sling.caconfig.resource.impl.def.DefaultContextPathStrategy");

        Dictionary<String, Object> props = new Hashtable<>();
        props.put("configRefResourceNames", new String[] { ".", "jcr:content" });
        props.put("configRefPropertyNames", "cq:conf");
        serviceConfiguration.update(props);

        serviceConfiguration = configurationAdmin.getConfiguration(
            "org.apache.sling.caconfig.resource.impl.def.DefaultConfigurationResourceResolvingStrategy");
        props = new Hashtable<>();
        props.put("configPath", "/conf");
        serviceConfiguration.update(props);

        serviceConfiguration = configurationAdmin.getConfiguration(
            "org.apache.sling.caconfig.impl.ConfigurationResolverImpl");
        props = new Hashtable<>();
        props.put("configBucketNames", new String[] { "settings" });
        serviceConfiguration.update(props);
    }).build();

    @Before
    public void setUp() throws IOException, RepositoryException {
        // load sample content
        context.load().json("/context/jcr-conf.json", "/conf/testing/settings");
        context.load().json("/context/jcr-dataroots.json", COMMERCE_ROOT);
        context.load().json("/context/jcr-wrong-conf.json", "/conf/wrong-configuration/settings");

        // register the services
        context.registerService(new MockResourceResolverFactory());
        FactoryConfig factoryConfig = new FactoryConfig(false);

        Map<String, String> properties = factoryConfig.properties;
        context.registerService(CatalogDataResourceProviderFactory.class, factoryConfig.factory, properties);

        ServiceUserMapped serviceUserMapped = Mockito.mock(ServiceUserMapped.class);
        context.registerService(ServiceUserMapped.class, serviceUserMapped, ImmutableMap.of(ServiceUserMapped.SUBSERVICENAME,
            "virtual-products-service"));

        manager = context.registerInjectActivateService(new CatalogDataResourceProviderManagerImpl());
    }

    @Test
    public void testDataRoots() throws LoginException, RepositoryException, InterruptedException {
        String expectedDataRootPath = COMMERCE_ROOT + "/products/" + NN_DATA_ROOT;

        List<Resource> dataRoots = manager.getDataRoots();
        Assert.assertEquals("The manager found two data roots", 3, dataRoots.size());

        Resource dataRoot = dataRoots.get(0);
        Assert.assertEquals("The data root points to " + expectedDataRootPath, expectedDataRootPath, dataRoot.getPath());

    }

    private class FactoryConfig {
        CatalogDataResourceProviderFactory factory;
        Map<String, String> properties;
        String factoryId;

        FactoryConfig(boolean nullFactory) {
            CatalogDataResourceProviderFactory providerFactory = Mockito.mock(CatalogDataResourceProviderFactory.class);
            Mockito.when(providerFactory.createResourceProvider(Mockito.any())).thenAnswer(
                (Answer<ResourceProvider>) invocation -> nullFactory ? null
                    : Mockito
                        .mock(ResourceProvider.class));
            factory = providerFactory;

            factoryId = TEST_PROVIDER_FACTORY_ID;
            Map<String, String> properties1 = new HashMap<>();
            properties1.put(CatalogDataResourceProviderFactory.PROPERTY_FACTORY_SERVICE_ID, factoryId);
            properties = properties1;
        }

    }
}
