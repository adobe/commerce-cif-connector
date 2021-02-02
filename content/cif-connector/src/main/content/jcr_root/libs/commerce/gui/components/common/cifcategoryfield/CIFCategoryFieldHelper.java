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

package libs.commerce.gui.components.common.cifcategoryfield;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.cq.sightly.WCMUsePojo;
import com.day.cq.commons.jcr.JcrConstants;

public class CIFCategoryFieldHelper extends WCMUsePojo {
    //to be part of the public API of virtual bundle
    private static final String CQ_CATALOG_DATA_RESOURCE_PROVIDER_FACTORY = "cq:catalogDataResourceProviderFactory";
    private String selectionId;

    @Override
    public void activate() throws Exception {
        if (getProperties().get("isError", false)) {
            getResponse().sendError(500);
        }

        String st = getRequest().getParameter("selectionId");
        if ("id".equals(st) || "path".equals(st) || "idAndUrlPath".contentEquals(st)) {
            selectionId = st;
        } else {
            selectionId = "id";
        }
    }

    public boolean isCategory() {
        return isCategory(getResource());
    }

    public boolean hasChildren() {
        Resource resource = getResource();
        ValueMap valueMap = resource.getValueMap();
        if (valueMap != null && valueMap.containsKey("isLeaf") && valueMap.get("isLeaf").equals(true)) {
            return false;
        }
        return true;
    }

    public boolean isFolder() {
        return isFolder(getResource());
    }

    public boolean isCloudFolder() {
        Resource resource = getResource();

        return (isVirtual(resource) || isCloudBoundFolder(resource)) && (resource.isResourceType("sling:Folder")
                || resource.isResourceType("sling:OrderedFolder")
                || resource.isResourceType(JcrConstants.NT_FOLDER));
    }

    public String getTitle() {
        String title = getProperties().get("jcr:title", "");
        if (StringUtils.isBlank(title)) {
            title = getResource().getName();
        }
        return title;
    }

    public String getThumbnailImageUrl() {
        return getImageUrl(48);
    }

    public String getSelectionId() {
        if ("id".equals(selectionId)) {
            return getProperties().get("cifId", String.class);
        } else if ("path".equals(selectionId)) {
            return getResource().getPath();
        } else if ("idAndUrlPath".equals(selectionId)) {
            return getProperties().get("cifId", String.class) + "|" + getProperties().get("urlPath", String.class);
        } else {
            return getProperties().get("cifId", String.class);
        }
    }

    private boolean isCategory(Resource resource) {
        ValueMap valueMap = resource.getValueMap();

        if (!valueMap.containsKey("sling:resourceType") || !valueMap.containsKey("cq:commerceType"))
            return false;


        final boolean ret = resource.isResourceType("sling:Folder") &&
                "category".equals(valueMap.get("cq:commerceType", String.class));

        return ret;
    }

    private boolean isFolder(Resource resource ) {
        return resource.isResourceType("sling:Folder")
                || resource.isResourceType("sling:OrderedFolder")
                || resource.isResourceType(JcrConstants.NT_FOLDER);
    }

    private boolean isVirtual(Resource resource) {
        Node node = resource.adaptTo(Node.class);
        return node == null;
    }

    private boolean isCloudBoundFolder(Resource resource) {
        if (isVirtual(resource)) {
            return false;
        }

        Node node = resource.adaptTo(Node.class);
        try {
            return node.hasProperty(CQ_CATALOG_DATA_RESOURCE_PROVIDER_FACTORY);
        } catch (RepositoryException ex) {
            return false;
        }
    }
    
    private String getImageUrl(int size) {
        int ck = 600000 + (int) (Math.random() * (600001));
        String imageUrl = getRequest().getContextPath() + getResource().getPath() + ".folderthumbnail.jpg?width=" + size + "&height=" + size + "&ch_ck=" + ck;
        return imageUrl;
    }
}
