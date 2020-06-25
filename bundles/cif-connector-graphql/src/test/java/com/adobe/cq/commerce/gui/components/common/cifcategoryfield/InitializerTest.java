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

package com.adobe.cq.commerce.gui.components.common.cifcategoryfield;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.resource.ValueMap;
import org.junit.Test;

import com.adobe.cq.commerce.graphql.search.CatalogSearchSupport;
import com.adobe.cq.commerce.gui.components.common.FieldInitializerTest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class InitializerTest extends FieldInitializerTest {
    private Initializer initializer = new Initializer();

    @Test
    public void testDefaults() {

        initializer.init(bindings);

        assertNotNull(includedResourceSample);

        ValueMap properties = includedResourceSample.getValueMap();
        assertNotNull(properties);
        assertEquals("folderOrCategory", properties.get("filter", String.class));
        assertEquals("Category ID", properties.get("emptyText", String.class));
        assertFalse(properties.get("multiple", Boolean.class));
        assertEquals("granite/ui/components/coral/foundation/form/pathfield", properties.get("sling:resourceType", String.class));
        assertEquals(PRODUCTS_BASE_PATH, properties.get("rootPath", String.class));
        assertTrue(properties.get("forceselection", Boolean.class));
        String pickerSrc = "/mnt/overlay/commerce/gui/content/common/cifcategoryfield/picker.html?root=products%2fbase%2fpath&"
            + "filter=folderOrCategory&selectionCount=single&selectionId=id";
        assertEquals(pickerSrc, properties.get("pickerSrc", String.class));
    }

    private void testRootPath() {
        initializer.init(bindings);
        assertNotNull(includedResourceSample);
        ValueMap properties = includedResourceSample.getValueMap();
        assertNotNull(properties);
        assertEquals("catalog/path", properties.get("rootPath", String.class));
    }

    @Test
    public void testCatalogPathForComponentDialog() {
        when(request.getRequestURI()).thenReturn(CatalogSearchSupport.COMPONENT_DIALOG_URI_MARKER);
        when(requestPathInfo.getSuffix()).thenReturn("component/path");
        contentResourceProperties.put(CatalogSearchSupport.PN_CATALOG_PATH, "catalog/path");
        testRootPath();
    }

    @Test
    public void testCatalogPathForPageProperties() {
        when(request.getRequestURI()).thenReturn(CatalogSearchSupport.PAGE_PROPERTIES_URI_MARKER);
        when(request.getParameter("item")).thenReturn("component/path");
        contentResourceProperties.put(CatalogSearchSupport.PN_CATALOG_PATH, "catalog/path");
        testRootPath();
    }

    @Test
    public void testContextAwareConfiguration() {
        when(request.getRequestURI()).thenReturn(CatalogSearchSupport.COMPONENT_DIALOG_URI_MARKER);
        when(requestPathInfo.getSuffix()).thenReturn("component/path");
        Map<String, Object> map = Collections.singletonMap(CatalogSearchSupport.PN_CATALOG_PATH, "catalog/path");
        setupContextAwareConfiguration(map);
        testRootPath();
    }

    @Test
    public void testContextAwareConfigurationWithoutCatalogPath() {
        when(request.getRequestURI()).thenReturn(CatalogSearchSupport.COMPONENT_DIALOG_URI_MARKER);
        when(requestPathInfo.getSuffix()).thenReturn("component/path");

        // The implementation will fallback to the "old" way of getting the property
        Map<String, Object> map = Collections.emptyMap();
        contentResourceProperties.put(CatalogSearchSupport.PN_CATALOG_PATH, "catalog/path");

        setupContextAwareConfiguration(map);
        testRootPath();
    }
}
