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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.osgi.OsgiConsoleClient;
import org.apache.sling.testing.clients.util.JsonUtils;
import org.apache.sling.testing.clients.util.poller.Polling;
import org.codehaus.jackson.JsonNode;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.testing.client.CommerceClient;
import com.adobe.cq.testing.junit.rules.CQAuthorClassRule;
import com.adobe.cq.testing.junit.rules.CQRule;
import com.adobe.cq.testing.mockserver.Server;
import com.adobe.cq.testing.mockserver.junit.ServerRule;

import static org.apache.http.HttpStatus.SC_MOVED_TEMPORARILY;

public class CommerceTestBase {

    private static final Logger LOG = LoggerFactory.getLogger(CommerceTestBase.class);

    protected static CommerceClient cAdminAuthor;
    protected static CommerceClient cAuthorAuthor;

    public static List<Header> NO_CACHE_HEADERS = new ArrayList<Header>() {
        {
            add(new BasicHeader("Cache-Control", "no-cache, no-store, must-revalidate"));
        }
    };

    public static final String GRAPHQL_CLIENT_BUNDLE = "com.adobe.commerce.cif.graphql-client";
    public static final String GRAPHQL_CLIENT_FACTORY_PID = "com.adobe.cq.commerce.graphql.client.impl.GraphqlClientImpl";

    public static CQAuthorClassRule cqBaseClassRule = new CQAuthorClassRule();
    public static ServerRule mockServerRule = new ServerRule(new Server.Builder().withHttps());

    public static GraphqlOSGiConfig graphqlOsgiConfig = new GraphqlOSGiConfig();

    private static final String CONSOLE_URL = "/system/console";
    private static final String CONFIGURATION_CONSOLE_URL = CONSOLE_URL + "/configMgr";
    private static final String VIRTUAL_CATALOG_BUNDLE_NAME = "com.adobe.commerce.cif.virtual-catalog";

    @ClassRule
    public static TestRule chain = RuleChain.outerRule(cqBaseClassRule).around(mockServerRule);

    @Rule
    public CQRule cqBaseRule = new CQRule(cqBaseClassRule.authorRule);

    @BeforeClass
    public static void initCommerceClient() throws ClientException, InterruptedException, TimeoutException {
        cAdminAuthor = cqBaseClassRule.authorRule.getAdminClient(CommerceClient.class);
        cAuthorAuthor = cqBaseClassRule.authorRule.getClient(CommerceClient.class, "imccoy", "password");

        String httpsPort = String.valueOf(mockServerRule.getHttpsPort());

        // This configures the lower-level GraphQL client. The Magento client is configured in the test-content package.
        graphqlOsgiConfig.withIdentifier("default")
            .withUrl("https://localhost:" + httpsPort + "/graphql")
            .withHttpMethod("POST")
            .withAcceptSelfSignedCertificates(true)
            .withCatalogCachingSchedulerEnabled(false);

        updateOSGiConfiguration(cAdminAuthor, graphqlOsgiConfig.build(), GRAPHQL_CLIENT_BUNDLE, GRAPHQL_CLIENT_FACTORY_PID);
    }

    /**
     * Fetches the PID of a service based on the factory PID.
     * 
     * @param osgiClient
     * @return The PID of the first configuration found for factory PID.
     * @throws ClientException
     */
    private static String getConfigurationPid(OsgiConsoleClient osgiClient, String factoryPID) throws ClientException {
        SlingHttpResponse resp = osgiClient.doGet(CONFIGURATION_CONSOLE_URL + "/*.json");
        JsonNode json = JsonUtils.getJsonNodeFromString(resp.getContent());
        Iterator<JsonNode> it = json.getElements();
        while (it.hasNext()) {
            JsonNode config = it.next();
            JsonNode factoryId = config.get("factoryPid");
            if (factoryId != null && factoryPID.equals(factoryId.getTextValue())) {
                return config.get("pid").getTextValue();
            }
        }
        return null;
    }

    protected static void updateOSGiConfiguration(CommerceClient client, Map<String, Object> config, String bundle, String factoryPID)
        throws ClientException,
        TimeoutException, InterruptedException {
        final OsgiConsoleClient osgiClient = client.adaptTo(OsgiConsoleClient.class);
        Polling polling = new Polling(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                try {
                    String state = osgiClient.getBundleState(bundle);
                    LOG.info("Bundle {} state is now {}", bundle, state);
                    Assert.assertEquals("Active", state);
                    return true;
                } catch (AssertionError e) {
                    return false;
                } catch (ClientException cex) {
                    LOG.error(cex.getMessage(), cex);
                    return false;
                }
            }
        });

        // Check that the bundle has started
        polling.poll(30000, 1000);

        LOG.info("Creating configuration. {}", config);
        String configurationPid = getConfigurationPid(osgiClient, factoryPID);
        osgiClient.waitEditConfiguration(30, configurationPid, null, config, SC_MOVED_TEMPORARILY);
        restartVirtualCatalogBundle(osgiClient);

        // Wait for bundle to restart
        polling.poll(30000, 1000);

        // Wait a bit more so that other bundles can restart
        Thread.sleep(5000);
    }

    private static void restartVirtualCatalogBundle(OsgiConsoleClient osgiConsoleClient) throws ClientException, TimeoutException,
        InterruptedException {
        osgiConsoleClient.stopBundle(VIRTUAL_CATALOG_BUNDLE_NAME);
        osgiConsoleClient.waitStartBundle(VIRTUAL_CATALOG_BUNDLE_NAME, 2000, 20);
    }

    /*
     * HApi helper functions.
     */

    /**
     * Return all HApi objects in given element.
     *
     * @param e
     * @return
     */
    protected Elements getItems(Element e) {
        return e.select("[itemscope]:not([itemprop])");
    }

    /**
     * Return all HApi object in a given element whose itemtype match the given regex.
     *
     * @param e
     * @param itemTypeRegex
     * @return
     */
    protected Elements getItems(Element e, String itemTypeRegex) {
        return e.getElementsByAttributeValueMatching("itemtype", itemTypeRegex);
    }

    /**
     * Return value of HApi object.
     *
     * @param e
     * @return
     */
    protected String getValue(Element e) {
        if (e.tagName().equals("meta")) {
            return e.attr("content");
        }
        if (e.tagName().equals("a")) {
            return e.attr("href");
        }
        if (e.tagName().equals("link")) {
            return e.attr("href");
        }
        if (e.tagName().equals("img")) {
            return e.attr("src");
        }
        if (e.tagName().equals("input")) {
            return e.attr("value");
        }
        return e.text();
    }

}
