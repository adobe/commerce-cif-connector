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

package com.adobe.cq.commerce.gui.components.configuration;

import java.util.List;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.wcm.testing.mock.aem.junit.AemContext;

public class ConfigurationColumnViewItemTest {

    public static final String CONFIGURATION_PATH = "/conf/testing/settings/cloudconfigs/commerce";
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Before
    public void setUp() {
        context.load().json("/context/jcr-conf-console.json", "/conf/testing");
        context.addModelsForClasses(ConfigurationColumnViewItem.class);
    }

    @Test
    public void testGetTitle() {
        context.currentResource(context.resourceResolver().getResource(CONFIGURATION_PATH));

        ConfigurationColumnViewItem columnViewItem = context.request().adaptTo(ConfigurationColumnViewItem.class);
        Assert.assertEquals("Returns the correct title", "Mock configuration", columnViewItem.getTitle());
    }

    @Test
    public void testHasChildrenFalse() {
        context.currentResource(context.resourceResolver().getResource(CONFIGURATION_PATH));
        ConfigurationColumnViewItem columnViewItem = context.request().adaptTo(ConfigurationColumnViewItem.class);

        Assert.assertFalse("Configuration doesn't have children", columnViewItem.hasChildren());
    }

    @Test
    public void testHasChildrenTrue() {
        context.currentResource(context.resourceResolver().getResource("/conf/testing"));
        ConfigurationColumnViewItem columnViewItem = context.request().adaptTo(ConfigurationColumnViewItem.class);

        Assert.assertTrue("Configuration has children", columnViewItem.hasChildren());
    }

    @Test
    public void testGetQuickActions() {
        context.currentResource(context.resourceResolver().getResource("/conf/testing"));
        ConfigurationColumnViewItem columnViewItem = context.request().adaptTo(ConfigurationColumnViewItem.class);

        String[] expectedActions = new String[] { "cq-confadmin-actions-properties-activator",
            "cq-confadmin-actions-delete-activator" };

        List<String> actualActions = columnViewItem.getQuickActionsRel();
        Assert.assertArrayEquals("Returns the quick-actions", expectedActions, actualActions.toArray());

    }
}
