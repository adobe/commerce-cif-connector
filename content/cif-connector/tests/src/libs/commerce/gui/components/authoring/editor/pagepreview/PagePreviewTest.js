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
    it('createPreviewUrl() returns null for invalid parameters', () => {
        var createPreviewUrl = window.CIF.PagePreview.createPreviewUrl;

        assert.equal(null, createPreviewUrl(null, null));
        assert.equal(null, createPreviewUrl('editor.html', null));
        assert.equal(null, createPreviewUrl('editor.html', 'slug'));
        assert.equal(null, createPreviewUrl('/editor', 'slug'));
    });

    it('createPreviewUrl() adds selector as new URL selector or replaces old selector with new selector', () => {
        var createPreviewUrl = window.CIF.PagePreview.createPreviewUrl;
        assert.equal('/editor.slug.html', createPreviewUrl('/editor.html', 'slug'));
        assert.equal('/editor.slug.html', createPreviewUrl('/editor.any.html', 'slug'));
    });

    it('handlePreview() does nothing for invalid selections', () => {
        var handlePreview = window.CIF.PagePreview.handlePreview;
        sinon.spy(window, 'open');

        handlePreview(null, null);
        assert.isFalse(window.open.calledOnce);

        handlePreview(null, {});
        assert.isFalse(window.open.calledOnce);

        handlePreview(null, { selections: [] });
        assert.isFalse(window.open.calledOnce);

        handlePreview(null, { selections: 'any' });
        assert.isFalse(window.open.calledOnce);

        window.open.restore();
    });

    it('handlePreview() opens page preview for the first selection', () => {
        var handlePreview = window.CIF.PagePreview.handlePreview;
        sinon.spy(window, 'open');
        Granite.author.ContentFrame.location = '/editor.html/products/product-page.html';

        handlePreview(null, { selections: { value: 'slug' } });
        assert.isTrue(window.open.calledOnce);
        assert.equal('/editor.html/products/product-page.slug.html', window.open.getCall(0).args[0]);

        handlePreview(null, { selections: [{ value: 'slug' }] });
        assert.isTrue(window.open.calledTwice);
        assert.equal('/editor.html/products/product-page.slug.html', window.open.getCall(1).args[0]);

        Granite.author.ContentFrame.location = '/editor.html/products/page.html';
        handlePreview(null, { selections: [{ value: 'slug' }, { value: 'slug2' }] });
        assert.isTrue(window.open.calledThrice);
        assert.equal('/editor.html/products/page.slug.html', window.open.getCall(2).args[0]);

        window.open.restore();
    });
});
