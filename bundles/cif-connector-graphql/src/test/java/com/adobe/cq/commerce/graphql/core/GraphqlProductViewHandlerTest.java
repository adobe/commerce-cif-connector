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

package com.adobe.cq.commerce.graphql.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import javax.jcr.RepositoryException;
import javax.servlet.RequestDispatcher;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.adobe.cq.commerce.api.conf.CommerceBasePathsService;
import com.adobe.granite.omnisearch.spi.core.OmniSearchHandler;
import com.day.cq.commons.feed.StringResponseWrapper;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.core.contentfinder.Hit;
import com.day.cq.wcm.core.contentfinder.ViewQuery;

import static com.adobe.cq.commerce.graphql.resource.GraphqlQueryLanguageProvider.CATEGORY_ID_PARAMETER;
import static com.adobe.cq.commerce.graphql.resource.GraphqlQueryLanguageProvider.CATEGORY_PATH_PARAMETER;
import static com.adobe.cq.commerce.graphql.search.CatalogSearchSupport.PN_CATALOG_PATH;

public class GraphqlProductViewHandlerTest {

    private static final String LINE_SEPARATOR = System.lineSeparator();

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
        Mockito.when(resourceResolver.adaptTo(CommerceBasePathsService.class)).thenReturn(commerceBasePathsService);
        Mockito.when(commerceBasePathsService.getProductsBasePath()).thenReturn("/var/commerce/products");

        requestPathInfo = Mockito.mock(RequestPathInfo.class);
        Mockito.when(servletRequest.getRequestPathInfo()).thenReturn(requestPathInfo);
        Mockito.when(requestPathInfo.getSuffix()).thenReturn("/var/commerce/products");

        omniSearchHandler = Mockito.mock(OmniSearchHandler.class);
        searchResult = Mockito.mock(SearchResult.class);

        viewHandler = new GraphqlProductViewHandler();
        viewHandler.omniSearchHandler = omniSearchHandler;
        Mockito.when(omniSearchHandler.getResults(resourceResolver, null, 0, 0)).thenReturn(searchResult);
    }

    @Test
    public void testCreateTagSearchQuery() throws RepositoryException {
        ViewQuery viewQuery = viewHandler.createQuery(servletRequest, null, "tag:me");
        PredicateGroup predicateGroup = ((GraphqlProductViewHandler.GQLViewQuery) viewQuery).predicateGroup;
        Assert.assertNotNull(predicateGroup.getByName("1_tagsearch"));
    }

    @Test
    public void testCreateTextSearchQuery() throws RepositoryException {
        ViewQuery viewQuery = viewHandler.createQuery(servletRequest, null, "query=trail");
        PredicateGroup predicateGroup = ((GraphqlProductViewHandler.GQLViewQuery) viewQuery).predicateGroup;
        Assert.assertTrue(((PredicateGroup) predicateGroup.get(0)).get(0).getType().equals("fulltext"));
    }

    @Test
    public void testCreateEmptySearchQueryWithLimit() throws RepositoryException {
        Mockito.when(servletRequest.getParameter("limit")).thenReturn("0..20");
        ViewQuery viewQuery = viewHandler.createQuery(servletRequest, null, "");
        PredicateGroup predicateGroup = ((GraphqlProductViewHandler.GQLViewQuery) viewQuery).predicateGroup;
        Assert.assertEquals("0", predicateGroup.get("offset"));
        Assert.assertEquals("20", predicateGroup.get("limit"));
    }

    @Test
    public void testCreateRatingSearchQuery() throws RepositoryException {
        ViewQuery viewQuery = viewHandler.createQuery(servletRequest, null, "rating:5");
        PredicateGroup predicateGroup = ((GraphqlProductViewHandler.GQLViewQuery) viewQuery).predicateGroup;
        Assert.assertEquals(predicateGroup.getByName("1_property").get("property"), "rating");
    }

    @Test
    public void testCategoryConstraintSearchQuery() throws RepositoryException {
        Mockito.when(servletRequest.getHeader("Referer")).thenReturn("editor.html/test/page.html");
        PageManager pageManager = Mockito.mock(PageManager.class);
        Mockito.when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        Page page = Mockito.mock(Page.class);
        Mockito.when(pageManager.getContainingPage("/test/page")).thenReturn(page);
        Resource contentResource = Mockito.mock(Resource.class);
        Mockito.when(page.getContentResource()).thenReturn(contentResource);
        ModifiableValueMapDecorator valueMap = new ModifiableValueMapDecorator(new HashMap<>());
        Mockito.when(contentResource.getValueMap()).thenReturn(valueMap);
        valueMap.put(PN_CATALOG_PATH, "/catalog/path");
        Resource resource = Mockito.mock(Resource.class);
        Mockito.when(resourceResolver.getResource("/catalog/path")).thenReturn(resource);
        ModifiableValueMapDecorator properties = new ModifiableValueMapDecorator(new HashMap<>());
        Mockito.when(resource.getValueMap()).thenReturn(properties);
        properties.put("cq:commerceType", "category");
        properties.put("cifId", "1");

        ViewQuery viewQuery = viewHandler.createQuery(servletRequest, null, "query=trail");
        PredicateGroup predicateGroup = ((GraphqlProductViewHandler.GQLViewQuery) viewQuery).predicateGroup;
        Assert.assertNotNull(predicateGroup.getByName("3_" + CATEGORY_ID_PARAMETER));
        Assert.assertEquals(predicateGroup.getByName("3_" + CATEGORY_ID_PARAMETER).get(CATEGORY_ID_PARAMETER), "1");
        Assert.assertNotNull(predicateGroup.getByName("4_" + CATEGORY_PATH_PARAMETER));
        Assert.assertEquals(predicateGroup.getByName("4_" + CATEGORY_PATH_PARAMETER).get(CATEGORY_PATH_PARAMETER), "/catalog/path");
    }

    @Test
    public void testGenerateHtmlOutput() throws Exception {
        SlingHttpServletResponse servletResponse = Mockito.mock(SlingHttpServletResponse.class);
        Collection<Hit> hits = new ArrayList<>();
        Hit hit = new Hit();
        hit.set("path", "/hit/path1");

        Hit hit2 = new Hit();
        hit2.set("path", "/hit/path2");
        hit2.set("resource", Mockito.mock(Resource.class));

        RequestDispatcher requestDispatcher = Mockito.mock(RequestDispatcher.class);
        Mockito.when(servletRequest.getRequestDispatcher(Mockito.any(Resource.class), Mockito.any())).thenReturn(requestDispatcher);
        Mockito.doAnswer(invocation -> {
            StringResponseWrapper response = invocation.getArgumentAt(1, StringResponseWrapper.class);
            response.getWriter().println("Hit");
            return null;
        }).when(requestDispatcher).include(Mockito.any(), Mockito.any(StringResponseWrapper.class));

        // no hit, no itemResourceType
        String output = viewHandler.generateHtmlOutput(Collections.emptyList(), servletRequest, servletResponse);
        Assert.assertEquals("[]", output);

        // one hit, no itemResourceType
        hits.add(hit);
        output = viewHandler.generateHtmlOutput(hits, servletRequest, servletResponse);
        Assert.assertNotNull(output);
        Assert.assertTrue(output.startsWith("[com.day.cq.wcm.core.contentfinder.Hit@"));

        // no hit, with itemResourceType
        Mockito.when(servletRequest.getParameter("itemResourceType")).thenReturn("item/resource/type");
        output = viewHandler.generateHtmlOutput(Collections.emptyList(), servletRequest, servletResponse);
        Assert.assertEquals("", output);

        // one hit, with itemResourceType, no resource
        output = viewHandler.generateHtmlOutput(hits, servletRequest, servletResponse);
        Assert.assertEquals("", output);

        // one hit with itemResourceType and resource
        hit.set("resource", Mockito.mock(Resource.class));
        output = viewHandler.generateHtmlOutput(hits, servletRequest, servletResponse);
        Assert.assertEquals("Hit" + LINE_SEPARATOR, output);

        // more hits with itemResourceType and resource
        hits.add(hit2);
        output = viewHandler.generateHtmlOutput(hits, servletRequest, servletResponse);
        Assert.assertEquals("Hit" + LINE_SEPARATOR + "Hit" + LINE_SEPARATOR, output);
    }
}
