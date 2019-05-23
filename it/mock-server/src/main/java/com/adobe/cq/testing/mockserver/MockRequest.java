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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.eclipse.jetty.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;

/**
 * MockRequest contains the data that is used to match a {@link RequestResponseRule} against an incoming HTTP request
 */
public class MockRequest implements Request {

    private static final Logger log = LoggerFactory.getLogger(MockRequest.class);

    /**
     * Expected path.
     */
    private String requestURI;

    /**
     * Expected request method.
     */
    private String method;

    /**
     * Expected parameters.
     */
    private Map<String, String[]> parameters = new HashMap<>();

    /**
     * Expected headers.
     */
    private Map<String, String> headers = new HashMap<>();

    /**
     * Expected body.
     */
    public String body = null;
    public StringComparator bodyComparator = null;

    /**
     * Private constructor.
     */
    private MockRequest() {
    }

    /**
     * Return a new Request.Builder instance.
     *
     * @return Request.Builder object
     */
    public static MockRequest.Builder request() {
        return new MockRequest.Builder();
    }

    @Override public String getAuthType() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public Cookie[] getCookies() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public long getDateHeader(String s) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getHeader(String s) {
        return this.headers.get(s);
    }

    @Override public Enumeration<String> getHeaders(String s) {
        return Collections.enumeration(Collections.singletonList(this.headers.get(s)));
    }

    @Override public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(this.headers.keySet());
    }

    @Override public int getIntHeader(String s) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getMethod() {
        return this.method;
    }

    @Override public String getPathInfo() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getPathTranslated() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getContextPath() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getQueryString() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getRemoteUser() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public boolean isUserInRole(String s) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public Principal getUserPrincipal() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getRequestedSessionId() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getRequestURI() {
        return this.requestURI;
    }

    @Override public StringBuffer getRequestURL() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getServletPath() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public HttpSession getSession(boolean b) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public HttpSession getSession() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String changeSessionId() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public boolean isRequestedSessionIdValid() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public boolean isRequestedSessionIdFromCookie() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public boolean isRequestedSessionIdFromURL() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public boolean isRequestedSessionIdFromUrl() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public void login(String s, String s1) throws ServletException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public void logout() throws ServletException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public Collection<Part> getParts() throws IOException, ServletException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public Part getPart(String s) throws IOException, ServletException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public Object getAttribute(String s) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public Enumeration<String> getAttributeNames() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getCharacterEncoding() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public int getContentLength() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public long getContentLengthLong() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getContentType() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public ServletInputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getParameter(String s) {
        if (this.parameters.get(0) == null) {
            return null;
        }
        return this.parameters.get(s)[0];
    }

    @Override public Enumeration<String> getParameterNames() {
        return Collections.enumeration(this.parameters.keySet());
    }

    @Override public String[] getParameterValues(String s) {
        return this.parameters.get(0);
    }

    @Override public Map<String, String[]> getParameterMap() {
        return this.parameters;
    }

    @Override public String getProtocol() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getScheme() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getServerName() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public int getServerPort() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public BufferedReader getReader() throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getRemoteAddr() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getRemoteHost() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public void setAttribute(String s, Object o) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public void removeAttribute(String s) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public Locale getLocale() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public Enumeration<Locale> getLocales() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public boolean isSecure() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public RequestDispatcher getRequestDispatcher(String s) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getRealPath(String s) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public int getRemotePort() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getLocalName() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public String getLocalAddr() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public int getLocalPort() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public ServletContext getServletContext() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public AsyncContext startAsync() throws IllegalStateException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public boolean isAsyncStarted() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public boolean isAsyncSupported() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public AsyncContext getAsyncContext() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public DispatcherType getDispatcherType() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override public boolean match(RequestWrapper r) {
        // Check method
        if (!r.getRequest().getMethod().equals(this.getMethod())) {
            return false;
        }

        // Check path
        if (!r.getRequest().getRequestURI().equals(this.getRequestURI())) {
            return false;
        }

        // Check parameters
        if (!this.parameters.isEmpty()) {
            for (Map.Entry<String, String[]> entry : this.parameters.entrySet()) {
                // Check that the parameter was passed
                if (!r.getRequest().getParameterMap().containsKey(entry.getKey())) {
                    return false;
                }

                // Get list of parameter values
                List<String> expectedList = new ArrayList<>(Arrays.asList(entry.getValue()));
                List<String> givenList = new ArrayList<>(Arrays.asList(r.getRequest().getParameterValues(entry.getKey())));

                // Verify that all expected parameters are given
                expectedList.removeAll(givenList);
                if (expectedList.size() > 0) {
                    return false;
                }
            }
        }

        // Check body
        if (this.body != null || this.bodyComparator != null) {
            String body = null;
            try {
                body = r.getBody();
            } catch (IOException e) {
                log.error("Could not read request body: {}", e);
            }
            if (this.body != null && !this.body.equals(body)) {
                return false;
            }
            if (this.bodyComparator != null && !this.bodyComparator.compare(body)) {
                return false;
            }
        }

        // Check headers
        Enumeration<String> headerKeys = this.getHeaderNames();
        while (headerKeys.hasMoreElements()) {
            String key = headerKeys.nextElement();
            if (!r.getRequest().getHeader(key).equals(this.getHeader(key))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Builder for {@link MockRequest}.
     *
     * @see MockRequest
     */
    @JsonIgnoreProperties(value = {"bodyComparator"})
    public static class Builder implements Request.Builder {

        /**
         * Expected path.
         */
        public String requestURI;

        /**
         * Expected request method.
         */
        public String method;

        /**
         * Expected parameters.
         */
        public Map<String, List<String>> parameters = new HashMap<>();

        /**
         * Expected headers.
         */
        public Map<String, String> headers = new HashMap<>();

        /**
         * Expected body
         */
        public String body = null;
        public StringComparator bodyComparator = null;


        /**
         * Private constructor.
         *
         * Use MockRequest.request() to get an instance of this builder.
         */
        private Builder() {}

        /**
         * Set expected path.
         *
         * @param requestURI Expected requestURI
         * @return Request.Builder object
         */
        public MockRequest.Builder withRequestURI(String requestURI) {
            this.requestURI = requestURI;
            return this;
        }

        /**
         * Set expected request method.
         *
         * @param method Expected request method
         * @return Request.Builder object
         */
        public MockRequest.Builder withMethod(String method) {
            this.method = method;
            return this;
        }

        /**
         * Set expected request method.
         *
         * @param method Expected request method
         * @return Request.Builder object
         */
        public MockRequest.Builder withMethod(HttpMethod method) {
            this.method = method.name();
            return this;
        }

        /**
         * Add expected parameter.
         *
         * @param key   Parameter key
         * @param value Parameter value
         * @return Request.Builder object
         */
        public MockRequest.Builder withParameter(String key, String value) {
            if (!this.parameters.containsKey(key)) {
                this.parameters.put(key, new ArrayList<String>());
            }
            this.parameters.get(key).add(value);
            return this;
        }

        /**
         * Add expected header.
         *
         * @param key   Header key
         * @param value Header value
         * @return Request.Builder object
         */
        public MockRequest.Builder withHeader(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        /**
         * Add expected content type of request body.
         *
         * @param type Content type
         * @return Request.Builder object
         */
        public MockRequest.Builder withContentType(String type) {
            this.headers.put("Content-Type", type);
            return this;
        }

        /**
         * Add expected request body.
         *
         * @param body Request body as string
         * @return Request.Builder object
         */
        public MockRequest.Builder withBody(String body) {
            this.body = body;
            return this;
        }

        /**
         * Add expected request body.
         *
         * @param comparator Request body as StringComparator function
         * @return Request.Builder object
         */
        public MockRequest.Builder withBody(StringComparator comparator) {
            this.bodyComparator = comparator;
            return this;
        }

        /**
         * Add expected request body from a resource file.
         *
         * @param path Path to resource
         * @return Request.Builder object
         */
        public MockRequest.Builder withBodyFromResource(String path) {
            String content = null;
            try {
                content = new String(Files.readAllBytes(Paths.get(this.getClass().getResource(path).toURI())), StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.body = content;
            return this;
        }

        @Override public MockRequest build() {
            MockRequest r = new MockRequest();
            r.requestURI = this.requestURI;
            r.method = this.method;
            r.headers = new HashMap<>(this.headers);
            r.body = this.body;
            r.bodyComparator = this.bodyComparator;

            // Convert parameter map
            Map<String, String[]> arrayParameters = new HashMap<>();
            for (Map.Entry<String, List<String>> entry : this.parameters.entrySet()) {
                arrayParameters.put(entry.getKey(), entry.getValue().toArray(new String[0]));
            }
            r.parameters = arrayParameters;

            return r;
        }
    }
}
