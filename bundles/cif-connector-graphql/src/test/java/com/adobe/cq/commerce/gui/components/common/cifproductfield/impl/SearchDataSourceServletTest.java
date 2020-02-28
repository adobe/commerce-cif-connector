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

package com.adobe.cq.commerce.gui.components.common.cifproductfield.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.conf.CommerceBasePathsService;
import com.adobe.cq.commerce.graphql.resource.Constants;
import com.adobe.granite.rest.utils.ModifiableMappedValueMapDecorator;
import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;

import static com.adobe.cq.commerce.graphql.resource.GraphqlQueryLanguageProvider.CATEGORY_ID_PARAMETER;
import static com.adobe.cq.commerce.graphql.resource.GraphqlQueryLanguageProvider.CATEGORY_PATH_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SearchDataSourceServletTest {
    private static final String TEST_PATH = "/test/path/";

    private SearchDataSourceServlet servlet;
    private SlingHttpServletRequest request;
    private SlingHttpServletResponse response;
    private ValueMap dataSourceProperties;
    private ValueMap rootResourceProperties;
    private ResourceResolver resourceResolver;
    private List<Resource> results;
    private Map<String, String[]> requestParameterMap;
    private String queryStringSample;

    @Before
    public void before() {
        servlet = new SearchDataSourceServlet();
        request = mock(SlingHttpServletRequest.class);
        response = spy(SlingHttpServletResponse.class);
        SlingBindings slingBindings = mock(SlingBindings.class);
        SlingScriptHelper slingScriptHelper = mock(SlingScriptHelper.class);
        Resource resource = mock(Resource.class);
        Resource dataSourceResource = mock(Resource.class);
        dataSourceProperties = new ModifiableMappedValueMapDecorator(new HashMap<>());
        resourceResolver = mock(ResourceResolver.class);

        results = new ArrayList<>();
        when(resourceResolver.findResources(anyString(), anyString())).thenAnswer(invocation -> {
            queryStringSample = String.valueOf(invocation.getArguments()[0]);
            return queryStringSample.contains("\"fulltext\":[\"something\"]") ? results.iterator() : Collections.emptyIterator();
        });
        CommerceBasePathsService commerceBasePathsService = mock(CommerceBasePathsService.class);
        ExpressionResolver expressionResolver = mock(ExpressionResolver.class);

        Resource parentResource = ChildrenDataSourceServletTest.mockFolderResource("path");

        when(resourceResolver.getResource(TEST_PATH)).thenReturn(parentResource);
        when(slingBindings.getSling()).thenReturn(slingScriptHelper);
        when(request.getResource()).thenReturn(resource);
        when(resource.getChild(Config.DATASOURCE)).thenReturn(dataSourceResource);
        when(dataSourceResource.getValueMap()).thenReturn(dataSourceProperties);
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(slingScriptHelper.getService(ExpressionResolver.class)).thenReturn(expressionResolver);
        when(slingScriptHelper.getService(CommerceBasePathsService.class)).thenReturn(commerceBasePathsService);
        when(expressionResolver.resolve(anyString(), (Locale) anyObject(), (Class<? extends Object>) anyObject(),
            (SlingHttpServletRequest) anyObject())).thenAnswer((Answer<Object>) invocation -> long.class.equals(invocation
                .getArguments()[2]) ? Long.valueOf((String) invocation.getArguments()[0]) : invocation.getArguments()[0]);
        Map<String, Object> requestAttributes = new HashMap<>();
        requestAttributes.put(SlingBindings.class.getName(), slingBindings);
        doAnswer(invocation -> requestAttributes.put((String) invocation.getArguments()[0], invocation.getArguments()[1])).when(request)
            .setAttribute(anyString(), anyObject());
        when(request.getAttribute(anyString())).thenAnswer(invocationOnMock -> requestAttributes.get(invocationOnMock.getArguments()[0]));

        requestParameterMap = new HashMap<>();
        when(request.getParameterMap()).thenAnswer(invocation -> requestParameterMap);
        final Resource rootResource = mock(Resource.class);
        when(rootResource.getName()).thenReturn("root");
        rootResourceProperties = new ModifiableMappedValueMapDecorator(new HashMap<>());
        when(resourceResolver.getResource("rootPath")).thenReturn(rootResource);
        when(rootResource.getValueMap()).thenAnswer(invocationOnMock -> rootResourceProperties);
        queryStringSample = null;
    }

    @Test
    public void testNoResult() throws IOException {
        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertFalse(dataSource.iterator().hasNext());
    }

    @Test
    public void testSomeResult() throws IOException {
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("itemResourceType", "itemRT");
        requestParameterMap.put("fulltext", new String[] { "something" });
        results.add(mock(Resource.class));
        when(request.getParameter("root")).thenReturn("rootPath");

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        final Iterator<Resource> iterator = dataSource.iterator();
        assertTrue(iterator.hasNext());
        assertEquals("itemRT", iterator.next().getResourceType());
        assertFalse(iterator.hasNext());
        assertFalse(queryStringSample.contains("\"" + CATEGORY_PATH_PARAMETER + "\""));
        assertFalse(queryStringSample.contains("\"" + CATEGORY_ID_PARAMETER + "\""));
    }

    @Test
    public void testCategoryId() throws IOException {
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("path", TEST_PATH);
        results.add(mock(Resource.class));
        when(request.getParameter("root")).thenReturn("rootPath");
        rootResourceProperties.put(CommerceConstants.PN_COMMERCE_TYPE, Constants.CATEGORY);
        rootResourceProperties.put(Constants.CIF_ID, "0");

        servlet.doGet(request, response);

        assertNotNull(queryStringSample);
        assertTrue(queryStringSample.contains("\"" + CATEGORY_PATH_PARAMETER + "\":\"rootPath\""));
        assertTrue(queryStringSample.contains("\"" + CATEGORY_ID_PARAMETER + "\":\"0\""));
    }

    @Test
    public void testRootCategoryId() throws IOException {
        dataSourceProperties.put("path", TEST_PATH);
        results.add(mock(Resource.class));
        when(request.getParameter("root")).thenReturn("rootPath");
        rootResourceProperties.put(Constants.MAGENTO_ROOT_CATEGORY_ID_PROPERTY, "1");

        servlet.doGet(request, response);

        assertNotNull(queryStringSample);
        assertTrue(queryStringSample.contains("\"" + CATEGORY_PATH_PARAMETER + "\":\"rootPath\""));
        assertTrue(queryStringSample.contains("\"" + CATEGORY_ID_PARAMETER + "\":\"1\""));
    }

    @Test
    public void testError() throws IOException {
        when(resourceResolver.findResources(anyString(), anyString())).thenThrow(new RuntimeException("MyError"));

        servlet.doGet(request, response);

        verify(response).sendError(500, "MyError");
    }
}
