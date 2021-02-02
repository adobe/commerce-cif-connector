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

import java.time.Instant;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.granite.ui.components.ExpressionResolver;
import com.day.cq.replication.ReplicationStatus;
import com.google.common.collect.ImmutableMap;
import io.wcm.testing.mock.aem.junit.AemContext;

public class ConfigurationColumnPreviewTest {

    public static final String CONFIGURATION_PATH = "/conf/testing/settings/cloudconfigs/commerce";
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    Resource columnPreviewDef;

    @Before
    public void setUp() {
        context.load().json("/context/jcr-conf-console.json", "/conf");

        ExpressionResolver expressionResolver = Mockito.mock(ExpressionResolver.class);
        Mockito.when(expressionResolver.resolve(Mockito.any(String.class), Mockito.any(Locale.class), Mockito.any(Class.class), Mockito.any(
            SlingHttpServletRequest.class))).thenReturn(CONFIGURATION_PATH);

        context.registerService(ExpressionResolver.class, expressionResolver);
        context.addModelsForClasses(ConfigurationColumnPreview.class);

        Map<String, Object> columnPreviewProps = ImmutableMap.of("sling:resourceType",
            "commerce/gui/components/configuration/columnpreview", "path", CONFIGURATION_PATH);
        columnPreviewDef = context.create().resource("/libs/something/columnpreview", columnPreviewProps);
    }

    @Test
    public void testGetProperties() {
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put("resource", columnPreviewDef);
        context.currentResource(columnPreviewDef);
        ConfigurationColumnPreview preview = context.request().adaptTo(ConfigurationColumnPreview.class);

        Assert.assertEquals("Returns the correct title", "Mock configuration", preview.getTitle());
        Assert.assertEquals("Returns whether it's folder or not", false, preview.isFolder());
        Assert.assertEquals("Returns the correct path", CONFIGURATION_PATH, preview.getItemResourcePath());
        Assert.assertEquals("Returns the last modified time", "2020-02-06T12:21:13Z", preview.getModifiedTime());
        Assert.assertEquals("Returns null for published times if not published", null, preview.getPublishedTime());
    }

    @Test
    public void testPublishedTime() {
        SlingBindings slingBindings = (SlingBindings) context.request().getAttribute(SlingBindings.class.getName());
        slingBindings.put("resource", columnPreviewDef);
        context.currentResource(columnPreviewDef);

        final String publishedTime = "2020-02-07T12:21:13Z";

        ReplicationStatus replicationStatus = Mockito.mock(ReplicationStatus.class);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Instant.parse(publishedTime).toEpochMilli());
        Mockito.when(replicationStatus.getLastPublished()).thenReturn(calendar);
        context.registerAdapter(Resource.class, ReplicationStatus.class, replicationStatus);

        ConfigurationColumnPreview preview = context.request().adaptTo(ConfigurationColumnPreview.class);
        Assert.assertEquals("Returns the published time", publishedTime, preview.getPublishedTime());

        Mockito.when(replicationStatus.isDeactivated()).thenReturn(true);
        preview = context.request().adaptTo(ConfigurationColumnPreview.class);
        Assert.assertEquals("Returns null for published time when deactivated", null, preview.getPublishedTime());
    }

}
