/*******************************************************************************
 *
 * Copyright 2019 Adobe. All rights reserved. This file is licensed to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.adobe.cq.commerce.graphql.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Iterator;
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
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.CommerceException;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.graphql.client.GraphqlClient;
import com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceConfiguration;
import com.adobe.cq.commerce.graphql.magento.GraphqlDataServiceImpl;
import com.adobe.cq.commerce.graphql.magento.MockGraphqlDataServiceConfiguration;
import com.adobe.cq.commerce.graphql.testing.Utils;
import com.day.cq.commons.DownloadResource;
import com.day.cq.commons.jcr.JcrConstants;
import com.google.gson.Gson;
import static com.adobe.cq.commerce.api.CommerceConstants.PN_COMMERCE_TYPE;
import static com.adobe.cq.commerce.graphql.resource.Constants.CATEGORY;
import static com.adobe.cq.commerce.graphql.resource.Constants.CIF_ID;
import static com.adobe.cq.commerce.graphql.resource.Constants.LEAF_CATEGORY;
import static com.adobe.cq.commerce.graphql.resource.Constants.MAGENTO_GRAPHQL_PROVIDER;
import static com.adobe.cq.commerce.graphql.resource.Constants.PRODUCT;
import static com.adobe.cq.commerce.graphql.resource.GraphqlQueryLanguageProvider.VIRTUAL_PRODUCT_QUERY_LANGUAGE;
import static org.apache.sling.api.resource.ResourceResolver.PROPERTY_RESOURCE_TYPE;
import static org.apache.sling.jcr.resource.api.JcrResourceConstants.NT_SLING_FOLDER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked"})
public class GraphqlResourceProviderTest {

  private static final String CATALOG_ROOT_PATH = "/var/commerce/products/magento-graphql";

  private static final String SKU = "meskwielt";
  private static final String NAME = "El Gordo Down Jacket";
  private static final String DESCRIPTION = "A nice jacket";
  private static final String URL_KEY = "el-gordo-down-jacket";
  private static final String PRODUCT_PATH = CATALOG_ROOT_PATH + "/men/coats/" + SKU;
  private static final String IMAGE_URL =
      "https://hostname/media/catalog/product/e/l/el_gordo_purple_2.jpg";

  private static final String MASTER_VARIANT_SKU = "meskwielt-Purple-XS";
  private static final String MASTER_VARIANT_URL_KEY = "meskwielt-purple-xs";
  private static final String MASTER_VARIANT_PATH =
      CATALOG_ROOT_PATH + "/men/coats/" + SKU + "/" + MASTER_VARIANT_SKU;

  private GraphqlDataServiceImpl dataService;
  private HttpClient httpClient;

  private ResolveContext resolveContext;
  private JcrResourceProvider jcrResourceProvider;
  private Resource rootResource;
  private ValueMap rootValueMap;
  private Scheduler scheduler;

  @Before
  public void setUp() throws Exception {
    httpClient = Mockito.mock(HttpClient.class);

    GraphqlClient baseClient = new GraphqlClientImpl();
    Whitebox.setInternalState(baseClient, "gson", new Gson());
    Whitebox.setInternalState(baseClient, "client", httpClient);

    GraphqlDataServiceConfiguration config = new MockGraphqlDataServiceConfiguration();
    dataService = new GraphqlDataServiceImpl();
    Whitebox.setInternalState(dataService, "clients", new SingletonMap("default", baseClient));
    dataService.activate(config);

    Resource resource = mock(Resource.class);
    ResourceResolver resourceResolver = mock(ResourceResolver.class);
    when(resource.getResourceResolver()).thenReturn(resourceResolver);

    resolveContext = mock(ResolveContext.class);
    jcrResourceProvider = mock(JcrResourceProvider.class);
    rootResource = mock(Resource.class);
    rootValueMap = mock(ValueMap.class);
    when(resolveContext.getParentResolveContext()).thenReturn(resolveContext);
    when(resolveContext.getParentResourceProvider()).thenReturn(jcrResourceProvider);
    when(resolveContext.getResourceResolver()).thenReturn(resourceResolver);
    when(jcrResourceProvider.getResource(resolveContext, CATALOG_ROOT_PATH, null, null))
        .thenReturn(rootResource);
    when(rootResource.getPath()).thenReturn(CATALOG_ROOT_PATH);
    when(rootResource.getValueMap()).thenReturn(rootValueMap);

    scheduler = mock(Scheduler.class);
    ScheduleOptions opts = mock(ScheduleOptions.class);
    when(scheduler.schedule(any(), any())).thenReturn(Boolean.FALSE);
    when(scheduler.NOW(Mockito.anyInt(), Mockito.anyLong())).thenReturn(opts);
  }

  @Test
  public void testFactoryInitMethod() throws InterruptedException, IOException {
    Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient,
        HttpStatus.SC_OK);

    ResourceMapper resourceMapper = new ResourceMapper(CATALOG_ROOT_PATH, dataService, scheduler);
    ResourceMapper spy = spy(resourceMapper);

    final Runnable cacheRefreshJob = new Runnable() {
      public void run() {
        spy.getAbsoluteCategoryPath("4"); // Call any method that calls ResourceMapper.init()
      }
    };

    Thread t1 = new Thread(cacheRefreshJob);
    Thread t2 = new Thread(cacheRefreshJob);
    t1.start();
    t2.start();
    t1.join();
    t2.join();

    // With caching enabled, refreshCache() should be called only once by the 1st thread
    Mockito.verify(spy, Mockito.times(1)).refreshCache();
  }

  @Test
  public void testRootCategory() {
    GraphqlResourceProvider provider =
        new GraphqlResourceProvider<>(CATALOG_ROOT_PATH, dataService, scheduler);
    Resource root = provider.getResource(resolveContext, CATALOG_ROOT_PATH, null, null);

    assertNotNull(root);

    ValueMap valueMap = root.adaptTo(ValueMap.class);

    // check special properties
    assertEquals(MockGraphqlDataServiceConfiguration.ROOT_CATEGORY_ID, valueMap.get(CIF_ID));
    assertEquals(CATEGORY, valueMap.get(PN_COMMERCE_TYPE));
    assertEquals(NT_SLING_FOLDER, valueMap.get(PROPERTY_RESOURCE_TYPE));
  }

  @Test
  public void testCategoryTree() throws IOException {
    Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient,
        HttpStatus.SC_OK);

    GraphqlResourceProvider provider =
        new GraphqlResourceProvider<>(CATALOG_ROOT_PATH, dataService, scheduler);
    Resource root = provider.getResource(resolveContext, CATALOG_ROOT_PATH, null, null);
    Iterator<Resource> it = provider.listChildren(resolveContext, root);
    assertTrue(it.hasNext());
    while (it.hasNext()) {
      assertCategoryPaths(provider, it.next(), 1);
    }
  }

  private void assertCategoryPaths(GraphqlResourceProvider provider, Resource category, int depth) {
    assertTrue(
        category.getPath().substring(CATALOG_ROOT_PATH.length() + 1).split("/").length == depth);
    Boolean isLeaf = category.getValueMap().get(LEAF_CATEGORY, Boolean.class);
    if (Boolean.FALSE.equals(isLeaf)) {
      Iterator<Resource> it = provider.listChildren(resolveContext, category);
      if (it != null) {
        while (it.hasNext()) {
          assertCategoryPaths(provider, it.next(), depth + 1);
        }
      }
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
  public void testProductResource() throws IOException, CommerceException {
    Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient,
        HttpStatus.SC_OK, "{category");
    Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK,
        "{product");

    GraphqlResourceProvider provider =
        new GraphqlResourceProvider<>(CATALOG_ROOT_PATH, dataService, scheduler);
    Resource resource = provider.getResource(resolveContext, PRODUCT_PATH, null, null);
    assertTrue(resource instanceof ProductResource);
    assertEquals(SKU, resource.getValueMap().get("sku", String.class));
    Date lastModified = resource.getValueMap().get(JcrConstants.JCR_LASTMODIFIED, Date.class);
    assertTrue(lastModified != null);

    Product product = resource.adaptTo(Product.class);
    assertEquals(SKU, product.getSKU());
    assertEquals(NAME, product.getTitle());
    assertEquals(DESCRIPTION, product.getDescription());
    assertEquals(IMAGE_URL, product.getImageUrl());
    assertEquals(IMAGE_URL, product.getThumbnailUrl());
    assertEquals(URL_KEY, product.getProperty("urlKey", String.class));

    Resource asset = product.getAsset();
    assertEquals(IMAGE_URL, asset.getPath());

    Resource image = product.getImage();
    assertEquals(PRODUCT_PATH + "/image", image.getPath());

    // Test master variant when fetched via Product API
    Product masterVariant = product.getVariants().next();
    assertMasterVariant(masterVariant);

    Iterator<Resource> it = provider.listChildren(resolveContext, resource);

    // First child is the image
    Resource imageResource = it.next();
    assertEquals(PRODUCT_PATH + "/image", imageResource.getPath());
    assertEquals(IMAGE_URL,
        imageResource.getValueMap().get(DownloadResource.PN_REFERENCE, String.class));

    // Test master variant when fetched via Resource API
    Resource firstVariant = it.next();
    assertEquals(MASTER_VARIANT_SKU, firstVariant.getValueMap().get("sku", String.class));
  }

  @Test
  public void testMasterVariantResource() throws IOException, CommerceException {
    Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient,
        HttpStatus.SC_OK, "{category");
    Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK,
        "{product");

    GraphqlResourceProvider provider =
        new GraphqlResourceProvider<>(CATALOG_ROOT_PATH, dataService, scheduler);
    Resource resource = provider.getResource(resolveContext, MASTER_VARIANT_PATH, null, null);
    assertTrue(resource instanceof ProductResource);
    assertEquals(MASTER_VARIANT_SKU, resource.getValueMap().get("sku", String.class));

    Product product = resource.adaptTo(Product.class);
    assertMasterVariant(product);
  }

  @Test
  public void testImageResource() throws IOException, CommerceException {
    Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient,
        HttpStatus.SC_OK, "{category");
    Utils.setupHttpResponse("magento-graphql-product.json", httpClient, HttpStatus.SC_OK,
        "{product");

    GraphqlResourceProvider provider =
        new GraphqlResourceProvider<>(CATALOG_ROOT_PATH, dataService, scheduler);
    Resource resource = provider.getResource(resolveContext, PRODUCT_PATH + "/image", null, null);
    assertTrue(resource instanceof SyntheticImageResource);
    assertEquals(IMAGE_URL,
        resource.getValueMap().get(DownloadResource.PN_REFERENCE, String.class));
  }

  @Test
  public void testQueryLanguageProvider() throws IOException {
    Utils.setupHttpResponse("magento-graphql-category-tree-2.3.1.json", httpClient,
        HttpStatus.SC_OK, "{category");
    Utils.setupHttpResponse("magento-graphql-products-search.json", httpClient, HttpStatus.SC_OK,
        "{product");

    // The search request coming from
    // com.adobe.cq.commerce.impl.omnisearch.ProductsOmniSearchHandler is serialized in JSON
    String jsonRequest = IOUtils.toString(this.getClass().getClassLoader()
        .getResourceAsStream("commerce-products-omni-search-request.json"), StandardCharsets.UTF_8);

    GraphqlResourceProvider provider =
        new GraphqlResourceProvider<>(CATALOG_ROOT_PATH, dataService, scheduler);
    QueryLanguageProvider queryLanguageProvider = provider.getQueryLanguageProvider();
    assertTrue(ArrayUtils.contains(queryLanguageProvider.getSupportedLanguages(null),
        VIRTUAL_PRODUCT_QUERY_LANGUAGE));

    Iterator<Resource> resources = queryLanguageProvider.findResources(resolveContext, jsonRequest,
        VIRTUAL_PRODUCT_QUERY_LANGUAGE);

    // We only check the first "meskwielt" product
    Resource resource = resources.next();
    assertEquals(PRODUCT_PATH, resource.getPath());

    ValueMap valueMap = resource.getValueMap();
    assertEquals(SKU, valueMap.get("sku", String.class));
    assertEquals(PRODUCT, valueMap.get(CommerceConstants.PN_COMMERCE_TYPE, String.class));
    assertEquals(MAGENTO_GRAPHQL_PROVIDER,
        valueMap.get(CommerceConstants.PN_COMMERCE_PROVIDER, String.class));
  }
}
