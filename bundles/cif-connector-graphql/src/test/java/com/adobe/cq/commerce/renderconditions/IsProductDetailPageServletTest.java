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

package com.adobe.cq.commerce.renderconditions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import com.adobe.granite.rest.utils.ModifiableMappedValueMapDecorator;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.rendercondition.RenderCondition;
import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextCallback;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class IsProductDetailPageServletTest {
    private IsProductDetailPageServlet servlet;
    private SlingHttpServletRequest request;
    private SlingHttpServletResponse response;
    private ValueMap resourceProperties;

    @Rule
    public final AemContext context = createContext("/context/is-product-detail-page-servlet-test-context.json");

    @Before
    public void before() {
        servlet = new IsProductDetailPageServlet();
        request = mock(SlingHttpServletRequest.class);
        response = mock(SlingHttpServletResponse.class);

        SlingBindings slingBindings = mock(SlingBindings.class);
        Map<String, Object> requestAttributes = new HashMap<>();
        doAnswer(invocation -> requestAttributes.put((String) invocation.getArguments()[0], invocation.getArguments()[1])).when(request)
            .setAttribute(anyString(), anyObject());
        when(request.getAttribute(anyString())).thenAnswer(invocationOnMock -> requestAttributes.get(invocationOnMock.getArguments()[0]));
        requestAttributes.put(SlingBindings.class.getName(), slingBindings);
        SlingScriptHelper slingScriptHelper = mock(SlingScriptHelper.class);
        when(slingBindings.getSling()).thenReturn(slingScriptHelper);
        ExpressionResolver expressionResolver = mock(ExpressionResolver.class);
        when(slingScriptHelper.getService(ExpressionResolver.class)).thenReturn(expressionResolver);

        Resource resource = mock(Resource.class);
        when(request.getResource()).thenReturn(resource);
        resourceProperties = new ModifiableMappedValueMapDecorator(new HashMap<>());
        when(resource.getValueMap()).thenReturn(resourceProperties);
        ResourceResolver resourceResolver = context.resourceResolver();
        when(request.getResourceResolver()).thenReturn(resourceResolver);

        when(expressionResolver.resolve(anyString(), (Locale) anyObject(), (Class<? extends Object>) anyObject(),
            (SlingHttpServletRequest) anyObject())).thenAnswer((Answer<Object>) invocation -> long.class.equals(invocation
                .getArguments()[2]) ? Long.valueOf((String) invocation.getArguments()[0]) : invocation.getArguments()[0]);

        when(resource.getPath()).thenAnswer(invocationOnMock -> resourceProperties.get("path"));
    }

    @Test
    public void testNoPath() throws Exception {
        servlet.doGet(request, response);

        RenderCondition renderCondition = (RenderCondition) request.getAttribute(RenderCondition.class.getName());
        assertNotNull(renderCondition);
        assertFalse(renderCondition.check());
    }

    @Test
    public void testNoResource() throws Exception {
        resourceProperties.put("path", "noresource");

        servlet.doGet(request, response);

        RenderCondition renderCondition = (RenderCondition) request.getAttribute(RenderCondition.class.getName());
        assertNotNull(renderCondition);
        assertFalse(renderCondition.check());
    }

    @Test
    public void testNoPage() throws Exception {
        resourceProperties.put("path", "/content/nopage");

        servlet.doGet(request, response);

        RenderCondition renderCondition = (RenderCondition) request.getAttribute(RenderCondition.class.getName());
        assertNotNull(renderCondition);
        assertFalse(renderCondition.check());
    }

    @Test
    public void testNoContentPage() throws Exception {
        resourceProperties.put("path", "/content/nocontentpage");

        servlet.doGet(request, response);

        RenderCondition renderCondition = (RenderCondition) request.getAttribute(RenderCondition.class.getName());
        assertNotNull(renderCondition);
        assertFalse(renderCondition.check());
    }

    @Test
    public void testNoProductPage() throws Exception {
        resourceProperties.put("path", "/content/noproductpage");

        servlet.doGet(request, response);

        RenderCondition renderCondition = (RenderCondition) request.getAttribute(RenderCondition.class.getName());
        assertNotNull(renderCondition);
        assertFalse(renderCondition.check());
    }

    @Test
    public void testValidPage() throws Exception {
        resourceProperties.put("path", "/content/validpage");

        servlet.doGet(request, response);

        RenderCondition renderCondition = (RenderCondition) request.getAttribute(RenderCondition.class.getName());
        assertNotNull(renderCondition);
        assertTrue(renderCondition.check());
    }

    private AemContext createContext(String contentPath) {
        return new AemContext((AemContextCallback) context -> {
            context.load().json(contentPath, "/content");
        }, ResourceResolverType.JCR_MOCK);
    }
}
