/*
 * ******************************************************************************
 *  *
 *  *    Copyright 2020 Adobe. All rights reserved.
 *  *    This file is licensed to you under the Apache License, Version 2.0 (the "License");
 *  *    you may not use this file except in compliance with the License. You may obtain a copy
 *  *    of the License at http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *    Unless required by applicable law or agreed to in writing, software distributed under
 *  *    the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 *  *    OF ANY KIND, either express or implied. See the License for the specific language
 *  *    governing permissions and limitations under the License.
 *  *
 *  *****************************************************************************
 */

package com.adobe.cq.commerce.gui.servlet;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import io.wcm.testing.mock.aem.junit.AemContext;

public class CreateConfigurationServletTest {

    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Test
    public void testFailOnMissingParams() throws ServletException, IOException {

        CreateConfigurationServlet servlet = new CreateConfigurationServlet();

        Resource mockResource = new MockResource(context.resourceResolver(),"/conf/venia", "sling:Folder");

        context.requestPathInfo().setExtension("json");
        context.requestPathInfo().setResourcePath("/conf/venia");
        context.requestPathInfo().setSelectorString("createcifconf");
        context.request().setResource(mockResource);

        servlet.doPost(context.request(), context.response());
        Assert.assertEquals("Returns status 500 for missing parameters", context.response().getStatus(), 500);
    }
}
