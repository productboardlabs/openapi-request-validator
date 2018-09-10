package com.atlassian.oai.validator.util;

import com.atlassian.oai.validator.model.Headers;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.google.common.net.MediaType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import static com.google.common.net.MediaType.FORM_DATA;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static java.util.Optional.empty;

public class ContentTypeUtils {
    //https://github.com/google/guava/issues/3184
    private static final MediaType HAL_JSON = MediaType.create("application", "hal+json");

    private ContentTypeUtils() {

    }

    /**
     * Determine whether a given request has a JSON content-type.
     *
     * @return Whether the content-type of the request (defined in the Content-Type header) is a JSON type.
     */
    public static boolean isJsonContentType(final Request request) {
        return isJsonContentType(request.getContentType().orElse(null));
    }

    /**
     * @return Whether the content-type of this response (defined in the Content-Type header) is a JSON type.
     */
    public static boolean isJsonContentType(final Response response) {
        return isJsonContentType(response.getContentType().orElse(null));
    }

    /**
     * @return Whether the provided content-type is a JSON type.
     */
    public static boolean isJsonContentType(@Nullable final String contentType) {
        return matches(contentType, JSON_UTF_8) || matches(contentType, HAL_JSON);
    }

    /**
     * Determine whether a given request has a formdata content-type.
     *
     * @return Whether the content-type of the request (defined in the Content-Type header) is a FORM_DATA type.
     */
    public static boolean isFormDataContentType(final Request request) {
        return isFormDataContentType(request.getContentType().orElse(null));
    }

    /**
     * Determine whether a given response has a formdata content-type.
     *
     * @return Whether the content-type of the response (defined in the Content-Type header) is a FORM_DATA type.
     */
    public static boolean isFormDataContentType(final Response response) {
        return isFormDataContentType(response.getContentType().orElse(null));
    }

    /**
     * @return Whether the provided content-type is a form data type.
     */
    public static boolean isFormDataContentType(@Nullable final String contentType) {
        return matches(contentType, FORM_DATA);
    }

    /**
     * @return Whether the provided content-type is a multi-part form data type.
     */
    public static boolean isMultipartFormDataContentType(@Nullable final String contentType) {
        return contentType != null && contentType.startsWith("multipart/");
    }

    public static boolean matches(@Nullable final String contentType, final MediaType expected) {
        if (contentType == null) {
            return false;
        }
        try {
            final MediaType mediaType = MediaType.parse(contentType);
            return expected.withoutParameters().is(mediaType.withoutParameters());
        } catch (final IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Checks if the content type of a multipart/form-data request matches the consumer's content type.
     *
     * @param requestContentType content-type of a request
     * @param consumesContentType content-type that the API consumes
     */
    public static boolean isMultipartContentTypeAcceptedByConsumer(@Nullable final String requestContentType,
                                                                   @Nullable final String consumesContentType) {
        if (requestContentType == null || consumesContentType == null) {
            return false;
        }

        // https://github.com/OAI/OpenAPI-Specification/issues/303
        if (!requestContentType.startsWith("multipart/") || !consumesContentType.startsWith("multipart/")) {
            return false;
        }

        final Optional<String> consumesContentTypeBoundary = extractMultipartBoundary(consumesContentType);
        if (consumesContentTypeBoundary.isPresent()) {
            // A corner-case when the boundary was specified as a part of "consumes": compare full content-type values
            return requestContentType.trim().equals(consumesContentType.trim().toLowerCase());
        }

        // startsWith() will neglect the "boundary" part
        return requestContentType.trim().toLowerCase().startsWith(consumesContentType.trim().toLowerCase());
    }

    /**
     * Extracts boundary from multipart/form-data content type
     *
     * @param multipartContentType a multipart form data content type, e.g. "multipart/form-data; boundary=blah"
     *
     * @return the boundary value (blah from the example above) or Optional.empty() if absent
     */
    @Nonnull
    public static Optional<String> extractMultipartBoundary(final String multipartContentType) {
        final String[] split = multipartContentType.split("=", 2);
        if (split.length < 2) {
            return Optional.empty();
        }
        return Optional.of(split[1]);
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
     * Find the content-type that most specifically matches the content-type defined on the given request.
     * <p>
     * e.g. If the response has {@code Content-Type=text/plain} and the list of types is <code>[text/&#42;, &#42;/&#42;, text/plain]</code>
     * (all of which could match), the most specific match {@code text/plain} will be returned.
     * <p>
     * If there are no matches, will return empty.
     *
     * @param request The request to find a matching content type for
     * @param apiContentTypes The list of content types to search
     *
     * @return The most specific content type that matches the given request, or empty if none match.
     */
    public static Optional<String> findMostSpecificMatch(final Request request, final Set<String> apiContentTypes) {
        return findMostSpecificMatch(request.getHeaderValue(Headers.CONTENT_TYPE).orElse("*/*"), apiContentTypes);
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
