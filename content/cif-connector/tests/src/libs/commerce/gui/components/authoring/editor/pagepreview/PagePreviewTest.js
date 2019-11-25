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

describe('PagePreviewTest', () => {
    it('test createPreviewUrl()', () => {
        var createPreviewUrl = window.CIF.PagePreview.createPreviewUrl;

        assert.equal(null, createPreviewUrl(null, null));
        assert.equal(null, createPreviewUrl('editor.html', null));
        assert.equal(null, createPreviewUrl('editor.html', 'slug'));
        assert.equal(null, createPreviewUrl('/editor', 'slug'));
        assert.equal('/editor.slug.html', createPreviewUrl('/editor.html', 'slug'));
        assert.equal('/editor.slug.html', createPreviewUrl('/editor.any.html', 'slug'));
    });

    it('test handlePdpPreview()', () => {
        var handlePdpPreview = window.CIF.PagePreview.handlePdpPreview;

        window.CIF.Granite.author.ContentFrame.location = '/editor.html/products/product-page.html';

        sinon.spy(window, 'open');

        handlePdpPreview(null, null);
        assert.isFalse(window.open.calledOnce);

        handlePdpPreview(null, {});
        assert.isFalse(window.open.calledOnce);

        handlePdpPreview(null, { selections: [] });
        assert.isFalse(window.open.calledOnce);

        handlePdpPreview(null, { selections: 'any' });
        assert.isFalse(window.open.calledOnce);

        handlePdpPreview(null, { selections: { value: 'slug' } });
        assert.isTrue(window.open.calledOnce);
        assert.equal('/editor.html/products/product-page.slug.html', window.open.getCall(0).args[0]);

        handlePdpPreview(null, { selections: [{ value: 'slug' }] });
        assert.isTrue(window.open.calledTwice);
        assert.equal('/editor.html/products/product-page.slug.html', window.open.getCall(1).args[0]);

        window.CIF.Granite.author.ContentFrame.location = '/editor.html/products/page.html';
        handlePdpPreview(null, { selections: [{ value: 'slug' }, { value: 'slugs' }] });
        assert.isTrue(window.open.calledThrice);
        assert.equal('/editor.html/products/page.slug.html', window.open.getCall(2).args[0]);
    });
});
