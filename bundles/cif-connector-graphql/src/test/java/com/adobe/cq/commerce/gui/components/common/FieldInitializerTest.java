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

package com.adobe.cq.commerce.gui.components.common;

import java.util.HashMap;

import javax.script.SimpleBindings;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.junit.Before;

import com.adobe.cq.commerce.api.conf.CommerceBasePathsService;
import com.adobe.granite.ui.components.ExpressionResolver;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.scripting.WCMBindingsConstants;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FieldInitializerTest {
    protected static final String PRODUCTS_BASE_PATH = "products/base/path";

    protected SimpleBindings bindings = new SimpleBindings();
    protected Resource includedResourceSample;
    protected ModifiableValueMapDecorator valueMap;
    protected ValueMap contentResourceProperties;
    protected RequestPathInfo requestPathInfo;

    @Before
    public void setUp() {
        SlingScriptHelper sling = mock(SlingScriptHelper.class);
        bindings.put(SlingBindings.SLING, sling);
        ExpressionResolver expressionResolver = mock(ExpressionResolver.class);
        when(expressionResolver.resolve(anyString(), any(), any(), (SlingHttpServletRequest) any())).thenAnswer(
            invocationOnMock -> invocationOnMock.getArguments()[0]);
        when(sling.getService(ExpressionResolver.class)).thenReturn(expressionResolver);
        CommerceBasePathsService pathService = mock(CommerceBasePathsService.class);
        when(pathService.getProductsBasePath()).thenReturn(PRODUCTS_BASE_PATH);
        when(sling.getService(CommerceBasePathsService.class)).thenReturn(pathService);
        doAnswer(invocationOnMock -> {
            includedResourceSample = (Resource) invocationOnMock.getArguments()[0];
            return null;
        }).when(sling).include(any(Resource.class));
        SlingHttpServletRequest request = mock(SlingHttpServletRequest.class);
        bindings.put(SlingBindings.REQUEST, request);
        requestPathInfo = mock(RequestPathInfo.class);
        when(request.getRequestPathInfo()).thenReturn(requestPathInfo);
        valueMap = new ModifiableValueMapDecorator(new HashMap<>());
        bindings.put(WCMBindingsConstants.NAME_PROPERTIES, valueMap);

        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        when(request.getResourceResolver()).thenReturn(resourceResolver);
        PageManager pageManager = mock(PageManager.class);
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        Page page = mock(Page.class);
        when(pageManager.getContainingPage(anyString())).thenReturn(page);
        Resource contentResource = mock(Resource.class);
        contentResourceProperties = new ModifiableValueMapDecorator(new HashMap<>());
        when(contentResource.getValueMap()).thenReturn(contentResourceProperties);
        when(page.getContentResource()).thenReturn(contentResource);
    }
}
