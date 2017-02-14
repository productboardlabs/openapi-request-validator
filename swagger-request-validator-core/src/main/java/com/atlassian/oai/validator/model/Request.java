package com.atlassian.oai.validator.model;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation-agnostic representation of a HTTP request
 */
public interface Request {

    /**
     * Supported HTTP request methods
     */
    enum Method {
        GET,
        POST,
        PUT,
        PATCH,
        DELETE
    }

    /**
     * @return the request path
     */
    @Nonnull
    String getPath();

    /**
     * @return the HTTP request method ("GET", "PUT" etc.)
     */
    @Nonnull
    Method getMethod();

    /**
     * @return the request body
     */
    @Nonnull
    Optional<String> getBody();

    /**
     * @return the collection of query parameter names present on this request
     */
    @Nonnull
    Collection<String> getQueryParameters();

    /**
     * Get the collection of query parameter values for the query param with the given name.
     *
     * @param name The (case insensitive) name of the parameter to retrieve
     *
     * @return The query parameter values for that param; or empty list
     */
    @Nonnull
    Collection<String> getQueryParameterValues(String name);

    @Nonnull
    Map<String, String> getHeaders();

}
