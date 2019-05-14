[![CircleCI](https://circleci.com/gh/adobe/commerce-cif-connector.svg?style=svg)](https://circleci.com/gh/adobe/commerce-cif-connector)
[![codecov](https://codecov.io/gh/adobe/commerce-cif-connector/branch/master/graph/badge.svg)](https://codecov.io/gh/adobe/commerce-cif-connector)
[![Maven Central](https://img.shields.io/maven-central/v/com.adobe.commerce.cif/cif-connector-all.svg)](https://search.maven.org/search?q=g:com.adobe.commerce.cif%20AND%20a:cif-connector-all)

# AEM Commerce connector for Magento and GraphQL

This is an AEM Commerce connector for Magento and GraphQL, that provides some integration of Magento products and categories in the AEM Commerce console, and some authoring features like product and category pickers.

This connector only provides authoring features, this is not meant to be used to develop frontend components. To develop AEM frontend components, refer to the [AEM CIF Core Components](https://github.com/adobe/aem-core-cif-components) project.

## Modules

The main parts of the project are:

* **bundles**: contains the following AEM bundles in the sub-folders
    * **cif-connector-graphql**: the CIF GraphQL connector, based on Magento GraphQL
    * **cif-virtual-catalog**: the bundle that permits to bind products in the AEM Commerce console
* **content**: contains the following content packages in the sub-folders
    * **cif-connector-graphql**: the content package for the CIF GraphQL connector
    * **cif-virtual-catalog**: the content package for the virtual catalog connector

## Installation

### Easy install with the "all" package

You can easily install all the modules of the connector and also its required dependencies with the [all](all) content package. If you want to use the latest released version, just download it with the Maven Central link located at the top of this README and install it in your running AEM instance.

If you want to build it yourself, just build all the modules and install the `all` content package by simply running the following command at the root of the repository:

```
mvn clean install -PautoInstallAll
```
This installs everything by default to `localhost:4502` without any context path. You can also configure the install location with the following maven properties:
* `aem.host`: the name of the AEM instance
* `aem.port`: the port number of the AEM instance
* `aem.contextPath`: the context path of your AEM instance (if not `/`)

## System Requirements

For simplicity, we only provide the version requirements of the `all` package. If you need to check the versions of other modules, simply checkout the corresponding `cif-connector-all-x.y.z` tag and check the versions of other modules in the corresponding POM files or in the POM of the `all` project.

| CIF Connector All   | AEM 6.4 | AEM 6.5 | Magento | Java |
|---------------------|---------|---------|---------|------|
| 0.1.0               | 6.4.4.0 | 6.5.0   | 2.3.1   | 1.8  |

## CIF Magento GraphQL Configuration

The CIF Magento GraphQL AEM commerce connector has to be configured to access your Magento instance and bind the catalog data. Follow the steps below to configure the bundle: 

1) Configure the generic GraphQL instance
    * Go to http://localhost:4502/system/console/configMgr
    * Look for `CIF GraphQL Client Configuration Factory`
    * Create a child configuration
        * Keep the `default` service identifier or set something custom. Make sure to use the same value in step 2) below.
        * For _GraphQL Service URL_ enter the URL of your Magento GraphQL endpoint (usually `https://hostname/graphql`)

2) Configuration of the connector
    * Go to http://localhost:4502/system/console/configMgr
    * Look for `CIF Catalog Magento GraphQL Configuration Factory`
    * Create a child configuration
        * For `Magento GraphQL Service Identifier` enter the ID of the GraphQL client you already configured (see "pre-requisites")
        * For `Magento root category id` enter the ID of the default root category of your Magento instance
        * For `Magento store view` enter the code of the Magento store view you want to use or keep "default"

3) Binding of product catalog to AEM resource tree
    * Go to AEM Commerce product console (http://localhost:4502/aem/products.html/var/commerce/products)
    * Click on Create > Bind Products
    * Enter Title, unique name and select `magento-graphql` as commerce provider
    * For "Project", you can select the ID of the Magento instance you created in the previous step

4) AEM content editor product drag & drop
    * To allow authors to drag & drop product assets from the AEM Assets Browser to a page a project specific configuration is needed to configure which component is used when dragging a product to a page. See AEM documentation about [Configuring a Paragraph System so that Dragging an Asset Creates a Component Instance](https://helpx.adobe.com/experience-manager/6-4/sites/developing/using/developing-components.html#ConfiguringaParagraphSystemsothatDragginganAssetCreatesaComponentInstance) for details.

## Usage
The project also provides product and category pickers to be used in any component dialog to select products or categories.

### Product Picker
To use the product picker a developer has to add `/libs/commerce/gui/components/common/cifproductfield` to a component dialog. For example use the following for the cq:dialog:

```
<product jcr:primaryType="nt:unstructured" fieldLabel="Product" name="./product" sling:resourceType="commerce/gui/components/common/cifproductfield"/>
```
The product field allows to navigation to the product a user want to select via the different views. A user also can use the integrated search to find a product. By default the product field will return the ID of the product, but this can be configured using the `selectionId` attribute.

The product picker field supports the following optional properties:
* `rootPath` - configure the root path of the virtual catalog data tree to be used (default = `/var/commerce/products`)
* `multiple` (true, false) - allows to select one or multiple products (default = false)
* `emptyText` - to configure the empty text value of the picker field
* `selectionId`(id, sku, slug, path) - allows to choose the product attribute to be returned by the picker (default = id)

### Category Picker
The category picker (provided by `/libs/commerce/gui/components/common/cifcategoryfield`) can be used in a component dialog as well. The following snippet can be used in a cq:dialog configuration:

```
<category jcr:primaryType="nt:unstructured" fieldLabel="Category" name="./category" sling:resourceType="commerce/gui/components/common/cifcategoryfield"/>
```

The category picker field supports the following optional properties:
* `rootPath` - configure the root path of the virtual catalog data tree to be used (default = `/var/commerce/products`)
* `multiple` (true, false) - allows to select one or multiple categories (default = false)
* `emptyText` - to configure the empty text value of the picker field
* `selectionId`(id, path) - allows to choose the category attribute to be returned by the picker (default = id)
 
## Building and installing from source

### Pre-requisites

If you build and install each module manually, the [magento-graphql](https://github.com/adobe/commerce-cif-magento-graphql) and [graphql-client](https://github.com/adobe/commerce-cif-graphql-client) bundles have to be installed in your AEM instance. You MUST also configure an instance of the GraphQL client, see the instructions on the corresponding repository to setup the client.

### Building and installing

To build all the modules run in the project root directory the following command with Maven 3:

```
mvn clean install
```

If you have a running AEM instance, you can also build and deploy all sub-projects into AEM with

```
mvn clean install -PautoInstall
```
This installs everything by default to `localhost:4502` without any context path. You can also configure the install location with the following maven properties:
* `aem.host`: the name of the AEM instance
* `aem.port`: the port number of the AEM instance
* `aem.contextPath`: the context path of your AEM instance (if not `/`)

## Releases to Maven Central

Releases are triggered by manually running `mvn release:prepare release:clean` on the `master` branch in one of the modules/subprojects of this repository. Once you choose the release and the next snapshot versions, this commits the change along with a release git tag like for example `cif-connector-all-x.y.z`. Note that the commits are not automatically pushed to the git repository, so you have some time to check your changes and then manually push them. The push then triggers a dedicated `CircleCI` build that performs the deployment of the tagged artifact to Maven Central.

Important: do **not** trigger releases from the reactor (top-level) project because we release each module independently.

### Contributing
 
Contributions are welcomed! Read the [Contributing Guide](.github/CONTRIBUTING.md) for more information.
 
### Licensing
 
This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.