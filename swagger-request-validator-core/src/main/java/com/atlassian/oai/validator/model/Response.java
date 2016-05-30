package com.atlassian.oai.validator.model;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Implementation-agnostic representation of a HTTP response
 */
public interface Response {

    /**
     * @return The response status code
     */
    int getStatus();

    /**
     * @return The response body, if there is one.
     */
    @Nonnull
    Optional<String> getBody();

}
