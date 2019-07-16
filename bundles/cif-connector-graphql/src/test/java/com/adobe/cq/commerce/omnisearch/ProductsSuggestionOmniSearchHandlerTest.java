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

package com.adobe.cq.commerce.omnisearch;

import java.util.Collections;
import java.util.Map;

import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockValue;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.testing.mock.jcr.MockQueryResult;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;

import com.adobe.granite.omnisearch.spi.core.OmniSearchHandler;
import com.day.cq.commons.jcr.JcrConstants;

public class ProductsSuggestionOmniSearchHandlerTest {

    private static final String EL_GORDO_DOWN_JACKET = "El Gordo Down Jacket";
    private static final String BROOKLYN_COAT = "Brooklyn Coat";

    private ProductsSuggestionOmniSearchHandler suggestionHandler;
    private ResourceResolver resolver;

    @Before
    public void setUp() throws Exception {
        suggestionHandler = Mockito.spy(new ProductsSuggestionOmniSearchHandler());
        resolver = Mockito.mock(ResourceResolver.class);
    }

    @Test
    public void testGetResults() {
        OmniSearchHandler productsOmniSearchHandler = Mockito.mock(OmniSearchHandler.class);
        Whitebox.setInternalState(suggestionHandler, "productsOmniSearchHandler", productsOmniSearchHandler);

        Map<String, Object> params = Collections.singletonMap("fulltext", "whatever");
        suggestionHandler.getResults(resolver, params, 20, 10);

        // Search is delegated to the legacy Commerce Core ProductsSuggestionOmniSearchHandler
        Mockito.verify(productsOmniSearchHandler).getResults(resolver, params, 20, 10);
    }

    @Test
    public void testSuggestionQuery() throws Exception {

        // The mocked JCR result
        MockNode jcrNode = new MockNode("/var/commerce/products/jcrnode");
        jcrNode.setProperty("rep:suggest()", BROOKLYN_COAT);
        MockQueryResult jcrResults = new MockQueryResult(Collections.singletonList(jcrNode));
        Query jcrQuery = Mockito.mock(Query.class);
        Mockito.when(jcrQuery.execute()).thenReturn(jcrResults);

        Mockito.doReturn(jcrQuery).when(suggestionHandler).getSuperSuggestionQuery(resolver, "coats");
        Mockito.doReturn(resolver).when(suggestionHandler).getResourceResolver();

        ValueFactory valueFactory = Mockito.mock(ValueFactory.class);
        Mockito.when(valueFactory.createValue(EL_GORDO_DOWN_JACKET)).thenReturn(new MockValue(EL_GORDO_DOWN_JACKET));
        Session session = Mockito.mock(Session.class);
        Mockito.when(resolver.adaptTo(Session.class)).thenReturn(session);
        Mockito.when(session.getValueFactory()).thenReturn(valueFactory);

        // The mocked CIF result
        MockResource cifProduct = new MockResource(resolver, "/var/commerce/products/graphql/cifresource", "commerce/components/product");
        cifProduct.addProperty(JcrConstants.JCR_TITLE, EL_GORDO_DOWN_JACKET); // The title is used for the suggestion
        Mockito.when(resolver.findResources(Mockito.any(), Mockito.any()))
            .thenReturn(Collections.singletonList((Resource) cifProduct).iterator());

        suggestionHandler.activate(null);
        Query suggestionQuery = suggestionHandler.getSuggestionQuery(resolver, "coats");
        QueryResult queryResult = suggestionQuery.execute();
        RowIterator rows = queryResult.getRows();

        // The CIF result is first, then the JCR result
        Row row = rows.nextRow();
        Assert.assertEquals(EL_GORDO_DOWN_JACKET, row.getValue("rep:suggest()").getString());
        Assert.assertEquals(BROOKLYN_COAT, rows.nextRow().getValue("rep:suggest()").getString());

        // Not implemented
        Assert.assertNull(suggestionQuery.getLanguage());
        Assert.assertNull(suggestionQuery.getStatement());
        Assert.assertNull(suggestionQuery.getStoredQueryPath());
        Assert.assertNull(suggestionQuery.getBindVariableNames());
        Assert.assertNull(suggestionQuery.storeAsNode("whatever"));

        Assert.assertNull(queryResult.getColumnNames());
        Assert.assertNull(queryResult.getNodes());
        Assert.assertNull(queryResult.getSelectorNames());

        Assert.assertNull(row.getValues());
        Assert.assertNull(row.getPath());
        Assert.assertNull(row.getPath("whatever"));
        Assert.assertNull(row.getNode());
        Assert.assertNull(row.getNode("whatever"));
        Assert.assertEquals(0d, row.getScore(), 0);
        Assert.assertEquals(0d, row.getScore("whatever"), 0);
    }

    @Test
    public void testSuggestionQueryNoCifResults() throws Exception {

        // The mocked JCR result
        MockNode jcrNode = new MockNode("/var/commerce/products/jcrnode");
        jcrNode.setProperty("rep:suggest()", BROOKLYN_COAT);
        MockQueryResult jcrResults = new MockQueryResult(Collections.singletonList(jcrNode));
        Query jcrQuery = Mockito.mock(Query.class);
        Mockito.when(jcrQuery.execute()).thenReturn(jcrResults);

        Mockito.doReturn(jcrQuery).when(suggestionHandler).getSuperSuggestionQuery(resolver, "coats");
        Mockito.doReturn(resolver).when(suggestionHandler).getResourceResolver();

        Mockito.when(resolver.findResources(Mockito.any(), Mockito.any())).thenReturn(Collections.emptyListIterator());

        suggestionHandler.activate(null);
        Query suggestionQuery = suggestionHandler.getSuggestionQuery(resolver, "coats");
        QueryResult queryResult = suggestionQuery.execute();
        RowIterator rows = queryResult.getRows();

        // No CIF result, only JCR result
        Assert.assertEquals(BROOKLYN_COAT, rows.nextRow().getValue("rep:suggest()").getString());
        Assert.assertFalse(rows.hasNext());
    }
}
