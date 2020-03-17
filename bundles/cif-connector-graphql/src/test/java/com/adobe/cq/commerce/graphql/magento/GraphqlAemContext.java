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

package com.adobe.cq.commerce.graphql.magento;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import io.wcm.testing.mock.aem.junit.AemContext;
import io.wcm.testing.mock.aem.junit.AemContextBuilder;

import static org.apache.sling.testing.mock.caconfig.ContextPlugins.CACONFIG;

public final class GraphqlAemContext {

    public final static String CATALOG_IDENTIFIER = "my-catalog";

    private GraphqlAemContext() {}

    public static AemContext createContext(Map<String, String> contentPaths) {

        AemContext context = new AemContextBuilder(ResourceResolverType.JCR_MOCK).plugin(CACONFIG)
            .beforeSetUp(ctx -> {
                ConfigurationAdmin configurationAdmin = ctx.getService(ConfigurationAdmin.class);
                Configuration serviceConfiguration = configurationAdmin.getConfiguration(
                    "org.apache.sling.caconfig.resource.impl.def.DefaultContextPathStrategy");

                Dictionary<String, Object> props = new Hashtable<>();
                props.put("configRefResourceNames", new String[] { ".", "jcr:content" });
                props.put("configRefPropertyNames", "cq:conf");
                serviceConfiguration.update(props);

                serviceConfiguration = configurationAdmin.getConfiguration(
                    "org.apache.sling.caconfig.resource.impl.def.DefaultConfigurationResourceResolvingStrategy");
                props = new Hashtable<>();
                props.put("configPath", "/conf");
                serviceConfiguration.update(props);

                serviceConfiguration = configurationAdmin.getConfiguration(
                    "org.apache.sling.caconfig.impl.ConfigurationResolverImpl");
                props = new Hashtable<>();
                props.put("configBucketNames", new String[] { "settings" });
                serviceConfiguration.update(props);
            })
            .build();
        // Load page structure
        contentPaths.entrySet().iterator().forEachRemaining(entry -> {
            context.load().json(entry.getValue(), entry.getKey());
        });
        return context;

    }

}
