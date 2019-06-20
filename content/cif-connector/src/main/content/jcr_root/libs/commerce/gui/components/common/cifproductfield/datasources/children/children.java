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

package libs.commerce.gui.components.common.cifproductfield.datasources.children;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.commons.collections.Transformer;
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

import com.adobe.cq.commerce.api.conf.CommerceBasePathsService;
import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.adobe.granite.ui.components.PagingIterator;
import com.adobe.granite.ui.components.ds.AbstractDataSource;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.EmptyDataSource;

public class children extends SlingSafeMethodsServlet {

    private static final String FILTER_PRODUCT = "product";
    private static final String FILTER_FOLDER_OR_PRODUCT = "folderOrProduct";
    private static final String FILTER_FOLDER_OR_PRODUCT_OR_VARIANT = "folderOrProductOrVariant";

    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
        throws ServletException, IOException {

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

            final Collection<Predicate> predicates = new ArrayList<Predicate>(2);
            predicates.add(createPredicate(filter));

            if (searchName != null) {
                final Pattern searchNamePattern = Pattern.compile(Pattern.quote(searchName), Pattern.CASE_INSENSITIVE);

                predicates.add(new Predicate() {
                    @Override
                    public boolean evaluate(Object o) {
                        Resource r = (Resource) o;
                        return searchNamePattern.matcher(r.getName()).lookingAt();
                    }
                });
            }

            final Predicate predicate = PredicateUtils.allPredicate(predicates);
            final Transformer transformer = createTransformer(itemRT, predicate);

            final List<Resource> list;
            if (FILTER_PRODUCT.equals(filter)) {
                class ProductFinder extends AbstractResourceVisitor {
                    private ProductPredicate productPredicate = new ProductPredicate();
                    List<Resource> products = new ArrayList<Resource>();

                    @Override
                    protected void visit(Resource res) {
                        if (productPredicate.evaluate(res)) {
                            products.add(res);
                        }
                    }
                }
                ;
                ProductFinder productFinder = new ProductFinder();
                productFinder.accept(parent);
                list = IteratorUtils.toList(new FilterIterator(productFinder.products.iterator(), predicate));
            } else {
                list = IteratorUtils.toList(new FilterIterator(parent.listChildren(), predicate));
            }

            @SuppressWarnings("unchecked")
            DataSource datasource = new AbstractDataSource() {

                public Iterator<Resource> iterator() {
                    Collections.sort(list, new Comparator<Resource>() {
                        public int compare(Resource o1, Resource o2) {
                            return o1.getName().compareTo(o2.getName());
                        }
                    });

                    return new TransformIterator(new PagingIterator<Resource>(list.iterator(), offset, limit), transformer);
                }
            };

            ds = datasource;
        }

        request.setAttribute(DataSource.class.getName(), ds);
    }

    private static Predicate createPredicate(String filter) {
        if (FILTER_PRODUCT.equals(filter)) {
            return new ProductPredicate();
        } else if (FILTER_FOLDER_OR_PRODUCT.equals(filter)) {
            return new FolderOrProductPredicate();
        } else if (FILTER_FOLDER_OR_PRODUCT_OR_VARIANT.equals(filter)) {
            return new FolderOrProductOrVariantPredicate();
        } else {
            //default
            return new FolderOrProductPredicate();
        }
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

    private static class ProductPredicate implements Predicate {
        @Override
        public boolean evaluate(Object object) {
            final Resource resource = (Resource) object;
            ValueMap valueMap = resource.getValueMap();

            if (!valueMap.containsKey("sling:resourceType") || !valueMap.containsKey("cq:commerceType"))
                return false;

            //product
            final boolean ret = resource.isResourceType("commerce/components/product") &&
                "product".equals(valueMap.get("cq:commerceType", String.class));

            return ret;
        }
    }

    private static class VariantPredicate implements Predicate {
        @Override
        public boolean evaluate(Object object) {
            final Resource resource = (Resource) object;
            ValueMap valueMap = resource.getValueMap();

            if (!valueMap.containsKey("sling:resourceType") || !valueMap.containsKey("cq:commerceType"))
                return false;

            //product
            final boolean ret = resource.isResourceType("commerce/components/product") &&
                "variant".equals(valueMap.get("cq:commerceType", String.class));

            return ret;
        }
    }

    private static class FolderOrProductPredicate implements Predicate {
        private final ProductPredicate productPredicate = new ProductPredicate();

        @Override
        public boolean evaluate(Object object) {
            final Resource resource = (Resource) object;

            if (resource.isResourceType("sling:Folder")
                || resource.isResourceType("sling:OrderedFolder")
                || resource.isResourceType(JcrConstants.NT_FOLDER))
                return true;

            return productPredicate.evaluate(object);
        }
    }

    private static class FolderOrProductOrVariantPredicate implements Predicate {
        private final ProductPredicate productPredicate = new ProductPredicate();
        private final VariantPredicate variantPredicate = new VariantPredicate();

        @Override
        public boolean evaluate(Object object) {
            final Resource resource = (Resource) object;

            if (resource.isResourceType("sling:Folder")
                || resource.isResourceType("sling:OrderedFolder")
                || resource.isResourceType(JcrConstants.NT_FOLDER))
                return true;

            return productPredicate.evaluate(object) || variantPredicate.evaluate(object);
        }
    }

    // TODO It can be extracted out to granite.ui.commons
    private static class PredicatedResourceWrapper extends ResourceWrapper {
        private Predicate predicate;

        public PredicatedResourceWrapper(Resource resource, Predicate predicate) {
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
            return new TransformIterator(new FilterIterator(super.listChildren(), predicate), new Transformer() {
                public Object transform(Object o) {
                    return new PredicatedResourceWrapper((Resource) o, predicate);
                }
            });
        }

        @Override
        public boolean hasChildren() {
            if (!super.hasChildren()) {
                return false;
            }
            return listChildren().hasNext();
        }
    }
}
