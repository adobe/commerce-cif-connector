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

package libs.commerce.gui.components.common.cifproductfield.columnitempreview;

import com.adobe.cq.sightly.WCMUsePojo;

import org.apache.sling.api.resource.Resource;
import com.adobe.granite.ui.components.ExpressionHelper;
import com.adobe.granite.ui.components.ExpressionResolver;


public class ColumnItemPreviewHelper extends WCMUsePojo  {
    private Resource itemResource;

    @Override
    public void activate() {
        final ExpressionHelper ex = new ExpressionHelper(getSlingScriptHelper().getService(ExpressionResolver.class), getRequest());
        String path = ex.getString(getProperties().get("path", String.class));
        itemResource  = getResourceResolver().getResource(path);
    }
    
    public Resource getItemResource() {
        return itemResource;
    }
}
