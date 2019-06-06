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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.adobe.cq.commerce.virtual.catalog.data.CatalogIdentifierService;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.EmptyDataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;

/**
 * A {@link DataSource} implementation that retrieves a list of catalog identifiers
 */
@Component(immediate = true, service = Servlet.class, property = {
    "sling.servlet.resourceTypes=commerce/gui/components/admin/products/bindproducttreewizard/catalogidentifierdatasource"})
public class CatalogIdentifierDatasource extends SlingSafeMethodsServlet {

  @Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY,
      cardinality = ReferenceCardinality.OPTIONAL, bind = "bindCatalogIdentifierService",
      unbind = "unbindCatalogIdentifierService")
  private CatalogIdentifierService catalogIdentifierService;

  protected void bindCatalogIdentifierService(CatalogIdentifierService service,
      Map<String, String> props) {
    this.catalogIdentifierService = service;
  }

  protected void unbindCatalogIdentifierService(CatalogIdentifierService service) {

  }

  @Override
  protected void doGet(@Nonnull SlingHttpServletRequest request,
      @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
    DataSource ds;

    if (catalogIdentifierService == null) {
      ds = EmptyDataSource.instance();
    } else {
      ds = buildValuesList(request.getResourceResolver());
    }

    request.setAttribute(DataSource.class.getName(), ds);
  }

  private DataSource buildValuesList(ResourceResolver resourceResolver) {
    Map<String, Collection<String>> identifiersMap =
        catalogIdentifierService.getCatalogIdentifiersForAllCommerceProviders();

    List<String> providerIdentifiers = flattenMapToList(identifiersMap);
    List<Resource> syntheticResources = generateResources(resourceResolver, providerIdentifiers);

    return toDataSource(syntheticResources);
  }

  protected List<Resource> generateResources(ResourceResolver resolver, List<String> properties) {
    return properties.stream().map(property -> {
      Map<String, Object> resourceProperties = new HashMap<String, Object>() {
        {
          put("text", property);
          put("value", property);
        }
      };
      ValueMapResource syntheticResource = new ValueMapResource(resolver, new ResourceMetadata(),
          "", new ValueMapDecorator(resourceProperties));
      return syntheticResource;
    }).collect(Collectors.toList());
  }

  protected List<String> flattenMapToList(Map<String, Collection<String>> mapOfCollections) {
    List<String> flatList = new ArrayList<>();

    mapOfCollections.forEach((provider, identifiers) -> {
      identifiers.stream().forEach(identifier -> flatList.add(provider + ":" + identifier));
    });

    return flatList;
  }


  protected DataSource toDataSource(List<Resource> resources) {
    return resources.isEmpty() ? EmptyDataSource.instance()
        : new SimpleDataSource(resources.iterator());
  }
}
