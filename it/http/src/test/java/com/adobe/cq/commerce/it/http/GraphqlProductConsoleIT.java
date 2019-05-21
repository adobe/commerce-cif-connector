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

package com.adobe.cq.commerce.it.http;

import org.apache.http.HttpEntity;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.adobe.cq.testing.mockserver.RequestResponseRule;

import static com.adobe.cq.testing.mockserver.MockRequest.request;
import static com.adobe.cq.testing.mockserver.MockResponse.response;
import static com.adobe.cq.testing.mockserver.RequestResponseRule.rule;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_OK;

public class GraphqlProductConsoleIT extends CommerceTestBase {

    private static final String CORAL_COLUMN_FORMAT_EQUALS = "coral-columnview-item[data-foundation-collection-item-id=%s]";
    private static final String CORAL_COLUMN_FORMAT_STARTS_WITH = "coral-columnview-item[data-foundation-collection-item-id^=%s]";
    
    private static final String JCR_PRODUCT_ROOT = "we-retail";
    private static final String JCR_BASE_PATH = "/var/commerce/products/graphql";
    private static final String FOLDER_PROPERTIES = "/mnt/overlay/commerce/gui/content/products/folderproperties.html";

    @BeforeClass
    public static void setup() throws ClientException, InterruptedException {
        // Create product binding
        HttpEntity params = FormEntityBuilder.create()
            .addParameter("_charset_", "utf-8")
            .addParameter("./jcr:title", "graphql")
            .addParameter(":name", "graphql")
            .addParameter("./cq:catalogDataResourceProviderFactory", "magento-graphql")
            .addParameter("./cq:catalogIdentifier", "default")
            .addParameter("./jcr:primaryType", "sling:Folder")
            .build();
        cAdminAuthor.doPost("/var/commerce/products/", params, SC_CREATED);
        Thread.sleep(2000);
    }

    @Before
    public void resetMockServer() throws ClientException {
        mockServerRule.reset();
    }

    @AfterClass
    public static void cleanup() throws ClientException {
        cAdminAuthor.deletePage(new String[] { JCR_BASE_PATH }, true, false, SC_OK);
    }

    public static RequestResponseRule.Builder CATALOG_RULE = rule()
            .on(request()
                    .withMethod(HttpMethod.POST)
                    .withRequestURI("/graphql")
                    .withBody(s -> s.startsWith("{\"query\":\"{category(id:4)")))
            .send(response()
                    .withStatus(HttpStatus.OK_200)
                    .withContentFromResource("/com/adobe/cq/commerce/it/http/magento-graphql-category-tree-2.3.0.json")
                    .withContentType("application/json; charset=utf-8"));

    public static RequestResponseRule.Builder SEARCH_PRODUCTS_IN_CATEGORY = rule()
            .on(request()
                    .withMethod(HttpMethod.POST)
                    .withRequestURI("/graphql")
                    .withBody(s -> s.startsWith("{\"query\":\"{category(id:19)")))
            .send(response()
                    .withStatus(HttpStatus.OK_200)
                    .withContentFromResource("/com/adobe/cq/commerce/it/http/magento-graphql-category-products.json")
                    .withContentType("application/json; charset=utf-8"));

    public static RequestResponseRule.Builder SEARCH_PRODUCT_BY_SKU = rule()
            .on(request()
                    .withMethod(HttpMethod.POST)
                    .withRequestURI("/graphql")
                    .withBody(s -> s.startsWith("{\"query\":\"{products(filter:{sku:{eq:\\\"meskwielt\\\"}})")))
            .send(response()
                    .withStatus(HttpStatus.OK_200)
                    .withContentFromResource("/com/adobe/cq/commerce/it/http/magento-graphql-product.json")
                    .withContentType("application/json; charset=utf-8"));

    @Test
    public void testCategoryRoot() throws Exception {

        // Prepare
        mockServerRule.add(CATALOG_RULE.build());

        // Perform
        SlingHttpResponse response = cAuthorAuthor.doGet("/libs/commerce/gui/content/products.html" + JCR_BASE_PATH, null, NO_CACHE_HEADERS,
            SC_OK);

        // Verify
        mockServerRule.verify();
        Document doc = Jsoup.parse(response.getContent());

        // Check existence of root categories
        Assert.assertTrue(doc.select(String.format(CORAL_COLUMN_FORMAT_EQUALS, JCR_BASE_PATH + "/equipment")).size() > 0);
        Assert.assertTrue(doc.select(String.format(CORAL_COLUMN_FORMAT_EQUALS, JCR_BASE_PATH + "/men")).size() > 0);
        Assert.assertTrue(doc.select(String.format(CORAL_COLUMN_FORMAT_EQUALS, JCR_BASE_PATH + "/women")).size() > 0);

        // Check that child categories are not displayed
        Assert.assertTrue(doc.select(String.format(CORAL_COLUMN_FORMAT_EQUALS, JCR_BASE_PATH + "/men/pants")).size() == 0);
    }

    @Test
    public void testProductsInCategory() throws Exception {

        // Prepare
        mockServerRule.add(CATALOG_RULE.build());
        mockServerRule.add(SEARCH_PRODUCTS_IN_CATEGORY.build());

        // Perform
        SlingHttpResponse response = cAuthorAuthor.doGet("/aem/products.html" + JCR_BASE_PATH + "/men/coats", null, NO_CACHE_HEADERS,
            SC_OK);

        // Verify
        mockServerRule.verify();
        Document doc = Jsoup.parse(response.getContent());

        // Check existence of one child product
        Assert.assertTrue(doc.select(String.format(CORAL_COLUMN_FORMAT_EQUALS, JCR_BASE_PATH + "/men/coats/meotwibrt")).size() > 0);

        // Check that products from other categories are not displayed
        Assert.assertTrue(doc.select(String.format(CORAL_COLUMN_FORMAT_EQUALS, JCR_BASE_PATH + "/men/footwear/meotwisus")).size() == 0);
    }
    
    @Test
    public void testProductDetails() throws Exception {

        // Prepare
        mockServerRule.add(CATALOG_RULE.build());
        mockServerRule.add(SEARCH_PRODUCT_BY_SKU.build());

        // Perform
        SlingHttpResponse response = cAuthorAuthor.doGet("/aem/products.html" + JCR_BASE_PATH + "/men/coats/meskwielt", null, NO_CACHE_HEADERS,
            SC_OK);

        // Verify
        mockServerRule.verify();
        Document doc = Jsoup.parse(response.getContent());
        
        // Check variants
        Assert.assertEquals(15, doc.select(String.format(CORAL_COLUMN_FORMAT_STARTS_WITH, JCR_BASE_PATH + "/men/coats/meskwielt/meskwielt-")).size());
    }
    
    @Test
    public void testCcifFolderProperties() throws Exception {
        mockServerRule.add(CATALOG_RULE.build());
        SlingHttpResponse response = cAuthorAuthor.doGet(FOLDER_PROPERTIES + JCR_BASE_PATH + "/men", SC_OK);

        mockServerRule.verify();
        Document doc = Jsoup.parse(response.getContent());

        Assert.assertEquals("Close", doc.select("a[id=shell-propertiespage-closeactivator]").text());
        Assert.assertTrue(doc.select("input[name=./jcr:title]").hasAttr("disabled"));
        Assert.assertTrue(doc.select("input[name=./jcr:primaryType]").hasAttr("disabled"));
    }

    @Test
    public void testJCRFolderProperties() throws Exception {
        mockServerRule.add(CATALOG_RULE.build());
        SlingHttpResponse response = cAuthorAuthor.doGet(FOLDER_PROPERTIES + "/var/commerce/products/" + JCR_PRODUCT_ROOT, SC_OK);

        mockServerRule.verify();
        Document doc = Jsoup.parse(response.getContent());

        Assert.assertEquals("Cancel", doc.select("a[id=shell-propertiespage-closeactivator]").text());
        Assert.assertFalse(doc.select("input[name=./jcr:title]").hasAttr("disabled"));
        Assert.assertFalse(doc.select("input[name=./jcr:primaryType]").hasAttr("disabled"));
    }

    @Test
    public void testCategoryFolderProperties() throws Exception {

        // Prepare
        mockServerRule.add(CATALOG_RULE.build());

        // Perform
        SlingHttpResponse response = cAuthorAuthor.doGet(FOLDER_PROPERTIES + JCR_BASE_PATH + "/women/footwear", null, NO_CACHE_HEADERS,
            SC_OK);

        // Verify
        mockServerRule.verify();
        Document doc = Jsoup.parse(response.getContent());

        // Verify property fields
        // TODO: Behavior not correct, see CQ-4222819
        Assert.assertNotEquals("Footwear", doc.select("input[name=jcr:title]").val());
        Assert.assertFalse(doc.select("input[name=jcr:title]").hasAttr("disabled"));
    }

}
