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

package com.adobe.cq.commerce.gui.components.common.cifproductfield;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.collections.IteratorUtils;
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

import com.adobe.cq.commerce.api.conf.CommerceBasePathsService;
import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.cq.commerce.gui.components.common.cifproductfield.ChildrenDataSourceServlet.Filter;
import com.adobe.granite.rest.utils.ModifiableMappedValueMapDecorator;
import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.EmptyDataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChildrenDataSourceServletTest {
    private static final String TEST_PATH = "/test/path/";

    private ChildrenDataSourceServlet servlet;
    private SlingHttpServletRequest request;
    private SlingHttpServletResponse response;
    private ValueMap dataSourceProperties;
    private List<Resource> children;

    @Before
    public void before() {
        servlet = new ChildrenDataSourceServlet();
        request = mock(SlingHttpServletRequest.class);
        response = mock(SlingHttpServletResponse.class);
        SlingBindings slingBindings = mock(SlingBindings.class);
        SlingScriptHelper slingScriptHelper = mock(SlingScriptHelper.class);
        Resource resource = mock(Resource.class);
        Resource dataSourceResource = mock(Resource.class);
        dataSourceProperties = new ModifiableMappedValueMapDecorator(new HashMap<>());
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        CommerceBasePathsService commerceBasePathsService = mock(CommerceBasePathsService.class);
        ExpressionResolver expressionResolver = mock(ExpressionResolver.class);
        Resource parentResource = mockFolderResource("path");
        children = new ArrayList<>();

        when(resourceResolver.getResource(TEST_PATH)).thenReturn(parentResource);
        when(slingBindings.getSling()).thenReturn(slingScriptHelper);
        when(request.getResource()).thenReturn(resource);
        when(resource.getChild(Config.DATASOURCE)).thenReturn(dataSourceResource);
        when(dataSourceResource.getValueMap()).thenReturn(dataSourceProperties);
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        when(slingScriptHelper.getService(ExpressionResolver.class)).thenReturn(expressionResolver);
        when(slingScriptHelper.getService(CommerceBasePathsService.class)).thenReturn(commerceBasePathsService);
        when(expressionResolver.resolve(anyString(), (Locale) anyObject(), (Class<? extends Object>) anyObject(),
            (SlingHttpServletRequest) anyObject())).thenAnswer((Answer<Object>) invocation -> invocation.getArguments()[0]);
        Map<String, Object> requestAttributes = new HashMap<>();
        requestAttributes.put(SlingBindings.class.getName(), slingBindings);
        doAnswer(invocation -> requestAttributes.put((String) invocation.getArguments()[0], invocation.getArguments()[1])).when(request)
            .setAttribute(anyString(), anyObject());
        when(request.getAttribute(anyString())).thenAnswer(invocationOnMock -> requestAttributes.get(invocationOnMock.getArguments()[0]));
        when(parentResource.listChildren()).thenAnswer(invocationOnMock -> children.iterator());
    }

    @Test
    public void testEmptyDataSource() {
        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertEquals(EmptyDataSource.instance(), dataSource);
    }

    @Test
    public void testNoDataDataSource() {
        dataSourceProperties.put("path", "/test/path");

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertFalse(dataSource.iterator().hasNext());
    }

    @Test
    public void testNoFilter() {
        dataSourceProperties.put("path", TEST_PATH);
        children.add(mockProductResource());

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertTrue(dataSource.iterator().hasNext());
    }

    @Test
    public void testWrongFilter() {
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("filter", "any");
        children.add(mockProductResource());

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertTrue(dataSource.iterator().hasNext());
    }

    @Test
    public void testProductFilter() {
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("filter", Filter.product);
        children.add(mockSomeResource());
        children.add(mockProductResource());
        children.add(mockFolderResource());

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertTrue(dataSource.iterator().hasNext());
        assertEquals(1, IteratorUtils.toList(dataSource.iterator()).size());
    }

    @Test
    public void testProductFilterNoResult() {
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("filter", Filter.product);
        children.add(mockSomeResource());
        children.add(mockFolderResource());

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertFalse(dataSource.iterator().hasNext());
    }

    @Test
    public void testFolderOrProductFilter() {
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("filter", Filter.folderOrProduct);
        children.add(mockSomeResource());
        children.add(mockProductResource());
        children.add(mockSomeResource());
        children.add(mockFolderResource());
        children.add(mockSomeResource());

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertTrue(dataSource.iterator().hasNext());
        assertEquals(2, IteratorUtils.toList(dataSource.iterator()).size());
    }

    @Test
    public void testFolderOrProductOrVariantFilter() {
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("filter", Filter.folderOrProductOrVariant);
        children.add(mockSomeResource());
        children.add(mockVariantResource());
        children.add(mockFolderResource());
        children.add(mockSomeResource());

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertTrue(dataSource.iterator().hasNext());
        assertEquals(2, IteratorUtils.toList(dataSource.iterator()).size());
    }

    @Test
    public void testItemResourceTypeNoResourceType() {
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("filter", Filter.folderOrProductOrVariant);
        children.add(mockVariantResource());

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertTrue(dataSource.iterator().hasNext());
        for (Iterator it = dataSource.iterator(); it.hasNext();) {
            Resource resource = (Resource) it.next();
            assertEquals("commerce/components/product", resource.getResourceType());
        }
    }

    @Test
    public void testItemResourceType() {
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("filter", Filter.folderOrProductOrVariant);
        dataSourceProperties.put("itemResourceType", "testRT");
        children.add(mockVariantResource());
        children.add(mockFolderResource());
        children.add(mockVariantResource());

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertTrue(dataSource.iterator().hasNext());
        for (Iterator it = dataSource.iterator(); it.hasNext();) {
            Resource resource = (Resource) it.next();
            assertTrue("testRT".equals(resource.getResourceType()));
        }
    }

    @Test
    public void testPredicateResourceWrapper() {
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("filter", Filter.folderOrProductOrVariant);
        Resource product = mockProductResource(false);
        children.add(product);
        children.add(mockFolderResource());
        List<Resource> productChildren = new ArrayList<>();
        Resource someResource = mock(Resource.class);
        when(someResource.getValueMap()).thenReturn(new ValueMapDecorator(new HashMap<>()));
        when(someResource.getName()).thenReturn("something");
        productChildren.add(someResource);
        productChildren.add(mockVariantResource("variant1"));
        Resource variant2 = mockVariantResource("variant2");
        productChildren.add(variant2);
        productChildren.add(mockVariantResource("variant3"));
        productChildren.add(mockFolderResource("folder1"));
        when(product.listChildren()).thenAnswer(invocation -> productChildren.iterator());
        when(product.hasChildren()).thenAnswer(invocation -> !productChildren.isEmpty());
        when(product.getChild("variant2")).thenReturn(variant2);
        when(product.getChild("something")).thenReturn(someResource);

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertTrue(dataSource.iterator().hasNext());
        Resource productResource = null;
        for (Iterator<Resource> it = dataSource.iterator(); it.hasNext();) {
            Resource r = it.next();
            if ("commerce/components/product".equals(r.getResourceType())) {
                productResource = r;
                break;
            }
        }
        assertNotNull(productResource);
        assertTrue(productResource.hasChildren());
        assertTrue(productResource.listChildren().hasNext());
        assertEquals("variant1", productResource.listChildren().next().getName());
        assertEquals("variant2", productResource.getChild("variant2").getName());
        assertNull(productResource.getChild("something"));

        int childCount = 0;
        int variantCount = 0;

        for (Iterator it = productResource.listChildren(); it.hasNext();) {
            Resource resource = (Resource) it.next();
            if ("commerce/components/product".equals(resource.getResourceType())) {
                assertEquals("variant", resource.getValueMap().get("cq:commerceType"));
                variantCount++;
            }
            childCount++;
        }
        assertEquals(4, childCount);
        assertEquals(3, variantCount);
    }

    @Test
    public void testQuerySimple() {
        dataSourceProperties.put("query", "variant");
        dataSourceProperties.put("rootPath", TEST_PATH);
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("filter", Filter.folderOrProductOrVariant);
        children.add(mockVariantResource());
        children.add(mockFolderResource());

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertTrue(dataSource.iterator().hasNext());
        assertTrue(dataSource.iterator().next().getName().startsWith("variant"));
        assertEquals(1, IteratorUtils.toList(dataSource.iterator()).size());
    }

    @Test
    public void testQuerySimpleNotFound() {
        dataSourceProperties.put("query", "product");
        dataSourceProperties.put("rootPath", TEST_PATH);
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("filter", Filter.folderOrProductOrVariant);
        children.add(mockVariantResource());
        children.add(mockFolderResource());

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertFalse(dataSource.iterator().hasNext());
    }

    @Test
    public void testQueryWithPath() {
        dataSourceProperties.put("query", TEST_PATH + "variant");
        dataSourceProperties.put("rootPath", TEST_PATH);
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("filter", Filter.folderOrProductOrVariant);
        children.add(mockVariantResource());
        children.add(mockFolderResource());

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertTrue(dataSource.iterator().hasNext());
        assertTrue(dataSource.iterator().next().getName().startsWith("variant"));
        assertEquals(1, IteratorUtils.toList(dataSource.iterator()).size());
    }

    @Test
    public void testQueryWithDifferentPath() {
        dataSourceProperties.put("query", TEST_PATH + "variant");
        dataSourceProperties.put("rootPath", "/other/path/");
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("filter", Filter.folderOrProductOrVariant);
        children.add(mockVariantResource());
        children.add(mockFolderResource());

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertFalse(dataSource.iterator().hasNext());
    }

    @Test
    public void testQueryWithMissingTerm() {
        dataSourceProperties.put("query", "/other/path/");
        dataSourceProperties.put("rootPath", "/other/path/");
        dataSourceProperties.put("path", TEST_PATH);
        dataSourceProperties.put("filter", Filter.folderOrProductOrVariant);
        children.add(mockVariantResource());
        children.add(mockFolderResource());

        servlet.doGet(request, response);

        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        assertNotNull(dataSource);
        assertFalse(dataSource.iterator().hasNext());
    }

    private Resource mockFolderResource() {
        return mockFolderResource("folder" + (int) (100 * Math.random()));
    }

    private Resource mockFolderResource(String name) {
        Resource resource = mock(Resource.class);
        ValueMap properties = new ModifiableMappedValueMapDecorator(new HashMap<>());
        properties.put("sling:resourceType", "sling:Folder");
        when(resource.getValueMap()).thenReturn(properties);
        when(resource.isResourceType("sling:Folder")).thenReturn(true);
        when(resource.getResourceType()).thenReturn("sling:Folder");
        when(resource.listChildren()).thenReturn(Collections.EMPTY_LIST.iterator());
        when(resource.getName()).thenReturn(name);

        return resource;
    }

    private Resource mockProductResource() {
        return mockProductResource(true);
    }

    private Resource mockProductResource(boolean mockChildren) {
        Resource resource = mock(Resource.class);
        ValueMap properties = new ModifiableMappedValueMapDecorator(new HashMap<>());
        properties.put("sling:resourceType", "commerce/components/product");
        properties.put("cq:commerceType", "product");
        when(resource.getValueMap()).thenReturn(properties);
        when(resource.isResourceType("commerce/components/product")).thenReturn(true);
        when(resource.getResourceType()).thenReturn("commerce/components/product");
        if (mockChildren) {
            when(resource.listChildren()).thenReturn(Collections.EMPTY_LIST.iterator());
        }
        when(resource.getName()).thenReturn("product" + (int) (100 * Math.random()));

        return resource;
    }

    private Resource mockVariantResource() {
        return mockVariantResource("variant" + (int) (100 * Math.random()));
    }

    private Resource mockVariantResource(String name) {
        Resource resource = mock(Resource.class);
        ValueMap properties = new ModifiableMappedValueMapDecorator(new HashMap<>());
        properties.put("sling:resourceType", "commerce/components/product");
        properties.put("cq:commerceType", "variant");
        when(resource.getValueMap()).thenReturn(properties);
        when(resource.isResourceType("commerce/components/product")).thenReturn(true);
        when(resource.getResourceType()).thenReturn("commerce/components/product");
        when(resource.listChildren()).thenReturn(Collections.EMPTY_LIST.iterator());
        when(resource.getName()).thenReturn(name);

        return resource;
    }

    private Resource mockSomeResource() {
        Resource resource = mock(Resource.class);
        ValueMap properties = new ModifiableMappedValueMapDecorator(new HashMap<>());
        properties.put("sling:resourceType", "some/type");
        when(resource.getValueMap()).thenReturn(properties);
        when(resource.getResourceType()).thenReturn("some/type");
        when(resource.listChildren()).thenReturn(Collections.EMPTY_LIST.iterator());

        return resource;
    }
}
