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

package com.adobe.cq.commerce.graphql.testing;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import com.adobe.cq.commerce.graphql.client.GraphqlRequest;
import com.adobe.cq.commerce.graphql.client.HttpMethod;
import com.google.gson.Gson;

public class Utils {

    /**
     * Matcher class used to match a GraphQL query. This is used to properly mock GraphQL responses.
     */
    private static class GraphqlQueryMatcher extends ArgumentMatcher<HttpUriRequest> {

        private String startsWith;

        public GraphqlQueryMatcher(String startsWith) {
            this.startsWith = startsWith;
        }

        @Override
        public boolean matches(Object obj) {
            if (!(obj instanceof HttpUriRequest)) {
                return false;
            }

            if (obj instanceof HttpEntityEnclosingRequest) {
                // GraphQL query is in POST body
                HttpEntityEnclosingRequest req = (HttpEntityEnclosingRequest) obj;
                try {
                    String body = IOUtils.toString(req.getEntity().getContent(), StandardCharsets.UTF_8);
                    Gson gson = new Gson();
                    GraphqlRequest graphqlRequest = gson.fromJson(body, GraphqlRequest.class);
                    return graphqlRequest.getQuery().startsWith(startsWith);
                } catch (Exception e) {
                    return false;
                }
            } else {
                // GraphQL query is in the URL 'query' parameter
                HttpUriRequest req = (HttpUriRequest) obj;
                String uri = null;
                try {
                    uri = URLDecoder.decode(req.getURI().toString(), StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    return false;
                }
                String graphqlQuery = uri.substring(uri.indexOf("?query=") + 7);
                return graphqlQuery.startsWith(startsWith);
            }
        }

    }

    /**
     * Matcher class used to check that the headers are properly passed to the HTTP client.
     */
    public static class HeadersMatcher extends ArgumentMatcher<HttpUriRequest> {

        private List<Header> headers;

        public HeadersMatcher(List<Header> headers) {
            this.headers = headers;
        }

        @Override
        public boolean matches(Object obj) {
            if (!(obj instanceof HttpUriRequest)) {
                return false;
            }
            HttpUriRequest req = (HttpUriRequest) obj;
            for (Header header : headers) {
                Header reqHeader = req.getFirstHeader(header.getName());
                if (reqHeader == null || !reqHeader.getValue().equals(header.getValue())) {
                    return false;
                }
            }
            return true;
        }
    }

    private static String encode(String str) throws UnsupportedEncodingException {
        return URLEncoder.encode(str, StandardCharsets.UTF_8.name());
    }

    /**
     * Matcher class used to check that the GraphQL query is properly set and encoded when sent with a GET request.
     */
    public static class GetQueryMatcher extends ArgumentMatcher<HttpUriRequest> {

        GraphqlRequest request;

        public GetQueryMatcher(GraphqlRequest request) {
            this.request = request;
        }

        @Override
        public boolean matches(Object obj) {
            if (!(obj instanceof HttpUriRequest)) {
                return false;
            }
            HttpUriRequest req = (HttpUriRequest) obj;
            String expectedEncodedQuery = "/";
            try {
                expectedEncodedQuery += "?query=" + encode(request.getQuery());
                if (request.getOperationName() != null) {
                    expectedEncodedQuery += "&operationName=" + encode(request.getOperationName());
                }
                if (request.getVariables() != null) {
                    String json = new Gson().toJson(request.getVariables());
                    expectedEncodedQuery += "&variables=" + encode(json);
                }
            } catch (UnsupportedEncodingException e) {
                return false;
            }
            return HttpMethod.GET.toString().equals(req.getMethod()) && expectedEncodedQuery.equals(req.getURI().toString());
        }
    }

    /**
     * This method prepares the mock http response with either the content of the <code>filename</code>
     * or the provided <code>content</code> String.<br>
     * <br>
     * <b>Important</b>: because of the way the content of an HTTP response is consumed, this method MUST be called each time
     * the client is called.
     *
     * @param filename The file to use for the json response.
     * @param httpClient The HTTP client for which we want to mock responses.
     * @param httpCode The http code that the mocked response will return.
     * 
     * @return The JSON content of that file.
     * 
     * @throws IOException
     */
    public static String setupHttpResponse(String filename, HttpClient httpClient, int httpCode) throws IOException {
        return setupHttpResponse(filename, httpClient, httpCode, null);
    }

    /**
     * This method prepares the mock http response with either the content of the <code>filename</code>
     * or the provided <code>content</code> String.<br>
     * <br>
     * <b>Important</b>: because of the way the content of an HTTP response is consumed, this method MUST be called each time
     * the client is called.
     *
     * @param filename The file to use for the json response.
     * @param httpClient The HTTP client for which we want to mock responses.
     * @param httpCode The http code that the mocked response will return.
     * @param startsWith When set, the body of the GraphQL POST request must start with that String.
     * 
     * @return The JSON content of that file.
     * 
     * @throws IOException
     */
    public static String setupHttpResponse(String filename, HttpClient httpClient, int httpCode, String startsWith) throws IOException {
        String json = IOUtils.toString(Utils.class.getClassLoader().getResourceAsStream(filename), StandardCharsets.UTF_8);

        HttpEntity mockedHttpEntity = Mockito.mock(HttpEntity.class);
        HttpResponse mockedHttpResponse = Mockito.mock(HttpResponse.class);
        StatusLine mockedStatusLine = Mockito.mock(StatusLine.class);

        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        Mockito.when(mockedHttpEntity.getContent()).thenReturn(new ByteArrayInputStream(bytes));
        Mockito.when(mockedHttpEntity.getContentLength()).thenReturn(new Long(bytes.length));

        Mockito.when(mockedHttpResponse.getEntity()).thenReturn(mockedHttpEntity);

        if (startsWith != null) {
            GraphqlQueryMatcher matcher = new GraphqlQueryMatcher(startsWith);
            Mockito.when(httpClient.execute(Mockito.argThat(matcher))).thenReturn(mockedHttpResponse);
        } else {
            Mockito.when(httpClient.execute((HttpUriRequest) Mockito.any())).thenReturn(mockedHttpResponse);
        }

        Mockito.when(mockedStatusLine.getStatusCode()).thenReturn(httpCode);
        Mockito.when(mockedHttpResponse.getStatusLine()).thenReturn(mockedStatusLine);

        return json;
    }

    /**
     * Calls the single-argument "activate" osgi lifecycle method on a component instance.
     */
    public static <T, C> T activateComponent(T component, Class<C> argumentType, C argument) throws Exception {
        Method activateMethod = component.getClass().getDeclaredMethod("activate", argumentType);
        activateMethod.setAccessible(true);
        activateMethod.invoke(component, argument);
        return component;
    }
}
