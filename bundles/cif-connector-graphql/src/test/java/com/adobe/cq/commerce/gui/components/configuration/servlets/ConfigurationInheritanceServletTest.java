/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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
package com.adobe.cq.commerce.gui.components.configuration.servlets;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.http.HttpServletResponse;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.servlethelpers.MockSlingHttpServletRequest;
import org.apache.sling.servlethelpers.MockSlingHttpServletResponse;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import io.wcm.testing.mock.aem.junit.AemContext;

import static org.mockito.Mockito.verify;

public class ConfigurationInheritanceServletTest {

    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    private MockSlingHttpServletRequest request;
    private MockSlingHttpServletResponse response;
    private ConfigurationInheritanceServlet servlet;

    @Before
    public void setUp() {
        context.load().json("/context/jcr-conf-inheritance.json", "/conf/default");
        request = new MockSlingHttpServletRequest(context.resourceResolver());
        response = new MockSlingHttpServletResponse();

        Resource resource = Mockito.mock(Resource.class);

        Mockito.when(resource.getResourceResolver()).thenReturn(context.resourceResolver());
        Mockito.when(resource.getValueMap()).thenReturn(new ModifiableValueMapDecorator(new HashMap<>()));
        request.setResource(resource);
        servlet = new ConfigurationInheritanceServlet();
    }

    @Test
    public void testInheritedConfig() throws IOException, JSONException {
        JSONObject jsonResponse = requestConfigInheritance("/conf/default/level1/level2");
        Assert.assertTrue(jsonResponse.getBoolean("inherited"));
        Assert.assertTrue(jsonResponse.has("overriddenProperties"));
        Assert.assertTrue(jsonResponse.has("inheritedProperties"));

        JSONArray overriddenProps = jsonResponse.getJSONArray("overriddenProperties");

        Assert.assertEquals(1, overriddenProps.length());
        Assert.assertEquals("magentoGraphqlEndpoint", overriddenProps.getString(0));

        JSONObject inheritedProps = jsonResponse.getJSONObject("inheritedProperties");

        Assert.assertEquals(7, inheritedProps.length());

        JSONObject level1Property = inheritedProps.getJSONObject("cq:graphqlClient");
        JSONObject rootProperty = inheritedProps.getJSONObject("cq:catalogIdentifier");

        Assert.assertEquals("not-default", level1Property.getString("value"));
        Assert.assertEquals("/conf/default/level1", level1Property.getString("inheritedFrom"));

        Assert.assertEquals("my-catalog", rootProperty.getString("value"));
        Assert.assertEquals("/conf/default", rootProperty.getString("inheritedFrom"));
    }

    @Test
    public void testMainConfig() throws IOException, JSONException {
        JSONObject jsonResponse = requestConfigInheritance("/conf/default");
        Assert.assertFalse(jsonResponse.getBoolean("inherited"));
    }

    @Test
    public void testWrongConfigPath() throws IOException, JSONException {
        SlingHttpServletResponse mockResponse = Mockito.mock(SlingHttpServletResponse.class);
        Mockito.when(request.getResource().getPath()).thenReturn("/conf/default/non-existent");
        servlet.doGet(request, mockResponse);
        verify(mockResponse).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load configuration container resource");
    }

    @Test
    public void testExceptionHandling() throws IOException {
        SlingHttpServletResponse mockResponse = Mockito.mock(SlingHttpServletResponse.class);
        Mockito.when(request.getResource().getPath()).thenThrow(JSONException.class);
        servlet.doGet(request, mockResponse);
        verify(mockResponse).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to create JSON response");
    }

    private JSONObject requestConfigInheritance(String path) throws IOException, JSONException {
        Mockito.when(request.getResource().getPath()).thenReturn(path);
        servlet.doGet(request, response);
        Assert.assertEquals("application/json", response.getContentType());

        return new JSONObject(response.getOutputAsString());
    }
}
