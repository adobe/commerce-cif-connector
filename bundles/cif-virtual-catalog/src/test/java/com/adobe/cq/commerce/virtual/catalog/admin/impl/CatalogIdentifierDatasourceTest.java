/*******************************************************************************
 *
 * Copyright 2019 Adobe. All rights reserved. This file is licensed to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License. You
 * may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.adobe.cq.commerce.virtual.catalog.admin.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import com.adobe.cq.commerce.virtual.catalog.data.CatalogIdentifierService;
import com.adobe.granite.ui.components.ds.DataSource;

/**
 * Unit tests for {@link CatalogIdentifierDatasource}
 */
public class CatalogIdentifierDatasourceTest {

  @Rule
  public SlingContext context = new SlingContext();

  private CatalogIdentifierDatasource classUnderTest;

  private SlingHttpServletRequest mockRequest;

  private SlingHttpServletResponse mockResponse;

  private CatalogIdentifierService mockCatalogIdentifierService;

  @Before
  public void setUp() {
    classUnderTest = new CatalogIdentifierDatasource();

    Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>() {
      {
        put("key1", Arrays.asList(new String[] {"value1", "value2"}));
        put("key2", Arrays.asList(new String[] {"value3", "value4"}));
      }
    };
    mockCatalogIdentifierService = Mockito.mock(CatalogIdentifierService.class);
    Mockito.when(mockCatalogIdentifierService.getCatalogIdentifiersForAllCommerceProviders())
        .thenReturn(testData);
    Whitebox.setInternalState(classUnderTest, "catalogIdentifierService",
        mockCatalogIdentifierService);

    mockRequest =
        new MockSlingHttpServletRequest(context.resourceResolver(), context.bundleContext());
    mockResponse = new MockSlingHttpServletResponse();
  }

  @Test
  public void testFlattenMap() {
    Map<String, Collection<String>> testData = new HashMap<String, Collection<String>>() {
      {
        put("key1", Arrays.asList(new String[] {"value1", "value2"}));
        put("key2", Arrays.asList(new String[] {"value3", "value4"}));
      }
    };
    String[] expectedResult =
        new String[] {"key1:value1", "key1:value2", "key2:value3", "key2:value4"};

    List<String> flattenData = classUnderTest.flattenMapToList(testData);
    Assert.assertArrayEquals("The map was flatten correctly", expectedResult,
        flattenData.toArray());
  }

  @Test
  public void testGenerateResources() {
    List<String> testData =
        Arrays.asList("key1:value1", "key1:value2", "key2:value3", "key2:value4");
    List<Resource> results =
        classUnderTest.generateResources(Mockito.mock(ResourceResolver.class), testData);

    Assert.assertEquals("The list has the correct number of elements", testData.size(),
        results.size());
    testData.stream().forEach(testProperty -> {
      Resource resourceToCheck = results.get(testData.indexOf(testProperty));
      ValueMap resourceProperties = resourceToCheck.getValueMap();

      Assert.assertEquals("The value of property [text] is [" + testProperty + "]", testProperty,
          resourceProperties.get("text"));
      Assert.assertEquals("The value of property [value] is [" + testProperty + "]", testProperty,
          resourceProperties.get("value"));
    });
  }

  @Test
  public void testDoGetWithData() throws ServletException, IOException {

    List<String> testResourceProperties =
        Arrays.asList("key1:value1", "key1:value2", "key2:value3", "key2:value4");

    classUnderTest.doGet(mockRequest, mockResponse);
    DataSource result = (DataSource) mockRequest.getAttribute(DataSource.class.getName());
    List<Resource> elementsInDatsource = new ArrayList<>();

    result.iterator().forEachRemaining(resourceToCheck -> {
      elementsInDatsource.add(resourceToCheck);
    });

    Assert.assertEquals("The datasource contains the correct number of elements",
        testResourceProperties.size(), elementsInDatsource.size());
  }

  @Test
  public void testDoGetWithNoProviders() throws ServletException, IOException {
    Mockito.when(mockCatalogIdentifierService.getCatalogIdentifiersForAllCommerceProviders())
        .thenReturn(new HashMap<>());

    classUnderTest.doGet(mockRequest, mockResponse);
    DataSource result = (DataSource) mockRequest.getAttribute(DataSource.class.getName());
    List<Resource> elementsInDatsource = new ArrayList<>();

    result.iterator().forEachRemaining(resourceToCheck -> {
      elementsInDatsource.add(resourceToCheck);
    });
    Assert.assertEquals("The datasource contains no elements", 0, elementsInDatsource.size());
  }

  @Test
  public void testDoGetWithNoAvailableService() throws ServletException, IOException {

    CatalogIdentifierDatasource datasourceToTest = new CatalogIdentifierDatasource();
    datasourceToTest.doGet(mockRequest, mockResponse);
    DataSource result = (DataSource) mockRequest.getAttribute(DataSource.class.getName());
    List<Resource> elementsInDatsource = new ArrayList<>();

    result.iterator().forEachRemaining(resourceToCheck -> {
      elementsInDatsource.add(resourceToCheck);
    });
    Assert.assertEquals(
        "The datasource contains no elements when the CatalogIdentifierService reference is not satisfied",
        0, elementsInDatsource.size());
  }
}
