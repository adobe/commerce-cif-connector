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

package com.adobe.cq.commerce.graphql.resource;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.commerce.common.ValueMapDecorator;
import com.day.cq.commons.DownloadResource;
import com.day.cq.commons.ImageResource;

public class SyntheticImageResource extends ImageResource {

    public static final String IMAGE_RESOURCE_TYPE = "commerce/components/product/image";

    /**
     * Creates an <code>ImageResource</code> based on a <code>SyntheticResource</code>.
     * This class returns the path of the SyntheticResource when {@link ImageResource#getFileReference()} is called.
     *
     * @param resolver The resource resolver.
     * @param path The resource path.
     * @param slingResourceType The sling resource type.
     * @param url The URL of the image.
     */
    public SyntheticImageResource(ResourceResolver resolver, String path, String slingResourceType, String url) {
        super(new SyntheticResource(resolver, path, slingResourceType){
            private ValueMap valueMap;
            {
                Map<String, Object> values = new HashMap<>();
                values.put(DownloadResource.PN_REFERENCE, url);
                valueMap = new ValueMapDecorator(values);
            }

            @Override
            public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                if (ValueMap.class.equals(type)) {
                    return type.cast(valueMap);
                } else {
                    return super.adaptTo(type);
                }
            }
        });
    }
}
