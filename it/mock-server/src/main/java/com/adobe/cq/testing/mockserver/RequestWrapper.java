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

package com.adobe.cq.testing.mockserver;

import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * RequestWrapper wraps a HttpServletRequest to enable multiple caching of the request body.
 */
public class RequestWrapper {

    /** Request reference. */
    private HttpServletRequest request;

    /** Request body cache. */
    private String body = null;

    /**
     * Constructor.
     *
     * @param request Request reference.
     */
    public RequestWrapper(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Returns the body of the request.
     *
     * @return Request body as string.
     * @throws IOException if body cannot be read
     */
    public String getBody() throws IOException {
        if (this.body == null) {
            this.body = IOUtils.toString(this.request.getReader());
        }
        return this.body;
    }

    /**
     * Returns request reference.
     *
     * @return Request reference.
     */
    public HttpServletRequest getRequest() {
        return request;
    }
}
