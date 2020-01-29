/*******************************************************************************
 *
 *    Copyright 2020 Adobe. All rights reserved.
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

package com.adobe.cq.commerce.gui;

/**
 * Constants related to the CIF Connector
 */
public abstract class Constants {

    public static String CONF_ROOT = "/conf";

    public static String CONF_CONTAINER_BUCKET_NAME = "settings";

    public static String CLOUDCONFIG_BUCKET_NAME = "cloudconfigs";

    public static String CLOUDCONFIG_BUCKET_PATH = CONF_CONTAINER_BUCKET_NAME + "/" + CLOUDCONFIG_BUCKET_NAME;

    private Constants() {

    }

}
