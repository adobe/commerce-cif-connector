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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Iterator;

import javax.jcr.Node;

import org.apache.commons.collections.map.SingletonMap;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrResourceProvider;
import org.apache.sling.spi.resource.provider.QueryLanguageProvider;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.graphql.core.MagentoProduct;
import com.adobe.cq.commerce.graphql.magento.GraphqlAemContext;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceConfiguration;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceImpl;
import com.adobe.cq.commerce.graphql.magento.MockGraphqlDataServiceConfiguration;
import com.adobe.cq.commerce.graphql.testing.Utils;
import com.day.cq.commons.DownloadResource;
import com.day.cq.commons.inherit.ComponentInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.commons.jcr.JcrConstants;
import com.google.gson.Gson;
import io.wcm.testing.mock.aem.junit.AemContext;

import static com.adobe.cq.commerce.api.CommerceConstants.PN_COMMERCE_TYPE;
import static com.adobe.cq.commerce.graphql.resource.Constants.CATEGORY;
import static com.adobe.cq.commerce.graphql.resource.Constants.CIF_ID;
import static com.adobe.cq.commerce.graphql.resource.Constants.LEAF_CATEGORY;
import static com.adobe.cq.commerce.graphql.resource.Constants.MAGENTO_GRAPHQL_PROVIDER;
import static com.adobe.cq.commerce.graphql.resource.Constants.PRODUCT;
import static com.adobe.cq.commerce.graphql.resource.GraphqlQueryLanguageProvider.VIRTUAL_PRODUCT_QUERY_LANGUAGE;
import static org.apache.sling.api.resource.ResourceResolver.PROPERTY_RESOURCE_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class GraphqlResourceProviderTest {

    private static final String CATALOG_ROOT_PATH = "/var/commerce/products/magento-graphql";

    private static final String SKU = "meskwielt";
    private static final String NAME = "El Gordo Down Jacket";
    private static final String DESCRIPTION = "A nice jacket";
    private static final String URL_KEY = "el-gordo-down-jacket";
    private static final String PRODUCT_PATH = CATALOG_ROOT_PATH + "/men/coats/" + SKU;
    private static final String IMAGE_URL = "https://hostname/media/catalog/product/e/l/el_gordo_purple_2.jpg";

    private static final String MASTER_VARIANT_SKU = "meskwielt-Purple-XS";
    private static final String MASTER_VARIANT_URL_KEY = "meskwielt-purple-xs";
    private static final String MASTER_VARIANT_PATH = CATALOG_ROOT_PATH + "/men/coats/" + SKU + "/" + MASTER_VARIANT_SKU;

    private GraphqlDataServiceImpl dataService;
    private HttpClient httpClient;

    private ResolveContext resolveContext;
    private ResourceResolver resourceResolver;
    private JcrResourceProvider jcrResourceProvider;
    private GraphqlResourceProvider provider;
    private Resource rootResource;
    private InheritanceValueMap rootValueMap;
    private Scheduler scheduler;

    @Rule
    public final AemContext context = GraphqlAemContext.createContext("/context/graphql-resource-provider-context.json", "/var");

    @Before
    public void setUp() throws Exception {
        httpClient = Mockito.mock(HttpClient.class);

        GraphqlClient baseClient = new GraphqlClientImpl();
        Whitebox.setInternalState(baseClient, "gson", new Gson());
        Whitebox.setInternalState(baseClient, "client", httpClient);
        Whitebox.setInternalState(baseClient, "httpMethod", HttpMethod.POST);

        GraphqlDataServiceConfiguration config = new MockGraphqlDataServiceConfiguration();
        dataService = new GraphqlDataServiceImpl();
        Whitebox.setInternalState(dataService, "clients", new SingletonMap("default", baseClient));
        dataService.activate(config);

        resourceResolver = mock(ResourceResolver.class);

        rootResource = Mockito.spy(context.resourceResolver().getResource(CATALOG_ROOT_PATH));
        when(rootResource.getResourceResolver()).thenReturn(resourceResolver);

        resolveContext = mock(ResolveContext.class);
        jcrResourceProvider = mock(JcrResourceProvider.class);

        when(resolveContext.getParentResolveContext()).thenReturn(resolveContext);
        when(resolveContext.getParentResourceProvider()).thenReturn(jcrResourceProvider);
        when(resolveContext.getResourceResolver()).thenReturn(resourceResolver);
        when(jcrResourceProvider.getResource(resolveContext, CATALOG_ROOT_PATH, null, null)).thenReturn(rootResource);

        scheduler = mock(Scheduler.class);
        ScheduleOptions opts = mock(ScheduleOptions.class);
        when(scheduler.schedule(any(), any())).thenReturn(Boolean.FALSE);
        when(scheduler.NOW(Mockito.anyInt(), Mockito.anyLong())).thenReturn(opts);

        rootValueMap = new ComponentInheritanceValueMap(rootResource);
        provider = new GraphqlResourceProvider<>(CATALOG_ROOT_PATH, dataService, scheduler, rootValueMap);

        when(resourceResolver.getResource(any())).then(invocationOnMock -> {
            String path = (String) invocationOnMock.getArguments()[0];
            if (path.endsWith("/.")) {
                path = path.substring(0, path.length() - 2);
            }
            return provider.getResource(resolveContext, path, null, null);
        });
    }

    @Test
    public void testFactoryInitMethod() throws InterruptedException, IOException {
        Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient, HttpStatus.SC_OK);

        ResourceMapper resourceMapper = new ResourceMapper(CATALOG_ROOT_PATH, dataService, scheduler, rootValueMap);
        ResourceMapper spy = spy(resourceMapper);

        final Runnable cacheRefreshJob = new Runnable() {
            public void run() {
                spy.getAbsoluteCategoryPath("4"); // Call any method that calls ResourceMapper.init()
            }
        };

        final Runnable schedulerJob = new Runnable() {
            public void run() {
                spy.refreshCache();
            }
        };

        // One of the "init()" thread will initialize the cache, the other will block,
        // and the scheduler thread will not be able to get the lock

        Thread t1 = new Thread(cacheRefreshJob);
        Thread t2 = new Thread(cacheRefreshJob);
        Thread t3 = new Thread(schedulerJob);

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        // With caching enabled, refreshCache() should be called only once by the 1st thread, and once by the scheduler
        Mockito.verify(spy, Mockito.times(2)).refreshCache();

        // With caching enabled, buildAllCategoryPaths() should be called only once by the 1st thread
        Mockito.verify(spy, Mockito.times(1)).buildAllCategoryPaths();
    }

    @Test
    public void testRootCategory() {
        Resource root = provider.getResource(resolveContext, CATALOG_ROOT_PATH, null, null);

        assertNotNull(root);

        ValueMap valueMap = root.adaptTo(ValueMap.class);

        // check special properties
        assertEquals(MockGraphqlDataServiceConfiguration.ROOT_CATEGORY_ID, valueMap.get(CIF_ID));
        assertEquals(RootCategoryResource.RESOURCE_TYPE, valueMap.get(PROPERTY_RESOURCE_TYPE));
        assertEquals(RootCategoryResource.RESOURCE_TYPE, root.getResourceType());
        assertEquals(CATEGORY, valueMap.get(PN_COMMERCE_TYPE));

        // deep read cifId
        assertEquals(MockGraphqlDataServiceConfiguration.ROOT_CATEGORY_ID, valueMap.get("./" + CIF_ID));

        assertNull(root.adaptTo(Product.class));

        // test adaptTo(Node)
        Node rootNode = mock(Node.class);
        when(rootResource.adaptTo(Node.class)).thenReturn(rootNode);
        assertEquals(rootNode, root.adaptTo(Node.class));

        RootCategoryResource rootCategoryResource = (RootCategoryResource) root;
        assertEquals(rootNode, rootCategoryResource.adaptToNode(new StackTraceElement[0]));

        StackTraceElement[] stackTrace = {
            new StackTraceElement("AClass", "aMethod", "aFile", 1),
            new StackTraceElement("AClass", "aMethod", "aFile", 1),
            new StackTraceElement("AClass", "aMethod", "aFile", 1),
            new StackTraceElement("AClass", "aMethod", "aFile", 1)
        };
        assertEquals(rootNode, rootCategoryResource.adaptToNode(stackTrace));

        String callerClass = "com.day.cq.dam.core.impl.servlet.FolderThumbnailServlet";
        String callerMethod = "getPreviewGenerator";
        stackTrace[2] = new StackTraceElement(callerClass, callerMethod, "", 0);

        assertNull(rootCategoryResource.adaptToNode(stackTrace));
    }

    @Test
    public void testCategoryTree() throws IOException {
        Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient, HttpStatus.SC_OK);

        Resource root = provider.getResource(resolveContext, CATALOG_ROOT_PATH, null, null);
        Iterator<Resource> it = provider.listChildren(resolveContext, root);
        assertTrue(it.hasNext());
        while (it.hasNext()) {
            assertCategoryPaths(provider, it.next(), 1);
        }
    }

    private void assertCategoryPaths(GraphqlResourceProvider provider, Resource category, int depth) {
        assertTrue(category.getPath().substring(CATALOG_ROOT_PATH.length() + 1).split("/").length == depth);
        assertNull(category.adaptTo(Product.class));
        String cifId = category.getValueMap().get(CIF_ID, String.class);
        // deep read cifId
        assertEquals(cifId, category.getValueMap().get("./" + CIF_ID, String.class));
        Boolean isLeaf = category.getValueMap().get(LEAF_CATEGORY, Boolean.class);
        if (Boolean.FALSE.equals(isLeaf)) {
            Iterator<Resource> it = provider.listChildren(resolveContext, category);
            if (it != null) {
                while (it.hasNext()) {
                    Resource child = it.next();
                    // deep read child/cifId
                    String childCifId = child.getValueMap().get(CIF_ID, String.class);
                    assertEquals(childCifId, category.getValueMap().get(child.getName() + "/" + CIF_ID, String.class));
                    assertCategoryPaths(provider, child, depth + 1);
                }
            }
        }
    }

    @Test
    public void testCategoryProductChildren() throws IOException {
        Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient, HttpStatus.SC_OK, "{category(id:4)");
        Utils.setupHttpResponse("magento-graphql-category-products.json", httpClient, HttpStatus.SC_OK, "{category(id:19)");

        Resource coats = provider.getResource(resolveContext, CATALOG_ROOT_PATH + "/men/coats", null, null);
        Iterator<Resource> it = provider.listChildren(resolveContext, coats);
        assertTrue(it.hasNext());
        while (it.hasNext()) {
            final Resource child = it.next();
            assertTrue(child instanceof ProductResource);
            // deep read child/sku
            String childSku = child.getValueMap().get("sku", String.class);
            assertEquals(childSku, coats.getValueMap().get(child.getName() + "/sku", String.class));
        }
    }

    @SuppressWarnings("deprecation")
    private void assertMasterVariant(Product masterVariant) {
        assertEquals(MASTER_VARIANT_SKU, masterVariant.getSKU());
        assertEquals(MASTER_VARIANT_PATH, masterVariant.getPath());
        assertEquals(NAME, masterVariant.getTitle());
        assertEquals(DESCRIPTION, masterVariant.getDescription());
        assertEquals(IMAGE_URL, masterVariant.getImageUrl());
        assertEquals(IMAGE_URL, masterVariant.getThumbnailUrl());
        assertEquals(MASTER_VARIANT_URL_KEY, masterVariant.getProperty("urlKey", String.class));

        Resource masterVariantAsset = masterVariant.getAsset();
        assertEquals(IMAGE_URL, masterVariantAsset.getPath());

        Resource masterVariantImage = masterVariant.getImage();
        assertEquals(MASTER_VARIANT_PATH + "/image", masterVariantImage.getPath());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testConfigurableProductResource() throws IOException, CommerceException {
        Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient, HttpStatus.SC_OK, "{category");
        Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK, "{product");

        Resource resource = provider.getResource(resolveContext, PRODUCT_PATH, null, null);
        assertTrue(resource instanceof ProductResource);
        assertTrue(MagentoProduct.isAProductOrVariant(resource));
        assertEquals(SKU, resource.getValueMap().get("sku", String.class));
        Date lastModified = resource.getValueMap().get(JcrConstants.JCR_LASTMODIFIED, Date.class);
        assertTrue(lastModified != null);

        Product product = resource.adaptTo(Product.class);
        assertEquals(product, product.getBaseProduct());
        assertEquals(product, product.getPIMProduct());
        assertEquals(SKU, product.getSKU());
        assertEquals(NAME, product.getTitle());
        assertEquals(DESCRIPTION, product.getDescription());
        assertEquals(IMAGE_URL, product.getImageUrl());
        assertEquals(IMAGE_URL, product.getThumbnailUrl());
        assertEquals(IMAGE_URL, product.getThumbnailUrl(123));
        assertEquals(IMAGE_URL, product.getThumbnailUrl("selector"));
        assertNull(product.getThumbnail());
        assertEquals(URL_KEY, product.getProperty("urlKey", String.class));
        assertNull(product.getProperty("whatever", String.class));

        assertEquals(IMAGE_URL, product.getAsset().getPath());
        assertEquals(IMAGE_URL, product.getAssets().get(0).getPath());

        assertEquals(PRODUCT_PATH + "/image", product.getImage().getPath());
        assertEquals(PRODUCT_PATH + "/image", product.getImages().get(0).getPath());
        assertNull(product.getImagePath());

        // We do not extract variant axes
        assertFalse(product.getVariantAxes().hasNext());
        assertFalse(product.axisIsVariant("whatever"));

        // Test master variant when fetched via Product API
        Product masterVariant = product.getVariants().next();
        assertMasterVariant(masterVariant);

        Iterator<Resource> it = provider.listChildren(resolveContext, resource);

        // First child is the image
        Resource imageResource = it.next();
        assertEquals(PRODUCT_PATH + "/image", imageResource.getPath());
        assertEquals(IMAGE_URL, imageResource.getValueMap().get(DownloadResource.PN_REFERENCE, String.class));

        // Test master variant when fetched via Resource API
        Resource firstVariant = it.next();
        assertEquals(MASTER_VARIANT_SKU, firstVariant.getValueMap().get("sku", String.class));

        // Test deep read firstVariantName/sku
        assertEquals(MASTER_VARIANT_SKU, resource.getValueMap().get(firstVariant.getName() + "/sku", String.class));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSimpleProductResource() throws IOException, CommerceException {
        Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient, HttpStatus.SC_OK, "{category");
        Utils.setupHttpResponse("magento-graphql-simple-product.json", httpClient, HttpStatus.SC_OK, "{product");

        String productPath = CATALOG_ROOT_PATH + "/men/coats/24-MB01";

        Resource resource = provider.getResource(resolveContext, productPath, null, null);
        assertTrue(resource instanceof ProductResource);
        assertTrue(MagentoProduct.isAProductOrVariant(resource));
        assertEquals("24-MB01", resource.getValueMap().get("sku", String.class));
        Date lastModified = resource.getValueMap().get(JcrConstants.JCR_LASTMODIFIED, Date.class);
        assertTrue(lastModified != null);

        Product product = resource.adaptTo(Product.class);
        assertEquals(product, product.getBaseProduct());
        assertEquals(product, product.getPIMProduct());
        assertEquals("24-MB01", product.getSKU());
        assertEquals("Joust Duffle Bag", product.getTitle());
        assertEquals("The sporty Joust Duffle Bag can't be beat", product.getDescription());

        String imageUrl = "http://hostname/pub/media/catalog/product/mb01-blue-0.jpg";
        assertEquals(imageUrl, product.getImageUrl());
        assertEquals(imageUrl, product.getThumbnailUrl());
        assertEquals(imageUrl, product.getThumbnailUrl(123));
        assertEquals(imageUrl, product.getThumbnailUrl("selector"));
        assertNull(product.getThumbnail());
        assertEquals("joust-duffle-bag", product.getProperty("urlKey", String.class));
        assertNull(product.getProperty("whatever", String.class));

        assertEquals(imageUrl, product.getAsset().getPath());
        assertEquals(imageUrl, product.getAssets().get(0).getPath());

        assertEquals(productPath + "/image", product.getImage().getPath());
        assertEquals(productPath + "/image", product.getImages().get(0).getPath());
        assertNull(product.getImagePath());

        // We do not extract variant axes
        assertFalse(product.getVariantAxes().hasNext());
        assertFalse(product.axisIsVariant("whatever"));

        // A simple product doesn't have any variant
        assertFalse(product.getVariants().hasNext());

        Iterator<Resource> it = provider.listChildren(resolveContext, resource);

        // First child is the image
        Resource imageResource = it.next();
        assertEquals(productPath + "/image", imageResource.getPath());
        assertEquals(imageUrl, imageResource.getValueMap().get(DownloadResource.PN_REFERENCE, String.class));
        // Deep read image/fileReference of product
        assertEquals(imageUrl, resource.getValueMap().get("image/" + DownloadResource.PN_REFERENCE, String.class));

        // A simple product doesn't have any variant
        assertFalse(it.hasNext());
    }

    @Test
    public void testMasterVariantResource() throws IOException, CommerceException {
        Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient, HttpStatus.SC_OK, "{category");
        Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK, "{product");

        Resource resource = provider.getResource(resolveContext, MASTER_VARIANT_PATH, null, null);
        assertTrue(resource instanceof ProductResource);
        assertEquals(MASTER_VARIANT_SKU, resource.getValueMap().get("sku", String.class));

        Product product = resource.adaptTo(Product.class);
        assertMasterVariant(product);

        // deep read sku
        assertEquals(MASTER_VARIANT_SKU, resource.getValueMap().get("./sku", String.class));
    }

    @Test
    public void testImageResource() throws IOException, CommerceException {
        Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient, HttpStatus.SC_OK, "{category");
        Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK, "{product");

        Resource resource = provider.getResource(resolveContext, PRODUCT_PATH + "/image", null, null);
        assertTrue(resource instanceof SyntheticImageResource);
        assertEquals(IMAGE_URL, resource.getValueMap().get(DownloadResource.PN_REFERENCE, String.class));

        // deep read fileReference
        assertEquals(IMAGE_URL, resource.getValueMap().get("./" + DownloadResource.PN_REFERENCE, String.class));
    }

    @Test
    public void testQueryLanguageProvider() throws IOException {
        Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient, HttpStatus.SC_OK, "{category");
        Utils.setupHttpResponse("magento-graphql-products-search.json", httpClient, HttpStatus.SC_OK, "{product");

        // The search request coming from com.adobe.cq.commerce.impl.omnisearch.ProductsOmniSearchHandler is serialized in JSON
        String jsonRequest = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream(
            "commerce-products-omni-search-request.json"), StandardCharsets.UTF_8);

        QueryLanguageProvider queryLanguageProvider = provider.getQueryLanguageProvider();
        assertTrue(ArrayUtils.contains(queryLanguageProvider.getSupportedLanguages(null), VIRTUAL_PRODUCT_QUERY_LANGUAGE));

        Iterator<Resource> resources = queryLanguageProvider.findResources(resolveContext, jsonRequest, VIRTUAL_PRODUCT_QUERY_LANGUAGE);

        // We only check the first "meskwielt" product
        Resource resource = resources.next();
        assertEquals(PRODUCT_PATH, resource.getPath());

        ValueMap valueMap = resource.getValueMap();
        assertEquals(SKU, valueMap.get("sku", String.class));
        assertEquals(PRODUCT, valueMap.get(CommerceConstants.PN_COMMERCE_TYPE, String.class));
        assertEquals(MAGENTO_GRAPHQL_PROVIDER, valueMap.get(CommerceConstants.PN_COMMERCE_PROVIDER, String.class));
    }
}
