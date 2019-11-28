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

import org.apache.sling.testing.clients.SlingHttpResponse;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;

import com.adobe.cq.testing.client.CommerceClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;

import static com.adobe.cq.commerce.it.http.CommerceTestBase.NO_CACHE_HEADERS;
import static org.apache.http.HttpStatus.SC_OK;

public class PageEditorExtensionIT {
    private static final String PAGE_EDITOR_PATH = "/editor.html";
    private static final String TEST_PDP_PATH = "/content/testproject/testpdp.html";
    private static final String PREVIEW_PDP_BUTTON_SELECTOR = "#pageinfo-data > button.cq-commerce-cifproductpicker-activator.cq-commerce-pdp-preview-activator:contains(%s)";
    private static final String TEST_PLP_PATH = "/content/testproject/testplp.html";
    private static final String PREVIEW_PLP_BUTTON_SELECTOR = "#pageinfo-data > button.cq-commerce-cifcategorypicker-activator.cq-commerce-plp-preview-activator:contains(%s)";

    private static CommerceClient cAdminAuthor;
    private static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    @ClassRule
    public static TestRule chain = RuleChain.outerRule(cqBaseClassRule);

    @BeforeClass
    public static void initCommerceClient() {
        cAdminAuthor = cqBaseClassRule.authorRule.getAdminClient(CommerceClient.class);
    }

    @Test
    public void testPreviewPdpButton() throws Exception {
        String requestPath = PAGE_EDITOR_PATH + "/" + TEST_PDP_PATH;
        SlingHttpResponse response = cAdminAuthor.doGet(requestPath, null, NO_CACHE_HEADERS, SC_OK);

        Document doc = Jsoup.parse(response.getContent());

        // check menu option "View with Product"
        Assert.assertEquals(1, doc.select(String.format(PREVIEW_PDP_BUTTON_SELECTOR, "View with Product")).size());
    }

    @Test
    public void testPreviewPlpButton() throws Exception {
        String requestPath = PAGE_EDITOR_PATH + "/" + TEST_PLP_PATH;
        SlingHttpResponse response = cAdminAuthor.doGet(requestPath, null, NO_CACHE_HEADERS, SC_OK);

        Document doc = Jsoup.parse(response.getContent());

        // check menu option "View with Product"
        Assert.assertEquals(1, doc.select(String.format(PREVIEW_PLP_BUTTON_SELECTOR, "View with Category")).size());
    }
}
