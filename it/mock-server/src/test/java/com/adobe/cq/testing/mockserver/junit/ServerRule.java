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

package com.adobe.cq.testing.mockserver.junit;

import com.adobe.cq.testing.mockserver.Rule;
import com.adobe.cq.testing.mockserver.Server;

import org.junit.rules.ExternalResource;

/**
 * Rule wrapper for the mock server to be used in JUnit.
 */
public class ServerRule extends ExternalResource {

    /** Server instance. */
    private Server server;

    /**
     * Private constructor.
     */
    private ServerRule() {}

    /**
     * Instantiate a ServerRule that automatically starts and stops a server as defined in the given Builder.
     *
     * @param builder Server.Builder object
     */
    public ServerRule(Server.Builder builder) {
        super();
        this.server = builder.build();
    }

    @Override
    protected void before() throws Exception {
        this.server.start();
    }

    @Override
    protected void after() {
        try {
            this.server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove all rules from the server.
     */
    public void reset() {
        this.server.reset();
    }

    /**
     * Add a rule builder to the server.
     *
     * @param builder Rule.Builder object
     */
    public void add(Rule.Builder builder) {
        this.server.add(builder);
    }

    /**
     * Add a rule to the server.
     *
     * @param rule Rule object
     */
    public void add(Rule rule) {
        this.server.add(rule);
    }

    /**
     * Verify all the rules of the server.
     */
    public void verify() throws Exception {
        this.server.verify();
    }

    /**
     * HTTP port, the server is listening to.
     *
     * @return HTTP port
     */
    public int getHttpPort() {
        return this.server.getHttpPort();
    }

    /**
     * HTTPS port, the server is listening to.
     *
     * @return HTTPS port
     */
    public int getHttpsPort() {
        return this.server.getHttpsPort();
    }
}
