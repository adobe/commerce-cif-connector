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

import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Rules that matches a given request and sends out a response.
 */
public class RequestResponseRule implements Rule {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseRule.class);

    /** Request to be expected. */
    private Request expectedRequest;

    /** Response to be send. */
    private Response responseToSend;

    /** Number of times this rule is expected to be called. */
    private int expectedCalls = 0;

    /** Number of times this rule was called. */
    private AtomicInteger called = new AtomicInteger(0);

    /**
     * Private constructor. Use Builder to create a Rule instance.
     */
    private RequestResponseRule() {
    }

    /**
     * Set number of expected calls.
     *
     * @param expectedCalls
     *            Expected calls
     */
    public void setExpectedCalls(int expectedCalls) {
        this.expectedCalls = expectedCalls;
    }

    /**
     * Checks if the request object matches a given HTTP request.
     *
     * @param request
     *            HTTP request object
     * @return true if the request matched the given HTTP request.
     */
    @Override
    public boolean match(RequestWrapper request) {
        return this.expectedRequest.match(request);
    }

    /**
     * Checks if the request object matches a given HTTP request and writes a response to the HTTP response object.
     *
     * @param request
     *            HTTP request object
     * @param response
     *            HTTP response object
     * @return true if the request matches and the response was written.
     */
    @Override
    public boolean execute(RequestWrapper request, HttpServletResponse response) {
        // Abort if rule doesn't match
        if (!this.match(request)) {
            return false;
        }

        log.info("Execute rule {}.", this.toString());

        // Increase call counter
        this.called.incrementAndGet();

        // Execute response
        this.responseToSend.applyTo(response);

        return true;
    }

    /**
     * Verify that the number of expected calls equals the actual number of calls.
     */
    @Override
    public void verify() throws Exception {
        if (this.expectedCalls > 0) {
            if (this.expectedCalls != this.called.get()) {
                throw new Exception("Rule " + this.toString() + " was not called as many times as expected. "
                        + String.valueOf(this.called.get()) + " calls received, but "
                        + String.valueOf(this.expectedCalls) + " calls expected.");
            }
        }
    }

    /**
     * Reset the number of actual calls to 0.
     */
    @Override
    public void reset() {
        this.called = new AtomicInteger();
    }

    /**
     * Return a new Rule.Builder instance.
     *
     * @return Rule.Builder instance
     */
    public static RequestResponseRule.Builder rule() {
        return new RequestResponseRule.Builder();
    }

    @Override
    public String toString() {
        return "RequestResponseRule{" + expectedRequest.getRequestURI() + '}';
    }

    /**
     * Builder for {@link RequestResponseRule}.
     * 
     * @see RequestResponseRule
     */
    public static class Builder implements Rule.Builder {

        /** Builder for the request object. */
        public MockRequest.Builder requestBuilder;

        /** Request object. */
        public MockRequest request;

        /** Builder for the response object. */
        public MockResponse.Builder responseBuilder;

        /** Response object. */
        public MockResponse response;

        /** Number of times this rule is expected to be called. */
        public int expectedCalls = 0;

        /**
         * Set the number of expected calls.
         *
         * @param e
         *            Expected calls
         * @return Rule.Builder object
         */
        public RequestResponseRule.Builder expectCalls(int e) {
            this.expectedCalls = e;
            return this;
        }

        /**
         * Set the request of the rule.
         *
         * @param b
         *            Request as Request.Builder.
         * @return Rule.Builder object
         */
        public RequestResponseRule.Builder on(MockRequest.Builder b) {
            this.requestBuilder = b;
            return this;
        }

        /**
         * Set the request of the rule.
         *
         * @param r
         *            Request as HttpServletRequest
         * @return Rule.Builder object
         */
        public RequestResponseRule.Builder on(MockRequest r) {
            this.request = r;
            return this;
        }

        /**
         * Set the response of the rule.
         *
         * @param b
         *            Response as Response.Builder
         * @return Rule.Builder object
         */
        public RequestResponseRule.Builder send(MockResponse.Builder b) {
            this.responseBuilder = b;
            return this;
        }

        /**
         * Set the response of the rule.
         *
         * @param r
         *            Response as HttpServletResponse
         * @return Rule.Builder object
         */
        public RequestResponseRule.Builder send(MockResponse r) {
            this.response = r;
            return this;
        }

        /**
         * Generate JSON representation of the rule which can be used when configuring the mock server with the
         * configuration servlet.
         *
         * @return JSON representation.
         * @throws JsonProcessingException
         *             if object cannot be converted to JSON
         */
        public String toJSON() throws JsonProcessingException {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(this);
        }

        @Override
        public Rule build() {
            RequestResponseRule r = new RequestResponseRule();

            r.expectedCalls = this.expectedCalls;

            if (this.requestBuilder != null) {
                this.request = this.requestBuilder.build();
            }
            r.expectedRequest = this.request;

            if (this.responseBuilder != null) {
                this.response = this.responseBuilder.build();
            }
            r.responseToSend = this.response;

            return r;
        }
    }
}
