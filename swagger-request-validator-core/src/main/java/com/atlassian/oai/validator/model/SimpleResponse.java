package com.atlassian.oai.validator.model;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;

/**
 * Simple immutable {@link Response} implementation
 */
public class SimpleResponse implements Response {

    private final int response;
    private final Optional<String> responseBody;
    private final Map<String, Collection<String>> headers;

    private SimpleResponse(final int response,
                           @Nonnull final Optional<String> responseBody,
                           @Nonnull final Map<String, Collection<String>> headers) {
        this.response = response;
        this.responseBody = responseBody;
        this.headers = headers;
    }

    @Override
    public int getStatus() {
        return response;
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return responseBody;
    }

    @Nonnull
    @Override
    public Collection<String> getHeaderValues(final String name) {
        if (name != null && headers.containsKey(name.toLowerCase())) {
            return Collections.unmodifiableCollection(headers.get(name.toLowerCase()));
        }
        return Collections.emptyList();
    }

    /**
     * A builder for constructing new {@link SimpleResponse} instances.
     */
    public static class Builder {

        private int status;
        private String body;
        private Multimap<String, String> headers = MultimapBuilder.hashKeys().arrayListValues().build();

        public static Builder status(final int status) {
            return new Builder(status);
        }

        public static Builder ok() {
            return new Builder(200);
        }

        public static Builder noContent() {
            return new Builder(204);
        }

        public static Builder badRequest() {
            return new Builder(400);
        }

        public static Builder notFound() {
            return new Builder(404);
        }

        public Builder(final int status) {
            this.status = status;
        }

        public Builder withBody(final String body) {
            this.body = body;
            return this;
        }

        public Builder withHeader(final String key, final String... values) {
            this.headers.putAll(key, asList(values));
            return this;
        }

        public SimpleResponse build() {
            return new SimpleResponse(status, Optional.ofNullable(body), headers.asMap());
        }

    }
}
