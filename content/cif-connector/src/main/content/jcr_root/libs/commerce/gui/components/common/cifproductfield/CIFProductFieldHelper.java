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

package libs.commerce.gui.components.common.cifproductfield;

import org.apache.sling.api.resource.Resource;
import com.adobe.cq.sightly.WCMUsePojo;
import com.day.cq.commons.jcr.JcrConstants;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

public class CIFProductFieldHelper extends WCMUsePojo {

    private String selectionId;

    @Override
    public void activate() throws Exception {
        String st = getRequest().getParameter("selectionId");
        if ("id".equals(st) || "sku".equals(st) || "path".equals(st) || "slug".equals(st)) {
            selectionId = st;
        } else {
            selectionId = "id";
        }
    }

    public boolean isCloudFolder() {
        Resource resource = getResource();

        return (isVirtual(resource) || isCloudBoundFolder(resource)) && (resource.isResourceType("sling:Folder")
                || resource.isResourceType("sling:OrderedFolder")
                || resource.isResourceType(JcrConstants.NT_FOLDER));
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
            return node.hasProperty("cq:catalogDataResourceProviderFactory");
        } catch (RepositoryException ex) {
            return false;
        }
    }

    public String getSelectionId() {
        if ("path".equals(selectionId)) {
            return getResource().getPath();
        }
        if ("slug".equals(selectionId)) {
            return getProperties().get("slug", String.class);
        }
        if ("sku".equals(selectionId)) {
            return getProperties().get("sku", String.class);
        }
        return getProperties().get("identifier", String.class);
    }

}
