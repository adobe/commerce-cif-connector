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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import com.adobe.cq.commerce.api.CommerceConstants;
import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.commerce.api.conf.CommerceBasePathsService;
import com.adobe.granite.omnisearch.spi.core.OmniSearchHandler;
import com.adobe.granite.xss.XSSAPI;
import com.day.cq.search.Predicate;
import com.day.cq.search.PredicateConverter;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.eval.FulltextPredicateEvaluator;
import com.day.cq.search.eval.JcrPropertyPredicateEvaluator;
import com.day.cq.search.eval.PathPredicateEvaluator;
import com.day.cq.search.eval.TypePredicateEvaluator;
import com.day.cq.search.result.SearchResult;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.tagging.impl.search.TagSearchPredicateEvaluator;
import com.day.cq.wcm.core.contentfinder.Hit;
import com.day.cq.wcm.core.contentfinder.ViewHandler;
import com.day.cq.wcm.core.contentfinder.ViewQuery;
import static com.day.cq.commons.jcr.JcrConstants.JCR_LASTMODIFIED;
import static com.day.cq.commons.jcr.JcrConstants.NT_UNSTRUCTURED;

/**
 * A ViewHandler servlet for products.
 */
@Component(service = Servlet.class, name = "GraphqlProductViewHandler", immediate = true,
    property = {"sling.servlet.paths=/bin/wcm/contentfinder/cifproduct/view"})
public class GraphqlProductViewHandler extends ViewHandler {

  private static final String PROPERTY_STEP = "*/";
  private static final String PRODUCT_COMMERCE_TYPE = "product";
  private static final String TAGS_PROP = "tags";
  private static final String CQ_TAGS_PROP = "cq:tags";

  @Reference(
      target = "(component.name=com.adobe.cq.commerce.impl.omnisearch.ProductsOmniSearchHandler)")
  OmniSearchHandler omniSearchHandler;

  @Reference
  private XSSAPI xssAPI;

  private TagManager tagManager;

  // ----------------------< Internal Methods >-------------------------------

  private static Hit createHit(Product product, XSSAPI xssAPI) {
    Hit hit = new Hit();
    String path = product.getPath();
    String name = path.substring(path.lastIndexOf("/") + 1);
    String title = product.getTitle();
    if (title == null || title.length() == 0) {
      title = name;
    }
    hit.set("name", xssAPI.encodeForHTML(name));
    hit.set("path", path);
    hit.set("thumbnail", xssAPI.getValidHref(product.getThumbnailUrl(48)));
    hit.set("title", xssAPI.encodeForHTML(title));
    hit.set("sku", xssAPI.encodeForHTML(product.getSKU()));
    hit.set("excerpt", xssAPI.filterHTML(""));

    return hit;
  }

  /**
   * As GQL supports only wildcards in the name property, we have to make some substitutions so
   * these wildcards aren't stripped out by gql
   *
   * @param queryString The query string.
   * @return The query string with '*' replaced by '%'.
   */
  protected static String preserveWildcards(String queryString) {
    // Replace "*" by "%", * so
    if (queryString != null) {
      queryString = queryString.replace("*", "%");
    }
    return queryString;
  }

  /**
   * As GQL supports only wildcards in the name property, we have to revert our substitutions
   *
   * @param queryString The query string.
   * @return The query string with '*' replaced by '%'.
   */
  protected static String revertWildcards(String queryString) {
    if (queryString != null) {
      // Replace "%" back to "*", to start a wildcard search
      queryString = queryString.replace("%", "*");
    }
    return queryString;
  }

  /**
   * Returns product property constraint in GQL for the given <code>request</code>.
   *
   * @param gqlPredicateGroup PredicateGroup containing the search predicates.
   */
  private static void addProductConstraint(PredicateGroup gqlPredicateGroup) {
    Predicate predicate = new Predicate(JcrPropertyPredicateEvaluator.PROPERTY);
    predicate.set(JcrPropertyPredicateEvaluator.PROPERTY, CommerceConstants.PN_COMMERCE_TYPE);
    predicate.set(JcrPropertyPredicateEvaluator.VALUE, PRODUCT_COMMERCE_TYPE);
    predicate.set(JcrPropertyPredicateEvaluator.OPERATION, JcrPropertyPredicateEvaluator.OP_EQUALS);
    gqlPredicateGroup.add(predicate);
  }

  @Override
  protected ViewQuery createQuery(SlingHttpServletRequest request, Session session,
      String queryString) throws RepositoryException {

    PredicateGroup gqlPredicateGroup = new PredicateGroup();
    queryString = preserveWildcards(queryString);

    gqlPredicateGroup = PredicateConverter.createPredicatesFromGQL(queryString);
    tagManager = request.getResourceResolver().adaptTo(TagManager.class);
    Set<String> predicateSet = customizePredicateGroup(gqlPredicateGroup);

    // set default start path
    RequestPathInfo pathInfo = request.getRequestPathInfo();
    final CommerceBasePathsService cbps =
        request.getResourceResolver().adaptTo(CommerceBasePathsService.class);
    String defaultStartPath = cbps.getProductsBasePath();
    String startPath =
        (pathInfo.getSuffix() != null && pathInfo.getSuffix().startsWith(defaultStartPath))
            ? pathInfo.getSuffix()
            : defaultStartPath;
    if (!predicateSet.contains(PathPredicateEvaluator.PATH)) {
      gqlPredicateGroup.add(
          new Predicate(PathPredicateEvaluator.PATH).set(PathPredicateEvaluator.PATH, startPath));
    }

    // append node type constraint to match product data index /etc/commerce/oak:index/commerce
    gqlPredicateGroup.add(new Predicate(TypePredicateEvaluator.TYPE)
        .set(TypePredicateEvaluator.TYPE, NT_UNSTRUCTURED));

    // append limit constraint
    if (gqlPredicateGroup.get(Predicate.PARAM_LIMIT) == null) {
      String limit = request.getParameter(LIMIT);
      if ((limit != null) && (!limit.equals(""))) {
        int offset = Integer.parseInt(StringUtils.substringBefore(limit, ".."));
        int total = Integer.parseInt(StringUtils.substringAfter(limit, ".."));
        gqlPredicateGroup.set(Predicate.PARAM_LIMIT, Long.toString(total - offset));
        gqlPredicateGroup.set(Predicate.PARAM_OFFSET, Long.toString(offset));
      } else {
        gqlPredicateGroup.set(Predicate.PARAM_LIMIT, Long.toString(DEFAULT_LIMIT));
      }
    }
    // add product property constraint
    addProductConstraint(gqlPredicateGroup);

    // append order constraint
    if (!predicateSet.contains(Predicate.ORDER_BY)) {
      gqlPredicateGroup
          .add(new Predicate(Predicate.ORDER_BY).set(Predicate.ORDER_BY, "@" + JCR_LASTMODIFIED)
              .set(Predicate.PARAM_SORT, Predicate.SORT_DESCENDING));
    }

    return new GQLViewQuery(request.getResourceResolver(), omniSearchHandler, xssAPI,
        gqlPredicateGroup);
  }

  /**
   * Performs customization in different predicates as per the requirement
   *
   * @param predicateGroup root PredicateGroup to search for predicates
   * @return set of all predicates types used in given predicateGroup
   */
  private Set<String> customizePredicateGroup(PredicateGroup predicateGroup) {
    if (predicateGroup == null) {
      return new HashSet<>();
    }
    Set<String> predicateSet = new HashSet<>();
    for (int i = 0; i < predicateGroup.size(); i++) {
      if (predicateGroup.get(i) instanceof PredicateGroup) {
        // if its a PredicateGroup traverse deeper
        PredicateGroup optionalGrp = (PredicateGroup) predicateGroup.get(i);
        predicateSet.addAll(customizePredicateGroup(optionalGrp));
        predicateGroup.set(i, optionalGrp);
      } else if (predicateGroup.get(i) instanceof Predicate) {
        // if its a predicate then perform modification as needed
        Predicate predicate = predicateGroup.get(i);
        predicateSet.add(predicate.getType());
        modifyPredicate(predicateGroup, predicate, i);
      }
    }
    return predicateSet;
  }

  /**
   * Modifies the given predicate as per need and set back to parentGroup at given index
   *
   * @param parentGroup root PredicateGroup containing predicate
   * @param predicate predicate to be modified
   * @param index index at which predicate is present in parentGroup
   */
  private void modifyPredicate(PredicateGroup parentGroup, Predicate predicate, int index) {
    String property = predicate.get("property");
    if (JcrPropertyPredicateEvaluator.PROPERTY.equals(predicate.getType())) {
      // if predicate type is property
      String value = predicate.get("value");
      if (property.equals(TAGS_PROP) || property.equals("tag")) {
        // if property is tags or tag resolve the tag using TagManager
        predicate.set(JcrPropertyPredicateEvaluator.PROPERTY, CQ_TAGS_PROP);
        parentGroup.set(index, predicate);
        if (tagManager != null) {
          Tag tag = tagManager.resolve(value);
          if (tag == null) {
            // if tag not resolved search tag by title and reset in parentGroup
            Predicate tagSearchPredicate = new Predicate(TagSearchPredicateEvaluator.TYPE);
            tagSearchPredicate.set(JcrPropertyPredicateEvaluator.PROPERTY, CQ_TAGS_PROP);
            tagSearchPredicate.set(TagSearchPredicateEvaluator.TYPE, value);
            parentGroup.set(index, tagSearchPredicate);
          }
        }
      } else {
        property = revertWildcards(property);
        int depth = StringUtils.countMatches(property, PROPERTY_STEP);
        if (depth > 0) {
          predicate.set(JcrPropertyPredicateEvaluator.PROPERTY,
              property.replace(PROPERTY_STEP, ""));
          predicate.set(JcrPropertyPredicateEvaluator.DEPTH, String.valueOf(depth));
          parentGroup.set(index, predicate);
        }
      }
    } else if (FulltextPredicateEvaluator.FULLTEXT.equals(predicate.getType())) {
      // for fulltext query applying its union with TagsearchPredicate to include tag titles in
      // fulltext search
      String fulltext = predicate.get(FulltextPredicateEvaluator.FULLTEXT, "");
      if (fulltext != null) {
        fulltext = revertWildcards(fulltext);
        predicate.set(FulltextPredicateEvaluator.FULLTEXT, fulltext);

        Predicate tagSearchPredicate = new Predicate(TagSearchPredicateEvaluator.TYPE);
        tagSearchPredicate.set(JcrPropertyPredicateEvaluator.PROPERTY, CQ_TAGS_PROP);
        tagSearchPredicate.set(TagSearchPredicateEvaluator.TYPE, fulltext);
        PredicateGroup optionalGrp = new PredicateGroup();
        optionalGrp.setAllRequired(false);
        optionalGrp.add(predicate);
        optionalGrp.add(tagSearchPredicate);
        parentGroup.set(index, optionalGrp);
      }
    }
  }

  // ----------------------< ViewQuery impl >---------------------------------

  protected static class GQLViewQuery implements ViewQuery {

    protected final PredicateGroup predicateGroup;
    private final XSSAPI xssAPI;
    private final OmniSearchHandler omniSearchHandler;
    ResourceResolver resolver;

    public GQLViewQuery(ResourceResolver resolver, OmniSearchHandler omniSearchHandler,
        XSSAPI xssAPI, PredicateGroup predicateGroup) {
      this.omniSearchHandler = omniSearchHandler;
      this.resolver = resolver;
      this.xssAPI = xssAPI;
      this.predicateGroup = predicateGroup;
    }

    public Collection<Hit> execute() {
      final List<Hit> hits = new ArrayList<>();
      final Map<String, Object> parameters =
          new HashMap(PredicateConverter.createMap(predicateGroup));

      SearchResult results = omniSearchHandler.getResults(resolver, parameters,
          NumberUtils.toInt(predicateGroup.get("limit"), 0),
          NumberUtils.toInt(predicateGroup.get("offset"), 20));


      // filter/prepare the result set
      Resource resource;
      final Iterator<Resource> resources = results.getResources();
      while (resources.hasNext()) {
        resource = resources.next();
        if (resource != null) {
          Product product = resource.adaptTo(Product.class);
          if (product != null) {
            hits.add(createHit(product, xssAPI));
          }
        }
      }
      return hits;
    }
  }
}
