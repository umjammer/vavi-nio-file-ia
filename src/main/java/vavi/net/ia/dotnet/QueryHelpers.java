// Copyright (c) .NET Foundation. All rights reserved.
// Licensed under the Apache License, Version 2.0. See License.txt in the project root for license information.

package vavi.net.ia.dotnet;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


/**
 * TODO move to dotnet4j
 */
public class QueryHelpers {

    private QueryHelpers() {}

    /**
     * append the given query key and value to the URI.
     *
     * @param uri   The base URI.
     * @param name  The name of the query key.
     * @param value The query value.
     * @returns The combined result.
     */
    public static String AddQueryString(String uri, String name, String value) {
        if (uri == null) {
            throw new IllegalArgumentException("uri");
        }

        if (name == null) {
            throw new IllegalArgumentException("name");
        }

        if (value == null) {
            throw new IllegalArgumentException("value");
        }

        return AddQueryString(uri, List.of(new KeyValuePair<>(name, value)));
    }

    /**
     * append the given query keys and values to the uri.
     *
     * @param uri         The base uri.
     * @param queryString A collection of name value query pairs to append.
     * @return The combined result.
     */
    public static String AddQueryString(String uri, Map<String, String> queryString) {
        if (uri == null) {
            throw new IllegalArgumentException("uri");
        }

        if (queryString == null) {
            throw new IllegalArgumentException("queryString");
        }

        return AddQueryString(uri, queryString.entrySet());
    }

    public static String AddQueryString(
            String uri,
            Iterable<Map.Entry<String, String>> queryString) {
        if (uri == null) {
            throw new IllegalArgumentException("uri");
        }

        if (queryString == null) {
            throw new IllegalArgumentException("queryString");
        }

        var anchorIndex = uri.indexOf('#');
        var uriToBeappended = uri;
        var anchorText = "";
        // If there is an anchor, then the query String must be inserted before its first occurance.
        if (anchorIndex != -1) {
            anchorText = uri.substring(anchorIndex);
            uriToBeappended = uri.substring(0, anchorIndex);
        }

        var queryIndex = uriToBeappended.indexOf('?');
        var hasQuery = queryIndex != -1;

        var sb = new StringBuilder();
        sb.append(uriToBeappended);
        for (var parameter : queryString) {
            if (parameter.getValue() == null) continue;

            sb.append(hasQuery ? '&' : '?');
            sb.append(URLEncoder.encode(parameter.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(parameter.getValue(), StandardCharsets.UTF_8));
            hasQuery = true;
        }

        sb.append(anchorText);
        return sb.toString();
    }
}