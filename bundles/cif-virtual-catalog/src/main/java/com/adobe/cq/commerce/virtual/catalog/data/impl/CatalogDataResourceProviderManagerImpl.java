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
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.virtual.catalog.data.CatalogDataResourceProviderFactory;
import com.adobe.cq.commerce.virtual.catalog.data.CatalogDataResourceProviderManager;

import static org.apache.sling.spi.resource.provider.ResourceProvider.PROPERTY_ROOT;

/**
 * Manages {@link CatalogDataResourceProviderFactory} instances and service registrations for {@link ResourceProvider} instances.
 * Provides read-only access to all registered providers.
 */
@Component(
    service = CatalogDataResourceProviderManager.class,
    immediate = true,
    property = { Constants.SERVICE_DESCRIPTION + "=Manages the resource registrations for the Virtual Catalog Resource Provider Manager." })
public class CatalogDataResourceProviderManagerImpl implements CatalogDataResourceProviderManager, EventListener {

    private static final String VIRTUAL_PRODUCTS_SERVICE = "virtual-products-service";

    private static final String OBSERVATION_PATHS_DEFAULT = "/var/commerce/products";

    private static final String FINDALLQUERIES_DEFAULT = "JCR-SQL2|SELECT * FROM [sling:Folder] WHERE ISDESCENDANTNODE('"
        + OBSERVATION_PATHS_DEFAULT + "') AND ([sling:Folder].'" + CatalogDataResourceProviderFactory.PROPERTY_FACTORY_ID
        + "' IS NOT NULL" + " OR [sling:Folder].'cq:conf' IS NOT NULL)";

    private String[] findAllQueries = { FINDALLQUERIES_DEFAULT };

    private String[] obervationPaths = { OBSERVATION_PATHS_DEFAULT };

    private EventListener[] observationEventListeners;

    private volatile List<Resource> dataRoots;

    /**
     * Map for holding the virtual catalog data resource provider registrations, with the catalog provider as key and the registration as
     * value.
     */
    private final Map<ResourceProvider, ServiceRegistration<?>> providerRegistrations = Collections.synchronizedMap(
        new IdentityHashMap<>());

    /**
     * Map for holding the virtual catalog data resource provider mappings, with the catalog root path as key and the providers as values.
     */
    private ConcurrentMap<String, ResourceProvider<?>> providers = new ConcurrentHashMap<>();

    private final Map<String, CatalogDataResourceProviderFactory<?>> providerFactories = new ConcurrentHashMap<>();

    @Reference
    private ResourceResolverFactory resolverFactory = null;

    /**
     * Service resource resolver (read only usage)
     */
    private ResourceResolver resolver;

    /**
     * This bundle's context.
     */
    private BundleContext bundleContext;

    /**
     * The default logger
     */
    private static final Logger log = LoggerFactory.getLogger(CatalogDataResourceProviderManagerImpl.class);

    /**
     * Find all existing virtual catalog data roots using all query defined in service configuration.
     *
     * @param resolver Resource resolver
     * @return all virtual catalog roots
     */
    @SuppressWarnings("unchecked")
    private List<Resource> findDataRoots(ResourceResolver resolver) {
        List<Resource> allResources = new ArrayList<>();
        for (String queryString : this.findAllQueries) {
            if (!StringUtils.contains(queryString, "|")) {
                throw new IllegalArgumentException("Query string does not contain query syntax seperated by '|': " + queryString);
            }
            String queryLanguage = StringUtils.substringBefore(queryString, "|");
            String query = StringUtils.substringAfter(queryString, "|");
            // data roots are JCR nodes, so we prefer JCR query because the resource resolver appears to be unreliable
            // when we are in the middle of registering/unregistering resource providers
            try {
                Session session = resolver.adaptTo(Session.class);
                Workspace workspace = session.getWorkspace();
                QueryManager qm = workspace.getQueryManager();
                Query jcrQuery = qm.createQuery(query, queryLanguage);
                QueryResult result = jcrQuery.execute();
                NodeIterator nodes = result.getNodes();
                while (nodes.hasNext()) {
                    Node node = nodes.nextNode();
                    Resource resource = resolver.getResource(node.getPath());
                    if (resource != null) {
                        allResources.add(resource);
                    }
                }
            } catch (RepositoryException x) {
                log.error("Error finding data roots", x);
            }
        }
        dataRoots = allResources;
        return allResources;
    }

    private void registerDataRoots() {
        log.debug("Start registering all virtual catalog trees...");
        final long start = System.currentTimeMillis();
        long countSuccess = 0;
        long countFailed = 0;

        final List<Resource> existingVIrtualCatalogs = findDataRoots(resolver);
        for (Resource virtualCatalogRootResource : existingVIrtualCatalogs) {
            boolean success = registerDataRoot(virtualCatalogRootResource);
            if (success) {
                countSuccess++;
            } else {
                countFailed++;
            }
        }

        final long time = System.currentTimeMillis() - start;
        log.info("Registered {} virtual catalog data resource providers(s) in {} ms, skipping {} invalid one(s).", countSuccess, time,
            countFailed);
    }

    /**
     * Tries to register a resource provider at the provided resource.
     * The property defined by {@link CatalogDataResourceProviderFactory#PROPERTY_FACTORY_ID} of the resource
     *
     * @param root the root folder of the provider
     * @return true if registration was done, false if skipped (already registered)
     */
    private boolean registerDataRoot(Resource root) {
        log.debug("Registering data root at {}", root.getPath());
        log.debug("This catalog manager has {} factories registered...", providerFactories.size());
        String rootPath = root.getPath();
        String providerId = getJcrStringProperty(rootPath, CatalogDataResourceProviderFactory.PROPERTY_FACTORY_ID);
        String cqConf = getJcrStringProperty(rootPath, "cq:conf");
        boolean valid = true;
        CatalogDataResourceProviderFactory factory = null;

        if (StringUtils.isNotEmpty(cqConf)) {
            log.debug("Found cq:conf property pointing at {}", cqConf);
            ConfigurationBuilder cfgBuilder = root.adaptTo(ConfigurationBuilder.class);
            ValueMap properties = cfgBuilder.name("commerce/default").asValueMap();
            providerId = properties.get(CatalogDataResourceProviderFactory.PROPERTY_FACTORY_ID, String.class);
            factory = providerFactories.get(providerId);
        } else {

            if (StringUtils.isBlank(rootPath)) {
                log.error("Root path is empty. Registering this data root will fail");
                valid = false;
            }
            if (StringUtils.isBlank(providerId)) {
                log.error("No providerId property found on node {}. Registering this data root will fail", rootPath);
                valid = false;
            }

            if ((factory = providerFactories.get(providerId)) == null) {
                log.error("No factory found for provider id {}. Registering this data root will fail", providerId);
                valid = false;
            }
        }
        log.debug("Factory retrieved... {}", factory != null);
        // register valid
        if (valid) {
            final ResourceProvider provider = factory.createResourceProvider(root);
            // provider is null when there's a misconfiguration in the root resource
            final ResourceProvider oldProvider = provider == null ? providers.remove(rootPath) : providers.put(rootPath, provider);

            // unregister in case there was a provider registered before
            if (provider == null && oldProvider != null) {
                unregisterService(oldProvider);
            } else if (provider != null && oldProvider == null) {
                registerService(rootPath, provider);
                return true;
            } else if (provider != null && !provider.equals(oldProvider)) {
                log.debug("(Re-)registering resource provider {}.", rootPath);
                unregisterService(oldProvider);
                registerService(rootPath, provider);
                return true;
            } else {
                log.debug("Skipped re-registering resource provider {} because there were no relevant changes.", rootPath);
            }
        } else {
            // otherwise remove previous virtual catalog resource provider if new virtual catalog definition is not valid
            final ResourceProvider oldProvider = providers.remove(rootPath);
            if (oldProvider != null) {
                log.debug("Unregistering resource provider {}.", rootPath);
                unregisterService(oldProvider);
            }
            log.warn("Virtual catalog data definition at '{}' is invalid.", rootPath);
        }

        return false;
    }

    private String getJcrStringProperty(String pNodePath, String pPropertName) {
        String absolutePropertyPath = pNodePath + "/" + pPropertName;
        Session session = resolver.adaptTo(Session.class);
        try {
            if (!session.itemExists(absolutePropertyPath)) {
                return null;
            }
            return session.getProperty(absolutePropertyPath)
                .getString();
        } catch (RepositoryException ex) {
            return null;
        }
    }

    // ---------- SCR Integration

    @Activate
    protected synchronized void activate(final ComponentContext ctx) throws LoginException, RepositoryException {
        // check enabled state
        if (resolver == null) {
            bundleContext = ctx.getBundleContext();
            final Map<String, Object> map = new HashMap<>();
            map.put(ResourceResolverFactory.SUBSERVICE, VIRTUAL_PRODUCTS_SERVICE);
            resolver = resolverFactory.getServiceResourceResolver(map);

            // Watch for events on the root to register/deregister virtual catalogs data roots at runtime
            // For each observed path create an event listener object which redirects the event to the main class
            final Session session = resolver.adaptTo(Session.class);
            if (session != null) {
                this.observationEventListeners = new EventListener[this.obervationPaths.length];
                for (int i = 0; i < this.obervationPaths.length; i++) {
                    this.observationEventListeners[i] = new EventListener() {
                        public void onEvent(EventIterator events) {
                            CatalogDataResourceProviderManagerImpl.this.onEvent(events);
                        }
                    };
                    session.getWorkspace()
                        .getObservationManager()
                        .addEventListener(this.observationEventListeners[i],
                            Event.NODE_ADDED | Event.NODE_REMOVED | Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED,
                            this.obervationPaths[i],
                            // absolute path
                            true,
                            // isDeep
                            null,
                            // uuids
                            null,
                            // node types
                            true); // noLocal
                }
            }

            // register all virtual catalog data definitions that already exist
            registerDataRoots();
        }
    }

    @Deactivate
    protected synchronized void deactivate(final ComponentContext ctx) throws RepositoryException {
        try {

            // de-register JCR observation
            if (resolver != null) {
                final Session session = resolver.adaptTo(Session.class);
                if (session != null && this.observationEventListeners != null) {
                    for (EventListener eventListener : this.observationEventListeners) {
                        session.getWorkspace()
                            .getObservationManager()
                            .removeEventListener(eventListener);
                    }
                }
            }

            // de-register all virtual catalog resource providers
            for (final ResourceProvider provider : providers.values()) {
                unregisterService(provider);
            }

        } finally {
            if (resolver != null) {
                resolver.close();
                resolver = null;
            }
            providers.clear();
            providerFactories.clear();
            if (!providerRegistrations.isEmpty()) {
                Map<ResourceProvider, ServiceRegistration<?>> map = new IdentityHashMap<>(providerRegistrations);
                for (ResourceProvider provider : map.keySet()) {
                    unregisterService(provider);
                }
            }
        }
    }

    /**
     * Handle resource events to add or remove virtual catalog data registrations.
     */
    public void onEvent(EventIterator events) {
        try {
            // collect all actions to be performed for this event
            final Map<String, Boolean> actions = new HashMap<>();
            boolean nodeAdded = false;
            boolean nodeRemoved = false;
            while (events.hasNext()) {
                final Event event = events.nextEvent();
                final String path = event.getPath();
                final int eventType = event.getType();
                if (eventType == Event.NODE_ADDED) {
                    nodeAdded = true;
                    Session session = resolver.adaptTo(Session.class);
                    final Node node = session.getNode(path);
                    if (node != null && node.isNodeType("sling:Folder") && node.hasProperty(
                        CatalogDataResourceProviderFactory.PROPERTY_FACTORY_ID)) {
                        actions.put(path, true);
                    }
                } else if (eventType == Event.NODE_REMOVED && providers.containsKey(path)) {
                    nodeRemoved = true;
                    actions.put(path, false);
                } else if ((eventType == Event.PROPERTY_CHANGED || eventType == Event.PROPERTY_ADDED || eventType == Event.PROPERTY_REMOVED)
                    && isRelevantPath(path)) {
                    // force re-registering
                    nodeAdded = true;
                    nodeRemoved = true;
                }
            }

            for (Map.Entry<String, Boolean> action : actions.entrySet()) {
                if (action.getValue()) {
                    final Resource rootResource = resolver.getResource(action.getKey());
                    if (rootResource != null) {
                        registerDataRoot(rootResource);
                    }
                } else {
                    final ResourceProvider provider = providers.remove(action.getKey());
                    if (provider != null) {
                        unregisterService(provider);
                    }
                }
            }

            if (nodeAdded && nodeRemoved) {
                // maybe a virtual catalog was moved, re-register all virtual catalogs
                // (existing ones will be skipped)
                registerDataRoots();
            }
        } catch (RepositoryException e) {
            log.error("Unexpected repository exception during event processing.", e);
        }
    }

    private boolean isRelevantPath(String path) {
        return findDataRoots(resolver).stream()
            .map(Resource::getPath)
            .anyMatch(path::startsWith);
    }

    @Reference(
        service = CatalogDataResourceProviderFactory.class,
        bind = "bindFactory",
        unbind = "unbindFactory",
        cardinality = ReferenceCardinality.MULTIPLE,
        policy = ReferencePolicy.DYNAMIC)
    @SuppressWarnings("unused")
    void bindFactory(CatalogDataResourceProviderFactory factory, Map<String, String> properties) {
        log.debug("Binding provider factory {}", factory.getClass().getName());
        providerFactories.put(properties.get(CatalogDataResourceProviderFactory.PROPERTY_FACTORY_SERVICE_ID), factory);
        // the resolver is null before activation and after deactivation
        if (resolver != null) {
            registerDataRoots();
        }
    }

    @SuppressWarnings("unused")
    void unbindFactory(CatalogDataResourceProviderFactory factory, Map<String, String> properties) {
        log.debug("Unbinding provider factory {}", factory.getClass().getName());
        providerFactories.remove(properties.get(CatalogDataResourceProviderFactory.PROPERTY_FACTORY_SERVICE_ID));
        // the resolver is null before activation and after deactivation
        if (resolver != null) {
            registerDataRoots();
        }
    }

    @Override
    public Map<String, CatalogDataResourceProviderFactory<?>> getProviderFactories() {
        Map<String, CatalogDataResourceProviderFactory<?>> ret = new HashMap<>();
        ret.putAll(providerFactories);
        return ret;
    }

    @Override
    public List<Resource> getDataRoots() {
        return dataRoots != null ? dataRoots : findDataRoots(resolver);
    }

    private void registerService(String rootPath, ResourceProvider provider) {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_DESCRIPTION, "Provider of virtual catalog data resources");
        props.put(Constants.SERVICE_VENDOR, "Adobe");
        props.put(PROPERTY_ROOT, new String[] { rootPath });
        ServiceRegistration<ResourceProvider> registration = bundleContext.registerService(ResourceProvider.class, provider, props);
        this.providerRegistrations.put(provider, registration);
        log.info("Registered {}", provider);
    }

    private void unregisterService(ResourceProvider provider) {
        ServiceRegistration registration = this.providerRegistrations.remove(provider);
        if (registration != null) {
            registration.unregister();
            log.info("Unregistered {}", provider);
        }
    }
}
