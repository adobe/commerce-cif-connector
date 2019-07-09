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
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.FalsePredicate;
import org.apache.commons.collections.functors.OrPredicate;
import org.apache.commons.collections.iterators.FilterIterator;
import org.apache.commons.collections.iterators.TransformIterator;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceWrapper;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

import com.adobe.cq.commerce.api.conf.CommerceBasePathsService;
import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.PagingIterator;
import com.adobe.granite.ui.components.ds.AbstractDataSource;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.EmptyDataSource;

@Component(
    service = Servlet.class,
    name = "CIFProductFieldChildrenDataSourceServlet",
    immediate = true,
    property = {
        "sling.servlet.resourceTypes=commerce/gui/components/common/cifproductfield/datasources/children",
        "sling.servlet.methods=GET"
    })
public class ChildrenDataSourceServlet extends SlingSafeMethodsServlet {
    enum Filter implements Predicate {
        product(PRODUCT_PREDICATE),
        folderOrProduct(FOLDER_PREDICATE, PRODUCT_PREDICATE),
        folderOrProductOrVariant(FOLDER_PREDICATE, PRODUCT_PREDICATE, VARIANT_PREDICATE);

        private final Predicate predicate;

        Filter(Predicate... predicates) {
            Predicate acc = FalsePredicate.INSTANCE;
            for (Predicate p : predicates) {
                acc = new OrPredicate(acc, p);
            }
            this.predicate = acc;
        }

        @Override
        public boolean evaluate(Object object) {
            return predicate.evaluate(object);
        }

        private static Predicate getFilter(String filter) {
            if (filter == null) {
                return Filter.folderOrProduct;
            }

            try {
                return Filter.valueOf(filter);
            } catch (IllegalArgumentException x) {
                return Filter.folderOrProduct;
            }
        }
    }

    private static Predicate FOLDER_PREDICATE = createFolderPredicate();
    private static Predicate PRODUCT_PREDICATE = createProductPredicate("product");
    private static Predicate VARIANT_PREDICATE = createProductPredicate("variant");

    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) {

        final SlingScriptHelper sling = getScriptHelper(request);

        final ExpressionHelper ex = new ExpressionHelper(sling.getService(ExpressionResolver.class), request);
        final Config dsCfg = new Config(request.getResource().getChild(Config.DATASOURCE));
        final CommerceBasePathsService cbps = sling.getService(CommerceBasePathsService.class);

        final String query = ex.getString(dsCfg.get("query", String.class));

        final String parentPath;
        final String searchName;

        if (query != null) {
            final String rootPath = ex.getString(dsCfg.get("rootPath", cbps.getProductsBasePath()));

            final int slashIndex = query.lastIndexOf('/');
            if (slashIndex < 0) {
                parentPath = rootPath;
                searchName = query.toLowerCase();
            } else if (!query.startsWith(rootPath)) {
                parentPath = rootPath;
                searchName = null;
            } else if (slashIndex == query.length() - 1) {
                parentPath = query;
                searchName = null;
            } else {
                parentPath = query.substring(0, slashIndex + 1);
                searchName = query.substring(slashIndex + 1).toLowerCase();
            }
        } else {
            parentPath = ex.getString(dsCfg.get("path", String.class));
            searchName = null;
        }

        final Resource parent = request.getResourceResolver().getResource(parentPath);

        final DataSource ds;
        if (parent == null) {
            ds = EmptyDataSource.instance();
        } else {
            final Integer offset = ex.get(dsCfg.get("offset", String.class), Integer.class);
            final Integer limit = ex.get(dsCfg.get("limit", String.class), Integer.class);
            final String itemRT = dsCfg.get("itemResourceType", String.class);
            final String filter = ex.getString(dsCfg.get("filter", String.class));

            final Collection<Predicate> predicates = new ArrayList<>(2);
            predicates.add(Filter.getFilter(filter));

            if (searchName != null) {
                final Pattern searchNamePattern = Pattern.compile(Pattern.quote(searchName), Pattern.CASE_INSENSITIVE);
                predicates.add(o -> searchNamePattern.matcher(((Resource) o).getName()).lookingAt());
            }

            final Predicate predicate = PredicateUtils.allPredicate(predicates);
            final Transformer transformer = createTransformer(itemRT, predicate);

            final List<Resource> list;
            if (Filter.product.name().equals(filter)) {
                class ProductFinder extends AbstractResourceVisitor {
                    private List<Resource> products = new ArrayList<>();

                    @Override
                    protected void visit(Resource res) {
                        if (Filter.product.evaluate(res)) {
                            products.add(res);
                        }
                    }
                }
                ProductFinder productFinder = new ProductFinder();
                productFinder.accept(parent);
                list = IteratorUtils.toList(new FilterIterator(productFinder.products.iterator(), predicate));
            } else {
                list = IteratorUtils.toList(new FilterIterator(parent.listChildren(), predicate));
            }

            @SuppressWarnings("unchecked")
            DataSource datasource = new AbstractDataSource() {

                public Iterator<Resource> iterator() {
                    list.sort(Comparator.comparing(Resource::getName));

                    return new TransformIterator(new PagingIterator<>(list.iterator(), offset, limit), transformer);
                }
            };

            ds = datasource;
        }

        request.setAttribute(DataSource.class.getName(), ds);
    }

    private static Transformer createTransformer(final String itemRT, final Predicate predicate) {
        return new Transformer() {
            public Object transform(Object o) {
                Resource r = ((Resource) o);

                return new PredicatedResourceWrapper(r, predicate) {
                    @Override
                    public String getResourceType() {
                        if (itemRT == null) {
                            return super.getResourceType();
                        }
                        return itemRT;
                    }
                };
            }
        };
    }

    private static SlingScriptHelper getScriptHelper(ServletRequest request) {
        SlingBindings bindings = (SlingBindings) request.getAttribute(SlingBindings.class.getName());
        return bindings.getSling();
    }

    private static Predicate createFolderPredicate() {
        return object -> {
            Resource resource = (Resource) object;
            return resource.isResourceType("sling:Folder")
                || resource.isResourceType("sling:OrderedFolder")
                || resource.isResourceType(JcrConstants.NT_FOLDER);
        };
    }

    private static Predicate createProductPredicate(String commerceType) {
        return object -> {
            final Resource resource = (Resource) object;
            ValueMap valueMap = resource.getValueMap();

            return valueMap.containsKey("sling:resourceType") &&
                resource.isResourceType("commerce/components/product") &&
                valueMap.containsKey("cq:commerceType") &&
                commerceType.equals(valueMap.get("cq:commerceType", String.class));
        };
    }

    private static class PredicatedResourceWrapper extends ResourceWrapper {
        private Predicate predicate;

        PredicatedResourceWrapper(Resource resource, Predicate predicate) {
            super(resource);
            this.predicate = predicate;
        }

        @Override
        public Resource getChild(String relPath) {
            Resource child = super.getChild(relPath);

            if (child == null || !predicate.evaluate(child)) {
                return null;
            }

            return new PredicatedResourceWrapper(child, predicate);
        }

        @SuppressWarnings("unchecked")
        @Override
        public Iterator<Resource> listChildren() {
            return new TransformIterator(new FilterIterator(super.listChildren(), predicate), o -> new PredicatedResourceWrapper(
                (Resource) o, predicate));
        }

        @Override
        public boolean hasChildren() {
            return super.hasChildren() && listChildren().hasNext();
        }
    }
}
