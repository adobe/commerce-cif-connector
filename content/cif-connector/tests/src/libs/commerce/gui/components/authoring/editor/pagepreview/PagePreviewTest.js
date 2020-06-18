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
    var handlePreview = window.CIF.PagePreview.handlePreview;
    var productPreviewServletUrl = '/bin/wcm/cif.previewproduct.html';
    var categoryPreviewServletUrl = '/bin/wcm/cif.previewcategory.html';

    it('handlePreview() does nothing for invalid selections or invalid preview Urls', () => {
        sinon.spy(window, 'open');

        handlePreview(null, productPreviewServletUrl);
        assert.isFalse(window.open.calledOnce);

        handlePreview({}, productPreviewServletUrl);
        assert.isFalse(window.open.calledOnce);

        handlePreview({ selections: [] }, productPreviewServletUrl);
        assert.isFalse(window.open.calledOnce);

        handlePreview({ selections: 'any' }, categoryPreviewServletUrl);
        assert.isFalse(window.open.calledOnce);

        handlePreview({ selections: { value: 'slug' } }, 'invalidPreviewUrl');
        assert.isFalse(window.open.calledOnce);

        window.open.restore();
    });

    it('handlePreview() opens product page preview for the first selection', () => {
        sinon.spy(window, 'open');

        handlePreview({ selections: { value: 'slug' } }, productPreviewServletUrl);
        assert.isTrue(window.open.calledWith(productPreviewServletUrl + '?url_key=slug&sku=slug'));

        handlePreview({ selections: { value: 'slug#variant' } }, productPreviewServletUrl);
        assert.isTrue(window.open.calledWith(productPreviewServletUrl + '?url_key=slug&sku=slug&variant_sku=variant'));

        handlePreview({ selections: [{ value: 'slug' }] }, productPreviewServletUrl);
        assert.isTrue(window.open.calledWith(productPreviewServletUrl + '?url_key=slug&sku=slug'));

        handlePreview({ selections: [{ value: 'slug' }, { value: 'slug2' }] }, productPreviewServletUrl);
        assert.isTrue(window.open.calledWith(productPreviewServletUrl + '?url_key=slug&sku=slug'));

        window.open.restore();
    });

    it('handlePreview() opens category page preview for the first selection', () => {
        sinon.spy(window, 'open');

        handlePreview({ selections: { value: 'slug' } }, categoryPreviewServletUrl);
        assert.isTrue(window.open.calledWith(categoryPreviewServletUrl + '?id=slug'));

        handlePreview({ selections: [{ value: 'slug' }] }, categoryPreviewServletUrl);
        assert.isTrue(window.open.calledWith(categoryPreviewServletUrl + '?id=slug'));

        handlePreview({ selections: [{ value: 'slug' }, { value: 'slug2' }] }, categoryPreviewServletUrl);
        assert.isTrue(window.open.calledWith(categoryPreviewServletUrl + '?id=slug'));

        window.open.restore();
    });
});
