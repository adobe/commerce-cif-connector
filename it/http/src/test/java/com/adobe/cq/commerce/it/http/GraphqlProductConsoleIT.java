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

import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.apache.sling.testing.clients.util.URLParameterBuilder;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.adobe.cq.testing.mockserver.RequestResponseRule;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
            .addParameter("./cq:magentoStore", "default")
            .addParameter("./magentoRootCategoryId", "4")
            .addParameter("./jcr:language", "en_us")
            .addParameter("./jcr:primaryType", "sling:Folder")
            .build();
        cAdminAuthor.doPost("/var/commerce/products/", params, SC_CREATED);
        Thread.sleep(2000);
    }

    @Before
    public void resetMockServer() {
        mockServerRule.reset();
    }

    @AfterClass
    public static void cleanup() throws ClientException, InterruptedException {
        Thread.sleep(5000);
        cAdminAuthor.deletePage(new String[] { JCR_BASE_PATH }, true, false, SC_OK);
    }

    public static RequestResponseRule.Builder CATEGORY_ROOT_RULE = rule()
        .on(request()
            .withMethod(HttpMethod.POST)
            .withRequestURI("/graphql")
            .withBody(s -> s.startsWith("{\"query\":\"{categoryList(filters:{ids:{eq:\\\"4\\\"")))
        .send(response()
            .withStatus(HttpStatus.OK_200)
            .withContentFromResource("/com/adobe/cq/commerce/it/http/magento-graphql-categorylist-root.json")
            .withContentType("application/json; charset=utf-8"));

    public static RequestResponseRule.Builder CATEGORY_MEN_RULE = rule()
        .on(request()
            .withMethod(HttpMethod.POST)
            .withRequestURI("/graphql")
            .withBody(s -> s.startsWith("{\"query\":\"{categoryList(filters:{url_key:{eq:\\\"men\\\"")))
        .send(response()
            .withStatus(HttpStatus.OK_200)
            .withContentFromResource("/com/adobe/cq/commerce/it/http/magento-graphql-categorylist-men.json")
            .withContentType("application/json; charset=utf-8"));

    public static RequestResponseRule.Builder CATEGORY_COATS_RULE = rule()
        .on(request()
            .withMethod(HttpMethod.POST)
            .withRequestURI("/graphql")
            .withBody(s -> s.startsWith("{\"query\":\"{categoryList(filters:{url_key:{eq:\\\"coats\\\"")))
        .send(response()
            .withStatus(HttpStatus.OK_200)
            .withContentFromResource("/com/adobe/cq/commerce/it/http/magento-graphql-categorylist-coats.json")
            .withContentType("application/json; charset=utf-8"));

    public static RequestResponseRule.Builder CATEGORY_MESKWIELT_EMPTY_RULE = rule()
        .on(request()
            .withMethod(HttpMethod.POST)
            .withRequestURI("/graphql")
            .withBody(s -> s.startsWith("{\"query\":\"{categoryList(filters:{url_key:{eq:\\\"meskwielt\\\"")))
        .send(response()
            .withStatus(HttpStatus.OK_200)
            .withContentFromResource("/com/adobe/cq/commerce/it/http/magento-graphql-categorylist-men.json")
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

    public static RequestResponseRule.Builder SEARCH_PRODUCTS_FULL_TEXT = rule()
        .on(request()
            .withMethod(HttpMethod.POST)
            .withRequestURI("/graphql")
            .withBody(s -> s.startsWith("{\"query\":\"{products(search:\\\"coats\\\"")))
        .send(response()
            .withStatus(HttpStatus.OK_200)
            .withContentFromResource("/com/adobe/cq/commerce/it/http/magento-graphql-product.json")
            .withContentType("application/json; charset=utf-8"));

    @Test
    public void testCategoryRoot() throws Exception {

        // Prepare
        mockServerRule.add(CATEGORY_ROOT_RULE.build());

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
        Assert.assertEquals(0, doc.select(String.format(CORAL_COLUMN_FORMAT_EQUALS, JCR_BASE_PATH + "/men/pants")).size());
    }

    @Test
    public void testProductsInCategory() throws Exception {

        // Prepare
        mockServerRule.add(CATEGORY_COATS_RULE.build());
        mockServerRule.add(CATEGORY_MEN_RULE.build());
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
        Assert.assertEquals(0, doc.select(String.format(CORAL_COLUMN_FORMAT_EQUALS, JCR_BASE_PATH + "/men/footwear/meotwisus")).size());
    }

    @Test
    public void testProductDetails() throws Exception {

        // Prepare
        mockServerRule.add(CATEGORY_MESKWIELT_EMPTY_RULE.build());
        mockServerRule.add(CATEGORY_COATS_RULE.build());
        mockServerRule.add(CATEGORY_MEN_RULE.build());
        mockServerRule.add(SEARCH_PRODUCT_BY_SKU.build());

        // Perform
        SlingHttpResponse response = cAuthorAuthor.doGet("/aem/products.html" + JCR_BASE_PATH + "/men/coats/meskwielt", null,
            NO_CACHE_HEADERS,
            SC_OK);

        // Verify
        mockServerRule.verify();
        Document doc = Jsoup.parse(response.getContent());

        // Check variants
        Assert.assertEquals(15, doc.select(String.format(CORAL_COLUMN_FORMAT_STARTS_WITH, JCR_BASE_PATH
            + "/men/coats/meskwielt/meskwielt-")).size());
    }

    @Test
    public void testProductPropertiesPage() throws ClientException {

        // Prepare
        mockServerRule.add(CATEGORY_MESKWIELT_EMPTY_RULE.build());
        mockServerRule.add(CATEGORY_COATS_RULE.build());
        mockServerRule.add(SEARCH_PRODUCT_BY_SKU.build());

        // 1. Update the scaffolding to point to the test catalog
        String scaffoldingPath = "/apps/commerce/scaffolding/product/jcr:content";
        UrlEncodedFormEntity postParams = FormEntityBuilder.create().addParameter("cq:targetPath", JCR_BASE_PATH).build();
        cAdminAuthor.doPost(scaffoldingPath, postParams, 200);

        // 2. Request a product properties page
        String productPropertiesUrl = "/mnt/overlay/commerce/gui/content/products/properties.html";
        List<NameValuePair> params = URLParameterBuilder.create()
            .add("item", JCR_BASE_PATH + "/men/coats/meskwielt")
            .getList();
        SlingHttpResponse response = cAuthorAuthor.doGet(productPropertiesUrl, params, 200);

        // 3. Check the fields
        Document doc = Jsoup.parse(response.getContent());

        Assert.assertEquals("The Title field is preset", 1, doc.select("input[name=jcr:title]").size());
        Assert.assertEquals("The Title has the correct value", "El Gordo Down Jacket", doc.select("input[name=jcr:title]").val());

        Assert.assertEquals("The Price field is correct", 1, doc.select("input[name=formattedPrice]").size());
        Assert.assertEquals("The Price field has the correct value", "USD 119.0", doc.select("input[name=formattedPrice]").val());

        Assert.assertEquals("The SKU field is correct", 1, doc.select("input[name=sku]").size());
        Assert.assertEquals("The SKU field has the correct value", "meskwielt", doc.select("input[name=sku]").val());
    }

    @Test
    public void testCifFolderProperties() throws Exception {
        mockServerRule.add(CATEGORY_MEN_RULE.build());
        SlingHttpResponse response = cAuthorAuthor.doGet(FOLDER_PROPERTIES + JCR_BASE_PATH + "/men", SC_OK);

        mockServerRule.verify();
        Document doc = Jsoup.parse(response.getContent());

        Assert.assertEquals("Close", doc.select("a[id=shell-propertiespage-closeactivator]").text());
        Elements title = doc.select("input[name=jcr:title]");
        Assert.assertTrue(title.hasAttr("disabled"));
        Assert.assertEquals("Men", title.val());

        Elements categoryId = doc.select("input[name=cifId]");
        Assert.assertTrue(title.hasAttr("disabled"));
        Assert.assertEquals("15", categoryId.val());
    }

    @Test
    public void testJCRFolderProperties() throws Exception {
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
        mockServerRule.add(CATEGORY_COATS_RULE.build());

        // Perform
        SlingHttpResponse response = cAuthorAuthor.doGet(FOLDER_PROPERTIES + JCR_BASE_PATH + "/men/coats", null, NO_CACHE_HEADERS,
            SC_OK);

        // Verify
        mockServerRule.verify();
        Document doc = Jsoup.parse(response.getContent());

        // Verify property fields
        Assert.assertEquals("Coats", doc.select("input[name=jcr:title]").val());
        Assert.assertTrue(doc.select("input[name=jcr:title]").hasAttr("disabled"));
    }

    @Test
    public void testAssetsProductsFinder() throws Exception {

        // Prepare
        mockServerRule.add(CATEGORY_COATS_RULE.build());
        mockServerRule.add(CATEGORY_MEN_RULE.build());
        mockServerRule.add(SEARCH_PRODUCTS_FULL_TEXT.build());
        mockServerRule.add(SEARCH_PRODUCT_BY_SKU.build());

        List<NameValuePair> params = URLParameterBuilder.create()
            .add("_dc", "1560254795557")
            .add("query", "coats")
            .add("itemResourceType", "commerce/gui/components/authoring/assetfinder/product")
            .add("limit", "0..20")
            .add("_charset_", "utf-8")
            .add("_", "1560254761223")
            .getList();

        // Perform
        SlingHttpResponse response = cAuthorAuthor.doGet("/bin/wcm/contentfinder/cifproduct/view.html" + JCR_BASE_PATH, params,
            NO_CACHE_HEADERS, SC_OK);

        // Verify
        mockServerRule.verify();
        Document doc = Jsoup.parse(response.getContent());

        Elements elements = doc.select("coral-card[data-asset-group=product]");
        Assert.assertEquals(1, elements.size());
        Assert.assertEquals(JCR_BASE_PATH + "/men/coats/meskwielt", elements.attr("data-path"));
    }

    @Test
    @Ignore
    public void testOmnisearch() throws Exception {

        // Prepare
        mockServerRule.add(CATEGORY_COATS_RULE.build());
        mockServerRule.add(CATEGORY_MEN_RULE.build());
        mockServerRule.add(SEARCH_PRODUCTS_FULL_TEXT.build());
        mockServerRule.add(SEARCH_PRODUCT_BY_SKU.build());

        List<NameValuePair> params = URLParameterBuilder.create()
            .add("p.guessTotal", "1000")
            .add("fulltext", "coats")
            .add("path", "/var/commerce/products")
            .add("property", "cq:commerceType")
            .add("property.value", "product")
            .add("_", "1562672338517")
            .getList();

        // Perform
        SlingHttpResponse response = cAuthorAuthor.doGet("/mnt/overlay/granite/ui/content/shell/omnisearch/searchresults.html", params,
            NO_CACHE_HEADERS, SC_OK);

        // Verify
        mockServerRule.verify();
        Document doc = Jsoup.parse(response.getContent());

        Elements elements = doc.select("coral-card[data-path^=" + JCR_BASE_PATH + "]");
        Assert.assertEquals(1, elements.size());
        Assert.assertEquals(JCR_BASE_PATH + "/men/coats/meskwielt", elements.attr("data-path"));
    }

    @Test
    public void testOmnisearchSuggestions() throws Exception {

        // Prepare
        mockServerRule.add(CATEGORY_COATS_RULE.build());
        mockServerRule.add(SEARCH_PRODUCTS_FULL_TEXT.build());
        mockServerRule.add(SEARCH_PRODUCT_BY_SKU.build());

        List<NameValuePair> params = URLParameterBuilder.create()
            .add("p.guessTotal", "1000")
            .add("fulltext", "coats")
            .add("path", "/var/commerce/products")
            .add("property", "cq:commerceType")
            .add("property.value", "product")
            .add("location", "product")
            .add("_", "1562672338517")
            .getList();

        // Perform
        SlingHttpResponse response = cAuthorAuthor.doGet("/libs/granite/omnisearch", params, NO_CACHE_HEADERS, SC_OK);

        // Verify
        mockServerRule.verify();

        // the JSON result must contain the suggestion "El gordo down jacket"
        JsonNode jsonNode = new ObjectMapper().readTree(response.getContent());
        JsonNode suggestions = jsonNode.get("suggestions");
        Assert.assertEquals(2, suggestions.size());

        boolean hasJacket = false;
        Iterator<JsonNode> elements = suggestions.elements();
        while (elements.hasNext()) {
            JsonNode suggestion = elements.next().get("suggestion");
            if (suggestion != null && suggestion.asText().equals("El gordo down jacket")) {
                hasJacket = true;
                break;
            }
        }
        Assert.assertTrue(hasJacket);
    }
}
