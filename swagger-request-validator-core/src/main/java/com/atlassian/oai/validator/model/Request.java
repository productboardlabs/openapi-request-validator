package com.atlassian.oai.validator.model;

import javax.annotation.Nonnull;
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

}
