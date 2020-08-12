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
package com.adobe.cq.commerce.virtual.catalog.models.impl;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.adobe.cq.commerce.virtual.catalog.models.ConfigurationReference;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextBuilder;

public class ConfigurationReferenceTest {

    public static final String CONFIGURATION_PATH = "/libs/commerce/components/cifrootfolder";

    @Rule
    public AemContext context = new AemContextBuilder(ResourceResolverType.JCR_OAK).build();
    private ConfigurationReference configurationReference;

    @Before
    public void setUp() {
        context.load().json("/context/jcr-dataroots.json", "/var/commerce");
        context.load().json("/context/jcr-conf-page.json", "/conf/testing/settings");
        context.load().json("/context/jcr-cifrootfolder-references.json", CONFIGURATION_PATH);
        context.requestPathInfo().setSuffix("");
        context.request().setQueryString("");
    }

    @Before

    @Test
    public void testTextIsSet() {
        context.currentResource(context.resourceResolver().getResource(CONFIGURATION_PATH + "/full-reference"));

        configurationReference = context.request().adaptTo(ConfigurationReference.class);
        Assert.assertEquals("Returns the correct title", "Configuration reference", configurationReference.getText());
    }

    @Test
    public void testTextIsNotSet() {
        context.currentResource(context.resourceResolver().getResource(CONFIGURATION_PATH + "/no-text-reference"));

        configurationReference = context.request().adaptTo(ConfigurationReference.class);
        Assert.assertEquals("Returns the correct title", "Reference", configurationReference.getText());
    }

    @Test
    public void testConfigurationExists() {
        context.currentResource(context.resourceResolver().getResource(CONFIGURATION_PATH + "/full-reference"));
        context.requestPathInfo().setSuffix("/var/commerce/products/testing");
        configurationReference = context.request().adaptTo(ConfigurationReference.class);

        Assert.assertEquals("Returns the correct URL",
            "/mnt/overlay/wcm/core/content/sites/properties.html?item=%2Fconf%2Ftesting%2Fsettings%2Fcloudconfigs%2Fcommerce",
            configurationReference.getConfigURL());

        context.request().setQueryString("item=%2Fvar%2Fcommerce%2Fproducts%2Ftesting");
        configurationReference = context.request().adaptTo(ConfigurationReference.class);

        Assert.assertEquals("Returns the correct URL",
            "/mnt/overlay/wcm/core/content/sites/properties.html?item=%2Fconf%2Ftesting%2Fsettings%2Fcloudconfigs%2Fcommerce",
            configurationReference.getConfigURL());
    }

    @Test
    public void testConfigurationWrongOrInexistent() {
        context.currentResource(context.resourceResolver().getResource(CONFIGURATION_PATH + "/full-reference"));
        context.requestPathInfo().setSuffix("/var/commerce/products/testingNoConfig");
        configurationReference = context.request().adaptTo(ConfigurationReference.class);

        Assert.assertNull(configurationReference.getConfigURL());

        context.requestPathInfo().setSuffix("/var/commerce/products/testingWrongConfig");
        configurationReference = context.request().adaptTo(ConfigurationReference.class);

        Assert.assertNull(configurationReference.getConfigURL());
    }
}
