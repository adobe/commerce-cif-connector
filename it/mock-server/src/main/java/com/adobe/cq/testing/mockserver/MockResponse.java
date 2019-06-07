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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * MockResponse contains the data that is sent in a HTTP response within a {@link RequestResponseRule}.
 */
public class MockResponse implements Response {

    /**
     * HTTP status code.
     */
    private int statusCode = 200;

    /**
     * Map of headers.
     */
    private Map<String, String> headers = new HashMap<>();

    /**
     * Writer for response body.
     */
    private StringWriter contentStringWriter;

    /**
     * Print writer for response body.
     */
    private PrintWriter contentPrintWriter;

    /**
     * Private constructor.
     */
    private MockResponse() {
    }

    @Override
    public void addCookie(Cookie cookie) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public boolean containsHeader(String s) {
        return this.headers.containsKey(s);
    }

    @Override
    public String encodeURL(String s) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public String encodeRedirectURL(String s) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public String encodeUrl(String s) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public String encodeRedirectUrl(String s) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void sendError(int i, String s) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void sendError(int i) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void sendRedirect(String s) throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setDateHeader(String s, long l) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void addDateHeader(String s, long l) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setHeader(String s, String s1) {
        this.headers.put(s, s1);
    }

    @Override
    public void addHeader(String s, String s1) {
        this.headers.put(s, s1);
    }

    @Override
    public void setIntHeader(String s, int i) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void addIntHeader(String s, int i) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setStatus(int i) {
        this.statusCode = i;
    }

    @Override
    public void setStatus(int i, String s) {
        this.setStatus(i);
    }

    @Override
    public int getStatus() {
        return this.statusCode;
    }

    @Override
    public String getHeader(String s) {
        return this.headers.get(s);
    }

    @Override
    public Collection<String> getHeaders(String s) {
        return Collections.singletonList(this.headers.get(s));
    }

    @Override
    public Collection<String> getHeaderNames() {
        return this.headers.keySet();
    }

    @Override
    public String getCharacterEncoding() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public String getContentType() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (this.contentPrintWriter == null) {
            this.contentStringWriter = new StringWriter();
            this.contentPrintWriter = new PrintWriter(contentStringWriter, true);
        }
        return this.contentPrintWriter;
    }

    @Override
    public void setCharacterEncoding(String s) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setContentLength(int i) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setContentLengthLong(long len) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setContentType(String s) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setBufferSize(int i) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public int getBufferSize() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void flushBuffer() throws IOException {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void resetBuffer() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public boolean isCommitted() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public void setLocale(Locale locale) {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    @Override
    public Locale getLocale() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    private String getContent() {
        return this.contentStringWriter.toString();
    }

    @Override
    public void applyTo(HttpServletResponse r) {
        // Apply headers
        for (String key : this.getHeaderNames()) {
            r.addHeader(key, this.getHeader(key));
        }

        // Apply content
        try {
            r.getWriter().write(this.getContent());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Apply status code
        r.setStatus(this.getStatus());
    }

    /**
     * Return a new Response.Builder instance.
     *
     * @return Response.Builder object
     */
    public static MockResponse.Builder response() {
        return new MockResponse.Builder();
    }

    /**
     * Builder for {@link MockResponse}.
     *
     * @see MockResponse
     */
    public static class Builder implements Response.Builder {

        /**
         * HTTP status code.
         */
        public int status = 200;

        /**
         * Content of body of response.
         */
        public String content = "";

        /**
         * Map of headers.
         */
        public Map<String, String> headers = new HashMap<>();

        /**
         * Private constructor.
         *
         * Use MockResponse.response() to get an instance of this builder.
         */
        private Builder() {
        }

        /**
         * Set status code for the response.
         *
         * @param c
         *            Status code
         * @return Response.Builder object
         */
        public MockResponse.Builder withStatus(int c) {
            this.status = c;
            return this;
        }

        /**
         * Set the content for the response.
         *
         * @param content
         *            Content
         * @return Response.Builder object
         */
        public MockResponse.Builder withContent(String content) {
            this.content = content;
            return this;
        }

        /**
         * Set content type for response.
         *
         * @param type
         *            Content Type
         * @return Response.Builder object
         */
        public MockResponse.Builder withContentType(String type) {
            this.headers.put("Content-Type", type);
            return this;
        }

        /**
         * Read the given resource and set its content as content for the response.
         *
         * @param path
         *            Path to a resource
         * @return Response.Builder object
         */
        public MockResponse.Builder withContentFromResource(String path) {
            String content = null;
            try {
                content = new String(Files.readAllBytes(Paths.get(this.getClass().getResource(path).toURI())),
                        StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.content = content;
            return this;
        }

        /**
         * Add a response header.
         *
         * @param key
         *            Header key
         * @param value
         *            Header value
         * @return Response.Builder object
         */
        public MockResponse.Builder withHeader(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        @Override
        public MockResponse build() {
            MockResponse r = new MockResponse();
            r.setStatus(this.status);

            try {
                PrintWriter writer = r.getWriter();
                writer.write(this.content);
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            for (Map.Entry<String, String> header : this.headers.entrySet()) {
                r.addHeader(header.getKey(), header.getValue());
            }

            return r;
        }
    }
}
