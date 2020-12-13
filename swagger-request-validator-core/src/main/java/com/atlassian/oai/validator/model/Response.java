package com.atlassian.oai.validator.model;

import com.atlassian.oai.validator.util.ContentTypeUtils;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Optional;

import static com.atlassian.oai.validator.model.Headers.CONTENT_TYPE;

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
     * @deprecated use {@link #getResponseBody()}. This method will be removed in a future release.
     */
    @Nonnull
    @Deprecated
    Optional<String> getBody();

    /**
     * @return the response body
     */
    @Nonnull
    default Optional<Body> getResponseBody() {
        return getBody()
                .map(content -> {
                    final String contentType = getContentType().orElse(null);
                    final Charset charset = ContentTypeUtils.getCharsetFromContentType(contentType)
                            .orElse(StandardCharsets.UTF_8);
                    return new StringBody(content, charset);
                });
    }

    /**
     * Get the collection of header values for the header param with the given name.
     *
     * @param name The (case insensitive) name of the parameter to retrieve
     *
     * @return The header values for that param; or empty list
     */
    @Nonnull
    Collection<String> getHeaderValues(String name);

    /**
     * Get the first of header value for the header param with the given name (if any exist).
     *
     * @param name The (case insensitive) name of the parameter to retrieve
     *
     * @return The first header value for that param (if it exists)
     */
    @Nonnull
    default Optional<String> getHeaderValue(final String name) {
        return getHeaderValues(name).stream().findFirst();
    }

    /**
     * Get the content-type header of this response, if it has been set.
     *
     * @return The content-type header, or empty if it has not been set.
     */
    @Nonnull
    default Optional<String> getContentType() {
        return getHeaderValue(CONTENT_TYPE);
    }

}
