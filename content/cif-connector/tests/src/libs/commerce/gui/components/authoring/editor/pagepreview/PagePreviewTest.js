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

'use strict';

describe('PagePreview', () => {
    var handleProductPreview = window.CIF.PagePreview.handleProductPreview;
    var handleCategoryPreview = window.CIF.PagePreview.handleCategoryPreview;

    it('Handling preview does nothing for invalid selections', () => {
        sinon.spy(window, 'open');

        handleProductPreview(null, null);
        assert.isFalse(window.open.calledOnce);

        handleProductPreview(null, {});
        assert.isFalse(window.open.calledOnce);

        handleProductPreview(null, { selections: [] });
        assert.isFalse(window.open.calledOnce);

        handleCategoryPreview(null, { selections: 'any' });
        assert.isFalse(window.open.calledOnce);

        window.open.restore();
    });

    it('handleProductPreview() opens product page preview for the first selection', () => {
        var productPreviewServletUrl = '/bin/wcm/cif.previewproduct.html';

        sinon.spy(window, 'open');

        handleProductPreview(null, { selections: { value: 'slug' } });
        assert.isTrue(window.open.calledWith(productPreviewServletUrl + '?url_key=slug&sku=slug'));

        handleProductPreview(null, { selections: { value: 'slug#variant' } });
        assert.isTrue(window.open.calledWith(productPreviewServletUrl + '?url_key=slug&sku=slug&variant_sku=variant'));

        handleProductPreview(null, { selections: [{ value: 'slug' }] });
        assert.isTrue(window.open.calledWith(productPreviewServletUrl + '?url_key=slug&sku=slug'));

        handleProductPreview(null, { selections: [{ value: 'slug' }, { value: 'slug2' }] });
        assert.isTrue(window.open.calledWith(productPreviewServletUrl + '?url_key=slug&sku=slug'));

        window.open.restore();
    });

    it('handleCategoryPreview() opens category page preview for the first selection', () => {
        var categoryPreviewServletUrl = '/bin/wcm/cif.previewcategory.html';

        sinon.spy(window, 'open');

        handleCategoryPreview(null, { selections: { value: 'slug' } });
        assert.isTrue(window.open.calledWith(categoryPreviewServletUrl + '?id=slug'));

        handleCategoryPreview(null, { selections: [{ value: 'slug' }] });
        assert.isTrue(window.open.calledWith(categoryPreviewServletUrl + '?id=slug'));

        handleCategoryPreview(null, { selections: [{ value: 'slug' }, { value: 'slug2' }] });
        assert.isTrue(window.open.calledWith(categoryPreviewServletUrl + '?id=slug'));

        window.open.restore();
    });
});
