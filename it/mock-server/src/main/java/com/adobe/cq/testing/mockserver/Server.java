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

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Server provides a mock server implementation.
 */
public class Server {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    /** Resource path to the key store. */
    private static final String KEY_STORE_PATH = "/com/adobe/cq/testing/mockserver/identity.jks";

    /** Password for the key store. */
    private static final String KEY_STORE_PASSWORD = "qabasel";

    private org.eclipse.jetty.server.Server server;
    private ServletContextHandler context;
    private Servlet servlet = new Servlet();
    private ServerConnector httpConnector;
    private ServerConnector httpsConnector;

    /**
     * Private constructor.
     * Use Builder to create a Server instance.
     */
    private Server() {
        this.server = new org.eclipse.jetty.server.Server();
        this.context = new ServletContextHandler(server, "/", false, false);
        this.context.addServlet(new ServletHolder(servlet), "/*");
    }

    /**
     * Add a HTTP connector to the server.
     *
     * @param httpPort HTTP port
     */
    private void addHttpConnector(int httpPort) {
        this.httpConnector = new ServerConnector(server);
        this.httpConnector.setPort(httpPort);
        server.addConnector(this.httpConnector);
    }

    /**
     * Add a HTTPS connector to the server.
     *
     * @param httpsPort HTTPS port
     */
    private void addHttpsConnector(int httpsPort) {
        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(Server.class.getResource(KEY_STORE_PATH).toExternalForm());
        sslContextFactory.setKeyStorePassword(KEY_STORE_PASSWORD);
        sslContextFactory.setKeyManagerPassword(KEY_STORE_PASSWORD);
        this.httpsConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
        this.httpsConnector.setPort(httpsPort);
        server.addConnector(this.httpsConnector);
    }

    /**
     * HTTP port, the server is listening to.
     *
     * @return HTTP port
     */
    public int getHttpPort() {
        return this.httpConnector.getLocalPort();
    }

    /**
     * HTTPS port, the server is listening to.
     *
     * @return HTTPS port
     */
    public int getHttpsPort() {
        return this.httpsConnector.getLocalPort();
    }

    /**
     * Start the server.
     *
     * @throws Exception if server cannot be started.
     */
    public void start() throws Exception {
        this.server.start();
    }

    /**
     * Stop the server.
     *
     * @throws Exception if server cannot be stopped.
     */
    public void stop() throws Exception {
        this.server.stop();
    }

    /**
     * Reset and remove all rules from the server.
     */
    public void reset() {
        this.servlet.reset();
    }

    /**
     * Add a rule builder to the server.
     *
     * @param builder RuleBuilder object
     */
    public void add(Rule.Builder builder) {
        Rule rule = builder.build();
        this.servlet.add(rule);
    }

    /**
     * Add a rule to the server.
     *
     * @param rule Rule object
     */
    public void add(Rule rule) {
        rule.reset();
        this.servlet.add(rule);
    }

    /**
     * Verify all the rules of the server.
     *
     * @throws Exception if not all rules are met.
     */
    public void verify() throws Exception {
        this.servlet.verify();
    }

    /**
     * Builder for {@link Server}.
     * @see Server
     */
    public static class Builder {

        /** HTTP port. Deactivated if -1. Random if 0. */
        private int httpPort = -1;

        /** HTTPS port. Deactivated if -1. Random if 0. */
        private int httpsPort = -1;

        /**
         * Add a HTTP connector to the server that listens to a random port.
         *
         * @return Server.Builder object
         */
        public Server.Builder withHttp() {
            return this.withHttp(0);
        }

        /**
         * Add a HTTP connector to the server that listens to the given port.
         *
         * @param port HTTP port
         * @return Server.Builder object
         */
        public Server.Builder withHttp(int port) {
            if (isValidPort(port)) {
                this.httpPort = port;
            }

            return this;
        }

        /**
         * Add a HTTPS connector to the server that listens to a random port.
         *
         * @return Server.Builder object
         */
        public Server.Builder withHttps() {
            return this.withHttps(0);
        }

        /**
         * Add a HTTPS connector to the server that listens to the given port.
         *
         * @param port HTTPS port
         * @return Server.Builder object
         */
        public Server.Builder withHttps(int port) {
            if (isValidPort(port)) {
                this.httpsPort = port;
            }
            return this;
        }

        private boolean isValidPort(int port) {
            return (0 <= port && port <= 65535);
        }

        /**
         * Validate and build Server object.
         *
         * @return Server object
         */
        public Server build() {
            if (this.httpPort == -1 && this.httpsPort == -1) {
                log.error("No HTTP or HTTPS ports specified.");
                return null;
            }

            Server server = new Server();
            if (this.httpPort != -1) {
                server.addHttpConnector(this.httpPort);
            }
            if (this.httpsPort != -1) {
                server.addHttpsConnector(this.httpsPort);
            }

            return server;
        }
    }
}
