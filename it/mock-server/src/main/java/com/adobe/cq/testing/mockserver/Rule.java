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
 * Defines behavior of the mock server via the generic servlet.
 *
 * @author Mark J. Becker {@literal <mabecker@adobe.com>}
 */
public interface Rule {

    /**
     * Checks if the Rule matches a given HTTP request.
     *
     * @param request HTTP request object
     * @return true if the rule matches the given HTTP request.
     */
    boolean match(RequestWrapper request);

    /**
     * Execute the rule for given request and response object.
     *
     * @param request HTTP request object
     * @param response HTTP response object
     * @return true if the rule was applied
     */
    boolean execute(RequestWrapper request, HttpServletResponse response);

    /**
     * Verify that the rule was applied correctly.
     *
     * @throws Exception if rule is not met
     */
    void verify() throws Exception;

    /**
     * Reset the rule to be reused.
     */
    void reset();

    /**
     * Generic interface for a Rule builder.
     *
     * @author Mark J. Becker {@literal <mabecker@adobe.com>}
     */
    interface Builder {

        /**
         * Validate and build Rule object.
         *
         * @return Rule object
         */
        Rule build();
    }

}
