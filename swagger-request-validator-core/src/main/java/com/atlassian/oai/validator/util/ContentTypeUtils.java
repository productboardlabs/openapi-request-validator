package com.atlassian.oai.validator.util;

import com.atlassian.oai.validator.model.Headers;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.net.MediaType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

import static com.google.common.net.MediaType.FORM_DATA;
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
        return isJsonContentType(request.getContentType().orElse(null));
    }

    /**
     * @return Whether the content-type of this response (defined in the Content-Type header) is a JSON type.
     */
    public static boolean isJsonContentType(final Response response) {
        return isJsonContentType(response.getContentType().orElse(null));
    }

    /**
     * @return Whether the provided content-type is a JSON type (includes JSON suffix).
     */
    public static boolean isJsonContentType(@Nullable final String contentType) {
        final Optional<MediaType> optionalMediaType = parseContentType(contentType);
        return optionalMediaType.map(mediaType -> {
            if (mediaType.withoutParameters().is(JSON_UTF_8.withoutParameters())) {
                return true;
            }
            if (mediaType.type().equals("application")) {
                return mediaType.subtype().endsWith("+json");
            }
            return false;
        }).orElse(false);
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
                    .map(ParsedContentType::of)
                    .sorted(new ParsedContentTypeComparator())
                    .filter(ct -> ct.matches(candidate))
                    .map(ParsedContentType::getContentType)
                    .findFirst();
        } catch (final IllegalArgumentException e) {
            return empty();
        }
    }

    /**
     * Returns whether the candidate media type matches any of the applied API content type expressions.
     * <p>
     * Supports matching against media type ranges e.g. "text/*". Note that if the global match "&#42;/&#42;"
     * is included will always return {@code true}.
     *
     * @param candidate The candidate type to match (e.g. from the request or response header)
     * @param apiContentTypes The content types defined in the API to match against. Can be media type ranges e.g. "text/*".
     *
     * @return {@code true} if the candidate matches against any of the provided API-defined content types.
     */
    public static boolean matchesAny(final String candidate, final Collection<String> apiContentTypes) {
        if (candidate == null || candidate.isEmpty()) {
            return false;
        }
        return matchesAny(MediaType.parse(candidate), apiContentTypes);
    }

    /**
     * Returns whether the candidate media type matches any of the applied API content type expressions.
     * <p>
     * Supports matching against media type ranges e.g. "text/*". Note that if the global match "&#42;/&#42;"
     * is included will always return {@code true}.
     *
     * @param candidate The candidate type to match (e.g. from the request or response header)
     * @param apiContentTypes The content types defined in the API to match against. Can be media type ranges e.g. "text/*".
     *
     * @return {@code true} if the candidate matches against any of the provided API-defined content types.
     */
    public static boolean matchesAny(final MediaType candidate, final Collection<String> apiContentTypes) {
        return apiContentTypes.stream()
                .map(com.google.common.net.MediaType::parse)
                .anyMatch(apiMediaType -> candidate.withoutParameters().is(apiMediaType.withoutParameters()));
    }

    /**
     * Extract and return the charset from the given content-type, if it is defined.
     * <p>
     * If no content-type is provided, or no charset is defined in the content-type, will return {@code empty}
     *
     * @param contentType The content-type value to extract the charset from
     *
     * @return The charset of the content-type, or {@code empty} if none is defined.
     */
    public static Optional<Charset> getCharsetFromContentType(@Nullable final String contentType) {
        return parseContentType(contentType)
                .flatMap(m -> Optional.ofNullable(m.charset().orNull()));
    }

    /**
     * Resolves the content-type of the given headers, if it is defined. And then extracts and returns the charset
     * from the found content-type header.
     * <p>
     * If no content-type is provided, or no charset is defined in the content-type, will return {@code empty}
     *
     * @param headers the headers that contain the content-type header
     *
     * @return The charset of the content-type, or {@code empty} if none is defined.
     */
    public static Optional<Charset> getCharsetFromContentType(@Nullable final Multimap<String, String> headers) {
        if (headers != null) {
            // Multimap#get(String) always returns at least an empty collection even if the key is not available
            final String contentType = Iterables.getFirst(headers.get(Headers.CONTENT_TYPE), null);
            return getCharsetFromContentType(contentType);
        }
        return Optional.empty();
    }

    /**
     * Return whether the given content types includes a "global match" wildcard
     *
     * @param apiContentTypes The content types to check
     *
     * @return {@code true} if at least one entry in the given content types is the global match
     */
    public static boolean containsGlobalAccept(final Collection<String> apiContentTypes) {
        return apiContentTypes.stream().anyMatch(c -> c.equals("*/*"));
    }

    private static Optional<MediaType> parseContentType(@Nullable final String contentType) {
        if (contentType == null) {
            return empty();
        }
        try {
            return Optional.ofNullable(MediaType.parse(contentType));
        } catch (final IllegalArgumentException e) {
            return empty();
        }
    }

    private static class ParsedContentType {
        private final String contentType;
        private final MediaType mediaType;

        static ParsedContentType of(final String contentType) {
            return new ParsedContentType(contentType, MediaType.parse(contentType));
        }

        private ParsedContentType(final String contentType, final MediaType mediaType) {
            this.contentType = contentType;
            this.mediaType = mediaType;
        }

        boolean matches(final String contentType) {
            return MediaType.parse(contentType).withoutParameters().is(mediaType.withoutParameters());
        }

        public String getContentType() {
            return contentType;
        }

        public MediaType getMediaType() {
            return mediaType;
        }
    }

    private static class ParsedContentTypeComparator implements Comparator<ParsedContentType> {
        @Override
        public int compare(final ParsedContentType o1, final ParsedContentType o2) {
            return countWildcards(o1.getMediaType()) - countWildcards(o2.getMediaType());
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
