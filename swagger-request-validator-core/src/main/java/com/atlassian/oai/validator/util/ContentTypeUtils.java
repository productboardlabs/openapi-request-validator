package com.atlassian.oai.validator.util;

import com.atlassian.oai.validator.model.Headers;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.google.common.net.MediaType;

import java.util.Optional;

public class ContentTypeUtils {

    private ContentTypeUtils() {

    }

    /**
     * Determine whether a given request has a JSON content-type.
     *
     * @return Whether the content-type of the request (defined in the Content-Type header) is a JSON type.
     */
    public static boolean isJsonContentType(final Request request) {
        return isJsonContentType(request.getHeaderValue(Headers.CONTENT_TYPE));
    }

    /**
     * @return Whether the content-type of this response (defined in the Content-Type header) is a JSON type.
     */
    public static boolean isJsonContentType(final Response response) {
        return isJsonContentType(response.getHeaderValue(Headers.CONTENT_TYPE));
    }

    private static boolean isJsonContentType(final Optional<String> maybeContentType) {
        try {
            return maybeContentType
                    .map(MediaType::parse)
                    .map(ct -> ct.withoutParameters().is(MediaType.JSON_UTF_8.withoutParameters()))
                    .orElse(false);
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Determine whether a given request has a content-type header.
     *
     * @return Whether a content-type header is defined on the request
     */
    public static boolean hasContentType(final Request request) {
        return request.getHeaderValue(Headers.CONTENT_TYPE).isPresent();
    }


    /**
     * Determine whether a given request has a content-type header.
     *
     * @return Whether a content-type header is defined on the response
     */
    public static boolean hasContentType(final Response response) {
        return response.getHeaderValue(Headers.CONTENT_TYPE).isPresent();
    }
}
