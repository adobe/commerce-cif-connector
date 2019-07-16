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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.observation.EventIterator;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.commons.iterator.RowIteratorAdapter;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.commerce.api.conf.CommerceBasePathsService;
import com.adobe.granite.omnisearch.commons.AbstractOmniSearchHandler;
import com.adobe.granite.omnisearch.spi.core.OmniSearchHandler;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component(immediate = true, service = OmniSearchHandler.class)
public class ProductsSuggestionOmniSearchHandler extends AbstractOmniSearchHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductsSuggestionOmniSearchHandler.class);

    // We replace the legacy omnisearch component from Commerce Core
    private static final String TYPE = "product";

    private static final String VIRTUAL_PRODUCT_QUERY_LANGUAGE = "virtualProductOmnisearchQuery";
    private static final String PARAMETER_OFFSET = "_commerce_offset";
    private static final String PARAMETER_LIMIT = "_commerce_limit";

    @Reference
    private ResourceResolverFactory resolverFactory = null;

    @Reference
    private QueryBuilder queryBuilder = null;

    // For the search, we reuse the legacy component from Commerce Core
    @Reference(
        cardinality = ReferenceCardinality.MANDATORY,
        target = "(component.name=com.adobe.cq.commerce.impl.omnisearch.ProductsOmniSearchHandler)")
    private OmniSearchHandler productsOmniSearchHandler;

    // This is never used, it id declared to "enforce" the dependency to Commerce Core
    // so that this bundle is always loaded or restarted AFTER the Commerce Core bundle
    // so we properly override the 'product' omnisearch handler
    @Reference
    private CommerceBasePathsService cbps;

    @Activate
    protected void activate(ComponentContext componentContext) throws LoginException, PathNotFoundException, RepositoryException {
        if (resolver == null) {
            resolver = getResourceResolver();
            init(resolver);
            jsonMapper = new ObjectMapper();
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) throws LoginException {
        try {
            destroy(resolver);
        } finally {
            resolver.close();
        }
    }

    @Override
    public String getID() {
        return TYPE;
    }

    private ResourceResolver resolver;
    private ObjectMapper jsonMapper;

    @Override
    public void onEvent(EventIterator eventIterator) {
        if (resolver == null) {
            try {
                resolver = getResourceResolver();
                init(resolver);
                jsonMapper = new ObjectMapper();
            } catch (LoginException e) {
                LOGGER.error("Error initializing!", e);
            }
        }
    }

    @Override
    public SearchResult getResults(ResourceResolver resolver, Map<String, Object> predicateParameters, long limit, long offset) {
        return productsOmniSearchHandler.getResults(resolver, predicateParameters, limit, offset);
    }

    protected javax.jcr.query.Query getSuperSuggestionQuery(ResourceResolver resolver, String searchTerm) {
        return super.getSuggestionQuery(resolver, searchTerm);
    }

    @Override
    public javax.jcr.query.Query getSuggestionQuery(ResourceResolver resolver, String searchTerm) {
        LOGGER.debug("Calling suggestion query with '{}'", searchTerm);
        return new SuggestionQueryWrapper(getSuperSuggestionQuery(resolver, searchTerm), searchTerm);
    }

    Iterator<Resource> getVirtualResults(ResourceResolver resolver, Map<String, Object> predicateParameters, long limit, long offset) {
        Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.putAll(predicateParameters);
        queryParameters.put(PARAMETER_OFFSET, String.valueOf(offset));
        queryParameters.put(PARAMETER_LIMIT, String.valueOf(limit));
        String queryString = mapToString(queryParameters);
        Iterator<Resource> virtualResults = null;
        try {
            virtualResults = resolver.findResources(queryString, VIRTUAL_PRODUCT_QUERY_LANGUAGE);
        } catch (Exception x) {
            LOGGER.error("Error searching virtual products", x);
        }
        return virtualResults;
    }

    String mapToString(Map<String, Object> map) {
        try {
            return jsonMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize map data", e);
            return null;
        }
    }

    ResourceResolver getResourceResolver() throws LoginException {
        Map<String, Object> param = new HashMap<>();
        param.put(ResourceResolverFactory.SUBSERVICE, OMNI_SEARCH_SERVICE_USER);
        return resolverFactory.getServiceResourceResolver(param);
    }

    private class SuggestionQueryWrapper implements javax.jcr.query.Query {

        private SuggestionQueryWrapper(javax.jcr.query.Query query, String searchTerm) {
            this.query = query;
            this.searchTerm = searchTerm;
        }

        private javax.jcr.query.Query query;
        private String searchTerm;

        @Override
        public QueryResult execute() throws InvalidQueryException, RepositoryException {
            // Get JCR results
            QueryResult queryResult = query.execute();

            // Get CIF products
            Iterator<Resource> it = getVirtualResults(resolver, Collections.singletonMap("fulltext", searchTerm), 10, 0);
            if (it == null || !it.hasNext()) {
                return queryResult; // No CIF results
            }

            ValueFactory valueFactory = resolver.adaptTo(Session.class).getValueFactory();
            List<Row> rows = new ArrayList<>();
            while (it.hasNext()) {
                String title = it.next().getValueMap().get(JcrConstants.JCR_TITLE, String.class);
                Value value = valueFactory.createValue(title);
                rows.add(new SuggestionRow(value));
            }

            RowIterator suggestionIterator = queryResult.getRows();
            while (suggestionIterator.hasNext()) {
                rows.add(suggestionIterator.nextRow());
            }

            SuggestionQueryResult result = new SuggestionQueryResult(rows);
            return result;
        }

        @Override
        public void setLimit(long limit) {}

        @Override
        public void setOffset(long offset) {}

        @Override
        public String getStatement() {
            return null;
        }

        @Override
        public String getLanguage() {
            return null;
        }

        @Override
        public String getStoredQueryPath() throws ItemNotFoundException, RepositoryException {
            return null;
        }

        @Override
        public Node storeAsNode(String absPath) throws ItemExistsException, PathNotFoundException, VersionException,
            ConstraintViolationException, LockException, UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        @Override
        public void bindValue(String varName, Value value) throws IllegalArgumentException, RepositoryException {}

        @Override
        public String[] getBindVariableNames() throws RepositoryException {
            return null;
        }
    }

    private class SuggestionQueryResult implements QueryResult {

        private List<Row> rows;

        private SuggestionQueryResult(List<Row> rows) {
            this.rows = rows;
        }

        @Override
        public String[] getColumnNames() throws RepositoryException {
            return null;
        }

        @Override
        public RowIterator getRows() throws RepositoryException {
            return new RowIteratorAdapter(rows.iterator());
        }

        @Override
        public NodeIterator getNodes() throws RepositoryException {
            return null;
        }

        @Override
        public String[] getSelectorNames() throws RepositoryException {
            return null;
        }
    }

    private class SuggestionRow implements Row {

        private Value value;

        private SuggestionRow(Value value) {
            this.value = value;
        }

        @Override
        public Value[] getValues() throws RepositoryException {
            return null;
        }

        @Override
        public Value getValue(String columnName) throws ItemNotFoundException, RepositoryException {
            return value;
        }

        @Override
        public Node getNode() throws RepositoryException {
            return null;
        }

        @Override
        public Node getNode(String selectorName) throws RepositoryException {
            return null;
        }

        @Override
        public String getPath() throws RepositoryException {
            return null;
        }

        @Override
        public String getPath(String selectorName) throws RepositoryException {
            return null;
        }

        @Override
        public double getScore() throws RepositoryException {
            return 0;
        }

        @Override
        public double getScore(String selectorName) throws RepositoryException {
            return 0;
        }
    }
}
