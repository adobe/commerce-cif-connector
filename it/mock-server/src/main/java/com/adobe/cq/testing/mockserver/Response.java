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

import javax.servlet.http.HttpServletResponse;

/**
 * Generic interface that extends {@link RequestResponseRule} to include additional methods for mocking.
 */
public interface Response extends HttpServletResponse {

    /**
     * Apply any property set on the response object to a given response.
     *
     * @param r
     *            Response object.
     */
    void applyTo(HttpServletResponse r);

    /**
     * Generic interface for a Response builder.
     */
    interface Builder {

        /**
         * Validate and build Response object.
         *
         * @return Response object
         */
        Response build();
    }
}
