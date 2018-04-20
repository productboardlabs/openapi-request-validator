package com.atlassian.oai.validator.util;

import com.atlassian.oai.validator.model.Headers;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.google.common.net.MediaType;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.util.Optional.empty;

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

    /**
     * Find the content-type that most specifically matches the content-type defined on the given response.
     * <p>
     * e.g. If the response has {@code Content-Type=text/plain} and the list of types is <code>[text/&#42;, &#42;/&#42;, text/plain]</code>
     * (all of which could match), the most specific match {@code text/plain} will be returned.
     * <p>
     * If there are no matches, will return empty.
     *
     * @param response The response to find a matching content type for
     * @param apiContentTypes The list of content types to search
     *
     * @return The most specific content type that matches the given request, or empty if none match.
     */
    public static Optional<String> findMostSpecificMatch(final Response response, final Set<String> apiContentTypes) {
        return findMostSpecificMatch(response.getHeaderValue(Headers.CONTENT_TYPE).orElse("*/*"), apiContentTypes);
    }

    /**
     * Find the content-type that most specifically matches the given candidate content type.
     * <p>
     * e.g. If the candidate is {@code text/plain} and the list of types is <code>[text/&#42;, &#42;/&#42;, text/plain]</code>
     * (all of which could match), the most specific match {@code text/plain} will be returned.
     * <p>
     * If there are no matches, will return empty.
     *
     * @param candidate The response to find a matching content type for
     * @param apiContentTypes The list of content types to search
     *
     * @return The most specific content type that matches the given request, or empty if none match.
     */
    public static Optional<String> findMostSpecificMatch(final String candidate, final Set<String> apiContentTypes) {
        try {
            return apiContentTypes
                    .stream()
                    .map(MediaType::parse)
                    .sorted(new ContentTypeComparator())
                    .filter(ct -> MediaType.parse(candidate).withoutParameters().is(ct.withoutParameters()))
                    .map(MediaType::toString)
                    .findFirst();
        } catch (final IllegalArgumentException e) {
            return empty();
        }

    }

    private static class ContentTypeComparator implements Comparator<MediaType> {
        @Override
        public int compare(final MediaType o1, final MediaType o2) {
            return countWildcards(o1) - countWildcards(o2);
        }

        private int countWildcards(final MediaType mt) {
            int result = 0;
            if (mt.type().equals("*")) {
                result++;
            }
            if (mt.subtype().equals("*")) {
                result++;
            }
            return result;
        }
    }
}
