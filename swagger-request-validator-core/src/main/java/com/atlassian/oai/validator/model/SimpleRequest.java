package com.atlassian.oai.validator.model;


import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * Simple immutable {@link Request} implementation.
 * <p>
 * New instances should be constructed with a {@link Builder}.
 */
public class SimpleRequest implements Request {

    private final String path;
    private final Method method;
    private final Optional<String> requestBody;
    private final Map<String, Collection<String>> queryParams;
    private final Map<String, String> headers;

    private SimpleRequest(@Nonnull final Method method,
                          @Nonnull final String path,
                          @Nullable final String body,
                          @Nonnull final Map<String, Collection<String>> queryParams,
                          @Nonnull final Map<String, String> headers) {
        this.method = requireNonNull(method, "A method is required");
        this.path = requireNonNull(path, "A request path is required");
        this.requestBody = Optional.ofNullable(body);
        this.queryParams = requireNonNull(queryParams);
        this.headers = headers;
    }

    @Nonnull
    @Override
    public String getPath() {
        return path;
    }

    @Nonnull
    @Override
    public Method getMethod() {
        return method;
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return requestBody;
    }

    @Override
    @Nonnull
    public Collection<String> getQueryParameterValues(final String name) {
        if (name != null && queryParams.containsKey(name.toLowerCase())) {
            return Collections.unmodifiableCollection(queryParams.get(name.toLowerCase()));
        }
        return Collections.emptyList();
    }

    @Override
    @Nonnull
    public Collection<String> getQueryParameters() {
        return Collections.unmodifiableCollection(queryParams.keySet());
    }

    @Nonnull
    @Override
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * A builder for constructing new {@link SimpleRequest} instances.
     */
    public static class Builder {

        private String path;
        private Method method;
        private String body;
        private Multimap<String, String> queryParams = MultimapBuilder.hashKeys().arrayListValues().build();
        private Map<String, String> headers = new HashMap<>();

        public static Builder get(final String path) {
            return new Builder(Method.GET, path);
        }

        public static Builder put(final String path) {
            return new Builder(Method.PUT, path);
        }

        public static Builder post(final String path) {
            return new Builder(Method.POST, path);
        }

        public static Builder delete(final String path) {
            return new Builder(Method.DELETE, path);
        }

        public static Builder patch(final String path) {
            return new Builder(Method.POST, path);
        }

        public Builder(final Method method, final String path) {
            this.method = requireNonNull(method, "A method is required");
            this.path = requireNonNull(path, "A path is required");
        }

        public Builder withBody(final String body) {
            this.body = body;
            return this;
        }

        public Builder withHeaders(final Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder withQueryParam(final String name, final String... values) {
            queryParams.putAll(name.toLowerCase(), asList(values));
            return this;
        }

        public SimpleRequest build() {
            return new SimpleRequest(method, path, body, queryParams.asMap(), headers);
        }
    }
}
