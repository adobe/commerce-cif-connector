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

package com.adobe.cq.commerce.graphql.core;

import javax.jcr.RepositoryException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import com.adobe.cq.commerce.api.conf.CommerceBasePathsService;
import com.adobe.granite.omnisearch.spi.core.OmniSearchHandler;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.core.contentfinder.ViewQuery;

public class GraphqlProductViewHandlerTest {

  SlingHttpServletRequest servletRequest;
  ResourceResolver resourceResolver;
  TagManager tagManager;
  CommerceBasePathsService commerceBasePathsService;
  RequestPathInfo requestPathInfo;
  OmniSearchHandler omniSearchHandler;
  SearchResult searchResult;
  GraphqlProductViewHandler viewHandler;

  @Before
  public void setUp() {
    servletRequest = Mockito.mock(SlingHttpServletRequest.class);
    resourceResolver = Mockito.mock(ResourceResolver.class);
    Mockito.when(servletRequest.getResourceResolver()).thenReturn(resourceResolver);

    tagManager = Mockito.mock(TagManager.class);
    Mockito.when(resourceResolver.adaptTo(TagManager.class)).thenReturn(tagManager);

    commerceBasePathsService = Mockito.mock(CommerceBasePathsService.class);
    Mockito.when(resourceResolver.adaptTo(CommerceBasePathsService.class))
        .thenReturn(commerceBasePathsService);
    Mockito.when(commerceBasePathsService.getProductsBasePath())
        .thenReturn("/var/commerce/products");

    requestPathInfo = Mockito.mock(RequestPathInfo.class);
    Mockito.when(servletRequest.getRequestPathInfo()).thenReturn(requestPathInfo);
    Mockito.when(requestPathInfo.getSuffix()).thenReturn("/var/commerce/products");

    omniSearchHandler = Mockito.mock(OmniSearchHandler.class);
    searchResult = Mockito.mock(SearchResult.class);

    viewHandler = new GraphqlProductViewHandler();
    viewHandler.omniSearchHandler = omniSearchHandler;
    Mockito.when(omniSearchHandler.getResults(resourceResolver, null, 0, 0))
        .thenReturn(searchResult);
  }

  @Test
  public void testCreateTagSearchQuery() throws RepositoryException {
    ViewQuery viewQuery = viewHandler.createQuery(servletRequest, null, "tag:me");
    PredicateGroup predicateGroup =
        ((GraphqlProductViewHandler.GQLViewQuery) viewQuery).predicateGroup;
    Assert.assertNotNull(predicateGroup.getByName("1_tagsearch"));
  }

  @Test
  public void testCreateTextSearchQuery() throws RepositoryException {
    ViewQuery viewQuery = viewHandler.createQuery(servletRequest, null, "query=trail");
    PredicateGroup predicateGroup =
        ((GraphqlProductViewHandler.GQLViewQuery) viewQuery).predicateGroup;
    Assert.assertTrue(((PredicateGroup) predicateGroup.get(0)).get(0).getType().equals("fulltext"));
  }

  @Test
  public void testCreateEmptySearchQueryWithLimit() throws RepositoryException {
    Mockito.when(servletRequest.getParameter("limit")).thenReturn("0..20");
    ViewQuery viewQuery = viewHandler.createQuery(servletRequest, null, "");
    PredicateGroup predicateGroup =
        ((GraphqlProductViewHandler.GQLViewQuery) viewQuery).predicateGroup;
    Assert.assertEquals("0", predicateGroup.get("offset"));
    Assert.assertEquals("20", predicateGroup.get("limit"));
  }

  @Test
  public void testCreateRatingSearchQuery() throws RepositoryException {
    ViewQuery viewQuery = viewHandler.createQuery(servletRequest, null, "rating:5");
    PredicateGroup predicateGroup =
        ((GraphqlProductViewHandler.GQLViewQuery) viewQuery).predicateGroup;
    Assert.assertEquals(predicateGroup.getByName("1_property").get("property"), "rating");
  }
}
