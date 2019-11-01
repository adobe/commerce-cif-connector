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

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
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

public class AssetMetadataEditorExtensionIT {
    private static final String ASSET_METADATA_EDITOR_PATH = "/mnt/overlay/dam/gui/content/assets/metadataeditor.external.html";
    private static final String ASSET_PATH = "/content/dam/ciftest/ciftest.jpg";
    private static final String CORAL_TAB_SELECTOR = "coral-tab:contains(%s)";
    private static final String FIELD_LABEL_SELECTOR = "div.coral-Form-fieldwrapper label:contains(%s)";
    private static final String FIELD_NAME_SELECTOR = "div.coral-Form-fieldwrapper foundation-autocomplete[name=%s]";

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
    public void testProductField() throws Exception {
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("item", ASSET_PATH));
        SlingHttpResponse response = cAdminAuthor.doGet(ASSET_METADATA_EDITOR_PATH, params, NO_CACHE_HEADERS, SC_OK);

        Document doc = Jsoup.parse(response.getContent());

        // check Commerce tab
        Assert.assertEquals(1, doc.select(String.format(CORAL_TAB_SELECTOR, "Commerce")).size());
        // check product field label
        Assert.assertEquals(1, doc.select(String.format(FIELD_LABEL_SELECTOR, "Product SKUs")).size());
        // check product field name
        Assert.assertEquals(1, doc.select(String.format(FIELD_NAME_SELECTOR, "./jcr:content/metadata/cq:productSku")).size());
    }
}
