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

import java.util.Calendar;

import com.adobe.cq.commerce.common.CommerceHelper;
import com.day.cq.commons.Externalizer;
import org.apache.sling.api.resource.Resource;

import com.adobe.cq.commerce.api.Product;
import com.adobe.cq.sightly.WCMUsePojo;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.i18n.I18n;
import org.apache.sling.api.resource.ResourceResolver;

public class ViewHelper extends WCMUsePojo {

    public static final String DEFAULT_THUMBNAIL_PATH = "/libs/commerce/gui/components/admin/resources/thumbnail.png";

    private Product product = null;
    private I18n i18n = null;
    private boolean deactivated;
    private Calendar publishedTime = null;
    private Calendar modifiedTime = null;
    private String thumbnailImageUrl = null;
    private String cardImageUrl = null;
    private ResourceResolver resolver;
    private Externalizer externalizer;

    @Override
    public void activate() throws Exception {
        product = getResource().adaptTo(Product.class);

        i18n = new I18n(getRequest());

        String lastReplicationAction = getProperties().get("cq:lastReplicationAction", String.class);
        boolean deactivated = "Deactivate".equals(lastReplicationAction);

        publishedTime = getProperties().get("cq:lastReplicated", Calendar.class);
        modifiedTime = getProperties().get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);

        resolver = getResourceResolver();
        externalizer = resolver.adaptTo(Externalizer.class);
    }

    public boolean isFolder() {
        Resource resource = getResource();

        return resource.isResourceType("sling:Folder")
                || resource.isResourceType("sling:OrderedFolder")
                || resource.isResourceType(JcrConstants.NT_FOLDER);
    }

    public boolean isProduct() {
        return product != null;
    }

    public boolean isNew() {
        Calendar modifiedDateRaw = getProperties().get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
        Calendar createdDateRaw = getProperties().get(JcrConstants.JCR_CREATED, Calendar.class);
        if ((createdDateRaw == null) || (modifiedDateRaw != null && modifiedDateRaw.before(createdDateRaw))) {
            createdDateRaw = modifiedDateRaw;
        }
        Calendar twentyFourHoursAgo = Calendar.getInstance();
        twentyFourHoursAgo.add(Calendar.DATE, -1);
        return createdDateRaw != null && twentyFourHoursAgo.before(createdDateRaw);
    }

    public String getTitle() {
        return isProduct() ? product.getTitle() : CommerceHelper.getCardTitle(getResource(), getPageManager());
    }

    public String getThumbnailImageUrl() {
        if (thumbnailImageUrl == null) {
            thumbnailImageUrl = getImageUrl(48);
        }
        if (thumbnailImageUrl == null) {
            return getDefaultThumbnail();
        }
        return thumbnailImageUrl;
    }

    public String getCardImageUrl() {
        if (cardImageUrl == null) {
            cardImageUrl = getImageUrl(240);
        }
        if (cardImageUrl == null) {
            return getDefaultThumbnail();
        }
        return cardImageUrl;
    }

    public String getDefaultThumbnail() {
        String imagePath = resolver.resolve(DEFAULT_THUMBNAIL_PATH).getPath();
        return externalizer.externalLink(resolver, Externalizer.LOCAL, imagePath);
    }

    public String getSku() {
        return isProduct() ? product.getSKU() : "";
    }

    public long getModifiedTimeMillis() {
        if (modifiedTime == null) {
            return 0;
        }
        return modifiedTime.getTimeInMillis();
    }

    public String getModifiedTime() {
        if (modifiedTime == null) {
            return null;
        }
        return modifiedTime.toInstant().toString();
    }

    public boolean isPublished() {
        return !deactivated && publishedTime != null;
    }

    public long publishedTimeMillis() {
        if (publishedTime == null) {
            return 0;
        }
        return publishedTime.getTimeInMillis();
    }

    public String getPublishedTime() {
        if (publishedTime == null) {
            return null;
        }
        return publishedTime.toInstant().toString();
    }

    private String getImageUrl(int size) {
        boolean manualThumnailExists = false;
        String imageUrl;
        int ck = 600000 + (int) (Math.random() * (600001));
        if (isProduct()) {
            imageUrl = product.getThumbnailUrl(size);
            if (imageUrl != null && (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://"))) {
                imageUrl = getRequest().getContextPath() + imageUrl + "/manualThumbnail?ch_ck=" + ck;
            }
        } else {
            imageUrl = getResource().getPath();
            if (imageUrl != null) {
                imageUrl = getRequest().getContextPath() + imageUrl + ".folderthumbnail.jpg?width=" + size + "&height=" + size + "&ch_ck=" + ck;
            }
        }

        return imageUrl;
    }
}
