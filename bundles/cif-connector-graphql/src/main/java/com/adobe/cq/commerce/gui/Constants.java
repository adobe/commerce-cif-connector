package com.adobe.cq.commerce.gui;

/**
 * Constants related to the CIF Connector
 */
public abstract class Constants {

    public static String CONF_ROOT = "/conf";

    public static String CONF_CONTAINER_BUCKET_NAME="settings";

    public static String CLOUDCONFIG_BUCKET_NAME="cloudconfigs";

    public static String CLOUDCONFIG_BUCKET_PATH = CONF_CONTAINER_BUCKET_NAME + "/" + CLOUDCONFIG_BUCKET_NAME;

    private Constants() {

    }

}
