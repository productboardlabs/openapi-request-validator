package com.atlassian.oai.validator.util;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Optional;

public class HttpParsingUtils {

    private HttpParsingUtils() {

    }

    /**
     * Checks if the content type of a multipart/form-data request matches the consumer's content type.
     *
     * @param requestContentType - content-type of a request
     * @param consumesContentType - content-type that the API consumes
     */
    public static boolean isMultipartContentTypeAcceptedByConsumer(@Nonnull final String requestContentType,
                                                                   @Nonnull final String consumesContentType) {
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
    public static Optional<String> extractMultipartBoundary(@Nonnull final String multipartContentType) {
        final String[] split = multipartContentType.split("=", 2);
        if (split.length < 2) {
            return Optional.empty();
        }
        return Optional.of(split[1]);
    }

    /**
     * Parses the body of an HTTP request that was submitted as a multipart document (multipart/mixed)
     *
     * @param multipartContentTypeWithBoundary - the content type of a request with the boundary value, e.g.
     *                                              "multipart/mixed; boundary=foobar"
     * @param httpBody  - the body of the request, e.g.:
     *      --foobar
     *      Content-Disposition: form-data; name="something"
     *
     *      some text that you wrote in your html form ...
     *      --foobar
     *      Content-Disposition: form-data; name="anything" filename="myfile.zip"
     *
     *      content
     *      --foobar
     *      ...
     *
     * @return parsed data
     */
    @Nonnull
    public static Multimap<String, String> parseMultipartFormDataBody(@Nonnull final String multipartContentTypeWithBoundary,
                                                                      @Nonnull final String httpBody) {
        final Multimap<String, String> params = ArrayListMultimap.create();

        final Optional<String> maybeBoundary = extractMultipartBoundary(multipartContentTypeWithBoundary);
        if (!maybeBoundary.isPresent() && httpBody.isEmpty()) {
            return params;
        }

        final String boundary = maybeBoundary.get();

        final String wrappedHttpBody = StringUtils.addOpeningAndTrailingNewlines(httpBody, true);
        if (!wrappedHttpBody.endsWith("\r\n--" + boundary + "--\r\n")) {
            // Invalid multipart body: body should be empty after last delimiter
            return params;
        }

        final String[] bodyChunks = wrappedHttpBody.split("\r\n--" + boundary + "\r\n");

        if (!bodyChunks[0].isEmpty()) {
            // Invalid multipart body: body should be empty before 1st delimiter"
            return params;
        }

        for (final String chunk: bodyChunks) {
            final String[] headerBody = chunk.split("\r\n\r\n", 2);
            final Optional<String> maybeChunkName = extractFormDataName(headerBody[0]);
            maybeChunkName.ifPresent(chunkName ->
                    // Get rid of terminal boundary
                    params.put(chunkName, headerBody[1].replace("\r\n--" + boundary + "--\r\n", ""))
            );
        }
        return params;
    }

    /**
     * Parses the body of an HTTP request that was submitted as a form (application/x-www-form-urlencoded)
     *
     * @param httpBody the body of the HTTP request, e.g. "foo=bar&amp;baz=blah";
     */
    @Nonnull
    public static Multimap<String, String> parseUrlEncodedFormDataBody(@Nonnull final String httpBody) {
        final Multimap<String, String> params = ArrayListMultimap.create();
        final String[] pairs = httpBody.split("&");
        try {
            for (final String pair : pairs) {
                final String[] fields = pair.split("=");
                final String name = URLDecoder.decode(fields[0], Charsets.UTF_8.name());
                final String value = (fields.length > 1) ? URLDecoder.decode(fields[1], Charsets.UTF_8.name()) : null;
                params.put(name, value);
            }
        } catch (final UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        return params;
    }

    private static Optional<String> extractFormDataName(final String multipartBodyChunkHeader) {
        final String[] lines = multipartBodyChunkHeader.split("\r\n\r\n");
        for (final String line: lines) {
            if (line.toLowerCase().startsWith("content-disposition: form-data; name=\"")) {
                final String[] splitLine = line.split("\"", 3);
                return Optional.of(splitLine[1]);
            }
        }
        return Optional.empty();
    }
}
