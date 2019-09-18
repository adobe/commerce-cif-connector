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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic servlet for the mock server.
 * The behavior of the servlet is defined by a set of rules that have to be added.
 */
public class Servlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(Servlet.class);

    /**
     * List of rules.
     */
    private List<Rule> rules = new ArrayList<>();

    /**
     * Reset and remove all the rules of the servlet.
     */
    public void reset() {
        for (Rule rule : this.rules) {
            rule.reset();
        }
        this.rules.clear();
    }

    /**
     * Add a rule to the servlet.
     *
     * @param rule Rule to be added.
     */
    public void add(Rule rule) {
        this.rules.add(rule);
    }

    /**
     * Verify all rules.
     *
     * @throws Exception if not all rules are met
     */
    public void verify() throws Exception {
        for (Rule rule : this.rules) {
            rule.verify();
        }
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // Go through rules and execute the first that matches
        RequestWrapper wrapper = new RequestWrapper(req);

        log.info("Incoming request {}: {}", req.getRequestURI(), wrapper.getBody());

        for (Rule rule : rules) {
            if (rule.execute(wrapper, resp))
                return;
        }

        log.info("Failed request {}: {}", req.getRequestURI(), wrapper.getBody());

        // Return 404 if no rule is applicable
        resp.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

}
