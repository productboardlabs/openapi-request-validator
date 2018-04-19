package com.atlassian.oai.validator.util;

import com.atlassian.oai.validator.model.Headers;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.google.common.net.MediaType;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import static com.google.common.net.MediaType.JSON_UTF_8;

public class ContentTypeUtils {

    private ContentTypeUtils() {

    }

    /**
     * Determine whether a given request has a JSON content-type.
     *
     * @return Whether the content-type of the request (defined in the Content-Type header) is a JSON type.
     */
    public static boolean isJsonContentType(final Request request) {
        return isJsonContentType(request.getHeaderValue(Headers.CONTENT_TYPE).orElse(null));
    }

    /**
     * @return Whether the content-type of this response (defined in the Content-Type header) is a JSON type.
     */
    public static boolean isJsonContentType(final Response response) {
        return isJsonContentType(response.getHeaderValue(Headers.CONTENT_TYPE).orElse(null));
    }

    /**
     * @return Whether the provided content-type is a JSON type.
     */
    public static boolean isJsonContentType(final String contentType) {
        if (contentType == null) {
            return false;
        }
        try {
            final MediaType mediaType = MediaType.parse(contentType);
            return JSON_UTF_8.withoutParameters().is(mediaType);
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

    public static Optional<String> findMostSpecificMatch(final String candidate, final Set<String> contentTypes) {
        return contentTypes
                .stream()
                .map(MediaType::parse)
                .sorted(new ContentTypeComparator())
                .filter(ct -> MediaType.parse(candidate).withoutParameters().is(ct.withoutParameters()))
                .map(MediaType::toString)
                .findFirst();
    }

    private static class ContentTypeComparator implements Comparator<MediaType> {
        @Override
        public int compare(final MediaType o1, final MediaType o2) {
            if (o1.hasWildcard() && o2.hasWildcard()) {
                return 0;
            } else if (o1.hasWildcard()) {
                return -1;
            } else if (o2.hasWildcard()) {
                return 1;
            }
            return 0;
        }
    }
}
