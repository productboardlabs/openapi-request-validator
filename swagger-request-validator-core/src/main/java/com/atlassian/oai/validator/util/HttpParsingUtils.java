package com.atlassian.oai.validator.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Optional;

import static java.lang.Double.parseDouble;
import static java.lang.Long.parseLong;
import static java.util.stream.Collectors.toList;

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
                final String name = URLDecoder.decode(fields[0], Charsets.UTF_8.name()).trim();
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

    /**
     * Parses the body of an HTTP request that was submitted as a form (application/x-www-form-urlencoded)
     * and transform it into a JSON representation that can be validated with the schema validator.
     * <p>
     * Makes some guesses about the intended JSON type based on the field value. Note that this may lead
     * to erroneous validation failures if it guesses wrong.
     *
     * @param httpBody the body of the HTTP request, e.g. "foo=bar&amp;baz=blah";
     *
     * @return A JSON representation of the formdata
     */
    @Nonnull
    public static JsonNode parseUrlEncodedFormDataBodyAsJsonNode(@Nonnull final String httpBody) {
        final Multimap<String, String> data = parseUrlEncodedFormDataBody(httpBody);
        final ObjectNode root = new ObjectNode(JsonNodeFactory.instance);
        data.asMap().forEach((key, values) -> root.set(key, toJsonObject(values)));
        return root;
    }

    /**
     * Parses the body of an HTTP request that was submitted as a form (application/x-www-form-urlencoded)
     * and transform it into a JSON representation.
     *
     * @deprecated Use {@link #parseUrlEncodedFormDataBodyAsJsonNode(String)} instead.
     */
    @Deprecated
    @Nonnull
    public static String parseUrlEncodedFormDataBodyAsJson(@Nonnull final String httpBody) {
        return parseUrlEncodedFormDataBodyAsJsonNode(httpBody).toString();
    }

    private static JsonNode toJsonObject(final Collection<String> values) {
        if (values.size() == 0) {
            return NullNode.getInstance();
        }
        if (values.size() == 1) {
            return toJsonObject(values.iterator().next());
        }
        return new ArrayNode(
                JsonNodeFactory.instance,
                values.stream().map(HttpParsingUtils::toJsonObject).collect(toList())
        );
    }

    private static JsonNode toJsonObject(@Nullable final String value) {
        if (value == null || value.equalsIgnoreCase("null")) {
            return NullNode.getInstance();
        }
        final String trimmed = value.trim();
        if (trimmed.equalsIgnoreCase("false")) {
            return BooleanNode.getFalse();
        }
        if (trimmed.equalsIgnoreCase("true")) {
            return BooleanNode.getTrue();
        }
        //CHECKSTYLE:OFF: EmptyCatchBlock
        try {
            return new LongNode(parseLong(trimmed));
        } catch (final NumberFormatException e) {
            // Do nothing
        }
        try {
            return new DoubleNode(parseDouble(trimmed));
        } catch (final NumberFormatException e) {
            // Do nothing
        }
        //CHECKSTYLE:ON: EmptyCatchBlock
        return new TextNode(trimmed);
    }
}
