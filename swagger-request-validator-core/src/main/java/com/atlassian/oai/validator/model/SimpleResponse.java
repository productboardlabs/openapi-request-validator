package com.atlassian.oai.validator.model;

import com.atlassian.oai.validator.util.ContentTypeUtils;
import com.google.common.collect.Multimap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.atlassian.oai.validator.model.Headers.CONTENT_TYPE;
import static com.atlassian.oai.validator.model.SimpleRequest.Builder.putValuesToMapOrDefault;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * Simple immutable {@link Response} implementation
 */
public class SimpleResponse implements Response {

    private final int status;
    private final Map<String, Collection<String>> headers;
    private final Optional<Body> responseBody;

    private SimpleResponse(final int status,
                           @Nonnull final Map<String, Collection<String>> headers,
                           @Nullable final Body body) {
        this.status = status;
        this.headers = requireNonNull(headers);
        this.responseBody = Optional.ofNullable(body);
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return Optional.empty(); // not used anymore
    }

    @Nonnull
    @Override
    public Optional<Body> getResponseBody() {
        return responseBody;
    }

    @Nonnull
    @Override
    public Collection<String> getHeaderValues(final String name) {
        return SimpleRequest.getFromMapOrEmptyList(headers, name);
    }

    /**
     * A builder for constructing new {@link SimpleResponse} instances.
     */
    public static class Builder {

        private final int status;
        private final Multimap<String, String> headers;
        private Body body;
        private String bodyAsStringFallback;

        /**
         * Creates a {@link SimpleResponse.Builder} with the given HTTP status code.
         *
         * @param status the responses HTTP status code
         *
         * @return a prepared {@link SimpleResponse.Builder}
         */
        public static Builder status(final int status) {
            return new Builder(status);
        }

        /**
         * A convenience method for creating a {@link SimpleResponse.Builder} with
         * the HTTP status code 200.
         *
         * @return a prepared {@link SimpleResponse.Builder}
         */
        public static Builder ok() {
            return new Builder(200);
        }

        /**
         * A convenience method for creating a {@link SimpleResponse.Builder} with
         * the HTTP status code 204.
         *
         * @return a prepared {@link SimpleResponse.Builder}
         */
        public static Builder noContent() {
            return new Builder(204);
        }

        /**
         * A convenience method for creating a {@link SimpleResponse.Builder} with
         * the HTTP status code 400.
         *
         * @return a prepared {@link SimpleResponse.Builder}
         */
        public static Builder badRequest() {
            return new Builder(400);
        }

        /**
         * A convenience method for creating a {@link SimpleResponse.Builder} with
         * the HTTP status code 401.
         *
         * @return a prepared {@link SimpleResponse.Builder}
         */
        public static Builder unauthorized() {
            return new Builder(401);
        }

        /**
         * A convenience method for creating a {@link SimpleResponse.Builder} with
         * the HTTP status code 404.
         *
         * @return a prepared {@link SimpleResponse.Builder}
         */
        public static Builder notFound() {
            return new Builder(404);
        }

        /**
         * A convenience method for creating a {@link SimpleResponse.Builder} with
         * the HTTP status code 500.
         *
         * @return a prepared {@link SimpleResponse.Builder}
         */
        public static Builder serverError() {
            return new Builder(500);
        }

        /**
         * Creates a {@link SimpleResponse.Builder} with the given HTTP status code.
         *
         * @param status the responses HTTP status code
         */
        public Builder(final int status) {
            this.status = status;
            headers = SimpleRequest.Builder.multimapBuilder(false /* header are always case insensitive */);
        }

        /**
         * Adds a response body to this builder.
         * <p>
         * The charset of the response body will be extracted from the content-type header during
         * {@link #build()}. If no such header is specified UTF-8 is used.
         * <p>
         * For better performance use {@link #withBody(byte[])} or {@link #withBody(InputStream)} if possible.
         *
         * @param content the response body
         *
         * @return this builder
         * @see #withBody(String, Charset)
         */
        public Builder withBody(final String content) {
            this.bodyAsStringFallback = content;
            return this;
        }

        /**
         * Adds a response body as {@link String} and its {@link Charset} to this builder.
         * <p>
         * For better performance use {@link #withBody(byte[])} or {@link #withBody(InputStream)} if possible.
         *
         * @param content the request body
         * @param charset the {@link Charset} of the request body
         *
         * @return this builder
         */
        public Builder withBody(final String content, final Charset charset) {
            if (content != null && charset != null) {
                this.body = new StringBody(content, charset);
                return this;
            }
            return withBody(content);
        }

        /**
         * Adds a response body as byte array to this builder.
         *
         * @param content the response body
         *
         * @return this builder
         */
        public Builder withBody(final byte[] content) {
            this.body = content != null ? new ByteArrayBody(content) : null;
            return this;
        }

        /**
         * Adds a response body as {@link InputStream} to this builder.
         *
         * @param content the response body stream
         *
         * @return this builder
         */
        public Builder withBody(final InputStream content) {
            this.body = content != null ? new InputStreamBody(content) : null;
            return this;
        }

        /**
         * Adds a response header to this builder. If there was already a header with this
         * name the values will be added.
         * <p>
         * Headers are treated case insensitive.
         *
         * @param name the header name
         * @param values the values for this header
         *
         * @return this builder
         */
        public Builder withHeader(final String name, final List<String> values) {
            // available but not set headers are considered as empty
            putValuesToMapOrDefault(headers, name, values, "");
            return this;
        }

        /**
         * Adds a response header to this builder. If there was already a header with this
         * name the values will be added.
         * <p>
         * Headers are treated case insensitive.
         *
         * @param name the header name
         * @param values the values for this header
         *
         * @return this builder
         */
        public Builder withHeader(final String name, final String... values) {
            return withHeader(name, asList(values));
        }

        /**
         * Sets the content type header on this builder.
         * <p>
         * Equivalent to: <pre>withHeader("Content-Type", contentType);</pre>
         *
         * @param contentType The content type to set
         *
         * @return this builder
         */
        public Builder withContentType(final String contentType) {
            return withHeader(CONTENT_TYPE, contentType);
        }

        /**
         * Builds a {@link SimpleResponse} out of this builder.
         *
         * @return the build {@link SimpleResponse}
         */
        public SimpleResponse build() {
            if (body == null && bodyAsStringFallback != null) {
                final Charset charset = ContentTypeUtils.getCharsetFromContentType(headers)
                        .orElse(StandardCharsets.UTF_8);
                this.body = new StringBody(bodyAsStringFallback, charset);
            }
            return new SimpleResponse(status, headers.asMap(), body);
        }
    }
}
