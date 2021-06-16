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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;

import com.adobe.cq.commerce.virtual.catalog.data.CatalogDataResourceProviderFactory;
import com.adobe.granite.test.tooling.RepositoryBaseTest;
import com.day.cq.commons.jcr.JcrUtil;
import junitx.util.PrivateAccessor;

public class CatalogDataResourceProviderManagerImplTest extends RepositoryBaseTest {
    private static final String TEST_PROVIDER_FACTORY_ID = "TestProviderFactory";
    private static final int WAIT_FOR_EVENTS = 200;
    private static final int ROOT_COUNT = 7;
    private Session session;
    private CatalogDataResourceProviderManagerImpl manager;
    private ComponentContext componentContext;
    private final AtomicInteger dataRootCounter = new AtomicInteger(1);
    private final AtomicInteger factoryCounter = new AtomicInteger(1);

    @Before
    public void beforeTest() throws Exception {
        getRepository().getDefaultWorkspace();
        session = getAdminSession();
        session.getWorkspace().getNamespaceRegistry().registerNamespace("cq", "http://www.day.com/jcr/cq/1.0");
        RepositoryUtil.registerSlingNodeTypes(session);

        manager = Mockito.spy(new CatalogDataResourceProviderManagerImpl());
        PrivateAccessor.setField(manager, "resolverFactory", getResourceResolverFactory());
        BundleContext bundleContext = Mockito.mock(BundleContext.class);
        Mockito.when(bundleContext.registerService((Class) Mockito.any(), (ResourceProvider) Mockito.any(), Mockito.any())).thenAnswer(
            (Answer<ServiceRegistration>) invocation -> Mockito.mock(ServiceRegistration.class));

        componentContext = Mockito.mock(ComponentContext.class);
        Mockito.when(componentContext.getBundleContext()).thenReturn(bundleContext);
    }

    @After
    public void afterTest() throws Exception {
        manager.deactivate(componentContext);
        final String absPath = "/var/commerce/products";
        if (session.nodeExists(absPath)) {
            Node root = session.getNode(absPath);
            root.remove();
            session.save();
        }
    }

    @Test
    public void testBindFactory() throws Exception {
        manager.activate(componentContext);

        FactoryConfig factoryConfig = bindFactory();
        Assert.assertEquals(1, manager.getProviderFactories().values().size());
        Assert.assertTrue(manager.getProviderFactories().values().contains(factoryConfig.factory));
        Assert.assertTrue(manager.getDataRoots().isEmpty());
    }

    @Test
    public void testUnbindFactory() throws Exception {
        manager.activate(componentContext);

        FactoryConfig factoryConfig = bindFactory();
        unbindFactory(factoryConfig);

        Assert.assertTrue(manager.getProviderFactories().values().isEmpty());
        Assert.assertTrue(manager.getDataRoots().isEmpty());
    }

    @Test
    public void testBindFactoryWithExistingRoot() throws Exception {
        testBindFactoryWithExistingRoots(1);
    }

    @Test
    public void testBindFactoryWithExistingRoots() throws Exception {
        testBindFactoryWithExistingRoots(ROOT_COUNT);
    }

    @Test
    public void testUnbindFactoryWithExistingRoot() throws Exception {
        testUnbindFactoryWithExistingRoot(1);
    }

    @Test
    public void testUnbindFactoryWithExistingRoots() throws Exception {
        testUnbindFactoryWithExistingRoot(ROOT_COUNT);
    }

    @Test
    public void testBindFactoryCreateRoot() throws Exception {
        testBindFactoryCreateRoots(1);
    }

    @Test
    public void testBindFactoryCreateInvalidRoot() throws Exception {
        manager.activate(componentContext);
        FactoryConfig factoryConfig = bindFactory();

        final String path = "/var/commerce/products/data" + dataRootCounter.getAndIncrement();
        Node root = JcrUtil.createPath(path, "sling:Folder", session);
        // invalid, empty property value
        root.setProperty(CatalogDataResourceProviderFactory.PROPERTY_FACTORY_ID, "");
        session.save();

        Thread.sleep(WAIT_FOR_EVENTS);

        Assert.assertEquals(0, getProviders().size());
        Assert.assertEquals(0, getProviderRegistrations().size());
        Assert.assertEquals(1, manager.getProviderFactories().values().size());
        Assert.assertTrue(manager.getProviderFactories().values().contains(factoryConfig.factory));
        Assert.assertEquals(0, manager.getDataRoots().size());
    }

    @Test
    public void testBindFactoryCreateRoots() throws Exception {
        testBindFactoryCreateRoots(ROOT_COUNT);
    }

    @Test
    public void testUnbindFactoryCreateRoot() throws Exception {
        testUnbindFactoryCreateRoots(1);
    }

    @Test
    public void testUnbindFactoryCreateRoots() throws Exception {
        testUnbindFactoryCreateRoots(ROOT_COUNT);
    }

    @Test
    public void testBindFactoryCreateRemoveRoots() throws Exception {
        testBindFactoryCreateRemoveRoots(ROOT_COUNT, ROOT_COUNT / 2);
    }

    @Test
    public void testBindFactoryCreateRoots2() throws Exception {
        testBindFactoryCreateRoots2(ROOT_COUNT);
    }

    @Test
    public void testBindFactoryModifyRoot() throws Exception {
        manager.activate(componentContext);

        FactoryConfig factoryConfig = bindFactory();

        String rootPath = createDataRoot(factoryConfig.factoryId);

        Thread.sleep(WAIT_FOR_EVENTS);

        Assert.assertEquals(1, getProviders().size());
        Assert.assertEquals(1, getProviderRegistrations().size());
        Assert.assertEquals(1, manager.getProviderFactories().values().size());
        Assert.assertTrue(manager.getProviderFactories().values().contains(factoryConfig.factory));

        Assert.assertTrue(getProviderRegistrations().keySet().iterator().hasNext());
        Object oldProvider = getProviderRegistrations().keySet().iterator().next();
        Assert.assertNotNull(oldProvider);

        Assert.assertTrue(session.nodeExists(rootPath));
        Node root = session.getNode(rootPath);
        root.setProperty("dummy", "dummy");
        session.save();

        Thread.sleep(WAIT_FOR_EVENTS);

        Assert.assertEquals(1, getProviders().size());
        Assert.assertEquals(1, getProviderRegistrations().size());
        Assert.assertEquals(1, manager.getProviderFactories().values().size());
        Assert.assertTrue(manager.getProviderFactories().values().contains(factoryConfig.factory));
        Assert.assertEquals(1, manager.getDataRoots().size());

        Assert.assertTrue(getProviderRegistrations().keySet().iterator().hasNext());
        Object newPovider = getProviderRegistrations().keySet().iterator().next();
        Assert.assertNotNull(newPovider);

        // check reregistration: new provider not the same as old provider
        Assert.assertNotSame(oldProvider, newPovider);
    }

    @Test
    public void testBindNullFactoryModifyRoot() throws Exception {
        manager.activate(componentContext);

        FactoryConfig factoryConfig = bindFactory();
        FactoryConfig nullFactoryConfig = bindNullFactory();

        String rootPath = createDataRoot(factoryConfig.factoryId);

        Thread.sleep(WAIT_FOR_EVENTS);

        Assert.assertEquals(1, getProviders().size());
        Assert.assertEquals(1, getProviderRegistrations().size());
        Assert.assertEquals(2, manager.getProviderFactories().values().size());
        Assert.assertEquals(1, manager.getDataRoots().size());
        Assert.assertTrue(manager.getProviderFactories().values().contains(factoryConfig.factory));
        Assert.assertTrue(manager.getProviderFactories().values().contains(nullFactoryConfig.factory));

        Assert.assertTrue(getProviderRegistrations().keySet().iterator().hasNext());
        Object oldProvider = getProviderRegistrations().keySet().iterator().next();
        Assert.assertNotNull(oldProvider);

        Assert.assertTrue(session.nodeExists(rootPath));
        Node root = session.getNode(rootPath);

        // change data root to null provider factory
        root.setProperty(CatalogDataResourceProviderFactory.PROPERTY_FACTORY_ID, nullFactoryConfig.factoryId);
        session.save();

        Thread.sleep(WAIT_FOR_EVENTS);

        // check that no provider was created
        Assert.assertEquals(0, getProviders().size());
        Assert.assertEquals(0, getProviderRegistrations().size());
        Assert.assertEquals(2, manager.getProviderFactories().values().size());
        Assert.assertEquals(0, manager.getDataRoots().size());
        Assert.assertTrue(manager.getProviderFactories().values().contains(factoryConfig.factory));
        Assert.assertTrue(manager.getProviderFactories().values().contains(nullFactoryConfig.factory));

        Assert.assertFalse(getProviderRegistrations().keySet().iterator().hasNext());

        // change data root to provider factory
        root.setProperty(CatalogDataResourceProviderFactory.PROPERTY_FACTORY_ID, factoryConfig.factoryId);
        session.save();

        Thread.sleep(WAIT_FOR_EVENTS);

        // check that provider was created
        Assert.assertEquals(1, getProviders().size());
        Assert.assertEquals(1, getProviderRegistrations().size());
        Assert.assertEquals(2, manager.getProviderFactories().values().size());
        Assert.assertEquals(1, manager.getDataRoots().size());
        Assert.assertTrue(manager.getProviderFactories().values().contains(factoryConfig.factory));
        Assert.assertTrue(manager.getProviderFactories().values().contains(nullFactoryConfig.factory));

        Assert.assertTrue(getProviderRegistrations().keySet().iterator().hasNext());
        Object newPovider = getProviderRegistrations().keySet().iterator().next();
        Assert.assertNotNull(newPovider);

        // check reregistration: new provider not the same as old provider
        Assert.assertNotSame(oldProvider, newPovider);
    }

    @Test
    public void testCreateNodesNextExistingRoots() throws Exception {
        testBindFactoryCreateRoots(2);
        List<Resource> dataRoots = manager.getDataRoots();

        // state before
        Assert.assertEquals(2, dataRoots.size());
        Mockito.verify(manager, Mockito.times(4)).findDataRoots(Mockito.any());

        // nodes added
        Resource dataRoot = dataRoots.get(0);
        Node testNode = null;
        for (int i = 0; i < 3; i++) {
            testNode = JcrUtil.createPath(dataRoot.getParent() + "/testNode" + i, "nt:unstructured", session);
            JcrUtil.createPath(dataRoot.getParent() + "/testFolder" + i, "sling:Folder", session);
        }
        session.save();

        Thread.sleep(WAIT_FOR_EVENTS);

        // state after: no new data nodes added, no new findDataRoots() invocation occurred
        Assert.assertEquals(2, manager.getDataRoots().size());
        Mockito.verify(manager, Mockito.times(4)).findDataRoots(Mockito.any());

        testNode.setProperty("aProp", "aValue");
        session.save();
        Thread.sleep(WAIT_FOR_EVENTS);

        // state after
        Assert.assertEquals(2, manager.getDataRoots().size());
        Mockito.verify(manager, Mockito.times(4)).findDataRoots(Mockito.any());

        testNode.remove();
        session.save();
        Thread.sleep(WAIT_FOR_EVENTS);

        // state after
        Assert.assertEquals(2, manager.getDataRoots().size());
        Mockito.verify(manager, Mockito.times(4)).findDataRoots(Mockito.any());
    }

    private void testBindFactoryCreateRemoveRoots(int createCount, int removeCount) throws Exception {
        if (removeCount > createCount) {
            throw new IllegalArgumentException("removeCount above createCount: " + removeCount + " > " + createCount);
        }

        manager.activate(componentContext);

        FactoryConfig factoryConfig = bindFactory();

        List<String> toRemove = new ArrayList<>();
        for (int i = 0; i < createCount; i++) {
            String path = createDataRoot(factoryConfig.factoryId, false);
            if (i < removeCount) {
                toRemove.add(path);
                removeDataRoot(path, false);
            }
        }
        session.save();

        Thread.sleep(WAIT_FOR_EVENTS);

        for (String path : toRemove) {
            removeDataRoot(path, false);
        }
        session.save();
        Thread.sleep(WAIT_FOR_EVENTS);

        int remaining = createCount - removeCount;
        Assert.assertEquals(remaining, getProviders().size());
        Assert.assertEquals(remaining, getProviderRegistrations().size());
        Assert.assertEquals(1, manager.getProviderFactories().values().size());
        Assert.assertTrue(manager.getProviderFactories().values().contains(factoryConfig.factory));
        Assert.assertEquals(remaining, manager.getDataRoots().size());
    }

    private void testBindFactoryCreateRoots(int rootCount) throws Exception {
        manager.activate(componentContext);

        FactoryConfig factoryConfig = bindFactory();

        for (int i = 0; i < rootCount; i++) {
            createDataRoot(factoryConfig.factoryId);
        }

        Thread.sleep(WAIT_FOR_EVENTS);

        Assert.assertEquals(rootCount, getProviders().size());
        Assert.assertEquals(rootCount, getProviderRegistrations().size());
        Assert.assertEquals(1, manager.getProviderFactories().values().size());
        Assert.assertTrue(manager.getProviderFactories().values().contains(factoryConfig.factory));
        Assert.assertEquals(rootCount, manager.getDataRoots().size());
    }

    private void testBindFactoryCreateRoots2(int rootCount) throws Exception {
        manager.activate(componentContext);

        FactoryConfig factoryConfig1 = bindFactory();

        for (int i = 0; i < rootCount; i++) {
            createDataRoot(factoryConfig1.factoryId);
        }

        Thread.sleep(WAIT_FOR_EVENTS);

        FactoryConfig factoryConfig2 = bindFactory();

        for (int i = 0; i < rootCount; i++) {
            createDataRoot(factoryConfig2.factoryId);
        }

        Thread.sleep(WAIT_FOR_EVENTS);

        Assert.assertEquals(2 * rootCount, getProviders().size());
        Assert.assertEquals(2 * rootCount, getProviderRegistrations().size());
        Assert.assertEquals(2, manager.getProviderFactories().values().size());
        Assert.assertTrue(manager.getProviderFactories().values().contains(factoryConfig1.factory));
        Assert.assertTrue(manager.getProviderFactories().values().contains(factoryConfig2.factory));
        Assert.assertEquals(2 * rootCount, manager.getDataRoots().size());
    }

    private void testUnbindFactoryCreateRoots(int rootCount) throws Exception {
        manager.activate(componentContext);

        FactoryConfig factoryConfig = bindFactory();

        for (int i = 0; i < rootCount; i++) {
            createDataRoot(factoryConfig.factoryId);
        }

        Thread.sleep(WAIT_FOR_EVENTS);

        unbindFactory(factoryConfig);

        Assert.assertTrue(getProviders().isEmpty());
        Assert.assertTrue(getProviderRegistrations().isEmpty());
        Assert.assertTrue(manager.getProviderFactories().values().isEmpty());
        Assert.assertTrue(manager.getDataRoots().isEmpty());
    }

    private void testBindFactoryWithExistingRoots(int rootCount) throws Exception {
        FactoryConfig factoryConfig = createFactoryConfig();

        for (int i = 0; i < rootCount; i++) {
            createDataRoot(factoryConfig.factoryId);
        }

        manager.activate(componentContext);

        bindFactory(factoryConfig);

        Assert.assertEquals(rootCount, getProviders().size());
        Assert.assertEquals(rootCount, getProviderRegistrations().size());
        Assert.assertEquals(1, manager.getProviderFactories().values().size());
        Assert.assertTrue(manager.getProviderFactories().values().contains(factoryConfig.factory));
        Assert.assertEquals(rootCount, manager.getDataRoots().size());
    }

    private void testUnbindFactoryWithExistingRoot(int rootCount) throws Exception {
        FactoryConfig factoryConfig = createFactoryConfig();

        for (int i = 0; i < rootCount; i++) {
            createDataRoot(factoryConfig.factoryId);
        }

        manager.activate(componentContext);

        bindFactory(factoryConfig);
        unbindFactory(factoryConfig);

        Assert.assertTrue(getProviders().isEmpty());
        Assert.assertTrue(getProviderRegistrations().isEmpty());
        Assert.assertTrue(manager.getProviderFactories().values().isEmpty());
        Assert.assertTrue(manager.getDataRoots().isEmpty());
    }

    private Map<String, CatalogDataResourceProviderFactory<?>> getProviders() throws NoSuchFieldException {
        return (Map<String, CatalogDataResourceProviderFactory<?>>) PrivateAccessor.getField(manager, "providers");
    }

    private Map<ResourceProvider, ServiceRegistration<?>> getProviderRegistrations() throws NoSuchFieldException {
        return (Map<ResourceProvider, ServiceRegistration<?>>) PrivateAccessor.getField(manager, "providerRegistrations");
    }

    private void unbindFactory(FactoryConfig factoryConfig) {
        manager.unbindFactory(factoryConfig.factory, factoryConfig.properties);
    }

    private String createDataRoot(String factoryId) throws RepositoryException {
        return createDataRoot(factoryId, true);
    }

    private String createDataRoot(String factoryId, boolean save) throws RepositoryException {
        final String path = "/var/commerce/products/data" + dataRootCounter.getAndIncrement();
        Node root = JcrUtil.createPath(path, "sling:Folder", session);
        root.setProperty(CatalogDataResourceProviderFactory.PROPERTY_FACTORY_ID, factoryId);
        if (save)
            session.save();

        return path;
    }

    private void removeDataRoot(String path, boolean save) throws Exception {
        if (session.nodeExists(path)) {
            session.getNode(path).remove();
        }
        if (save) {
            session.save();
        }
    }

    private FactoryConfig createFactoryConfig() {
        return new FactoryConfig(false);
    }

    private FactoryConfig createNullFactoryConfig() {
        return new FactoryConfig(true);
    }

    private FactoryConfig bindFactory() {
        FactoryConfig factoryConfig = createFactoryConfig();
        bindFactory(factoryConfig);

        return factoryConfig;
    }

    private FactoryConfig bindNullFactory() {
        FactoryConfig factoryConfig = createNullFactoryConfig();
        bindFactory(factoryConfig);

        return factoryConfig;
    }

    private void bindFactory(FactoryConfig factoryConfig) {
        CatalogDataResourceProviderFactory providerFactory = factoryConfig.factory;
        Map<String, String> properties = factoryConfig.properties;
        manager.bindFactory(providerFactory, properties);
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

            factoryId = TEST_PROVIDER_FACTORY_ID + factoryCounter.getAndIncrement();
            Map<String, String> properties1 = new HashMap<>();
            properties1.put(CatalogDataResourceProviderFactory.PROPERTY_FACTORY_SERVICE_ID, factoryId);
            properties = properties1;
        }

    }
}
