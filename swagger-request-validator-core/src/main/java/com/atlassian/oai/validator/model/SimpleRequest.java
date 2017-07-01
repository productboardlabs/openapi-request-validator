package com.atlassian.oai.validator.model;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * Simple immutable {@link Request} implementation.
 * <p>
 * New instances should be constructed with a {@link Builder}.
 */
public class SimpleRequest implements Request {

    private final Method method;
    private final String path;
    private final Map<String, Collection<String>> headers;
    private final Map<String, Collection<String>> queryParams;
    private final Optional<String> requestBody;

    private SimpleRequest(@Nonnull final Method method,
                          @Nonnull final String path,
                          @Nonnull final Map<String, Collection<String>> headers,
                          @Nonnull final Map<String, Collection<String>> queryParams,
                          @Nullable final String body) {
        this.method = requireNonNull(method, "A method is required");
        this.path = requireNonNull(path, "A request path is required");
        this.queryParams = requireNonNull(queryParams);
        this.headers = requireNonNull(headers);
        this.requestBody = Optional.ofNullable(body);
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
        return getFromMapOrEmptyList(name, queryParams);
    }

    @Override
    @Nonnull
    public Collection<String> getQueryParameters() {
        return Collections.unmodifiableCollection(queryParams.keySet());
    }

    @Nonnull
    @Override
    public Map<String, Collection<String>> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    @Nonnull
    @Override
    public Collection<String> getHeaderValues(final String name) {
        return getFromMapOrEmptyList(name, headers);
    }

    private static Collection<String> getFromMapOrEmptyList(String name, Map<String, Collection<String>> queryParams) {
        if (name == null || !queryParams.containsKey(name)) {
            return emptyList();
        }

        return queryParams.get(name).stream().filter(Objects::nonNull)
                .collect(collectingAndThen(toList(), Collections::unmodifiableList));
    }

    /**
     * A builder for constructing new {@link SimpleRequest} instances.
     */
    public static class Builder {

        private final Method method;
        private final String path;
        private final Multimap<String, String> headers;
        private final Multimap<String, String> queryParams;
        private String body;

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
            return new Builder(Method.PATCH, path);
        }

        public static Builder head(final String path) {
            return new Builder(Method.HEAD, path);
        }

        public static Builder options(final String path) {
            return new Builder(Method.OPTIONS, path);
        }

        public static Builder trace(final String path) {
            return new Builder(Method.TRACE, path);
        }

        public Builder(final String method, final String path) {
            this(method, path, true);
        }

        public Builder(final Method method, final String path) {
            this(method, path, true);
        }

        public Builder(final String method, final String path, final boolean queryParametersCaseSensitive) {
            this(Method.valueOf(requireNonNull(method, "A method is required").toUpperCase()),
                    path, queryParametersCaseSensitive);
        }

        public Builder(final Method method, final String path, final boolean queryParametersCaseSensitive) {
            this.method = requireNonNull(method, "A method is required");
            this.path = requireNonNull(path, "A path is required");

            this.headers = multimapBuilder(false /* header are always case insensitive */);
            this.queryParams = multimapBuilder(queryParametersCaseSensitive);
        }

        public Builder withBody(final String body) {
            this.body = body;
            return this;
        }

        public Builder withHeader(final String name, final List<String> values) {
            if (values == null || values.isEmpty()) {
                // available but not set headers are considered as empty
                headers.put(name, "");
            } else {
                headers.putAll(name, values);
            }
            return this;
        }

        public Builder withHeader(final String name, final String... values) {
            return withHeader(name, asList(values));
        }

        public Builder withQueryParam(final String name, final List<String> values) {
            if (values == null || values.isEmpty()) {
                // available but not set query parameters are considered as available but with no value
                queryParams.put(name, null);
            } else {
                queryParams.putAll(name, values);
            }
            return this;
        }

        public Builder withQueryParam(final String name, final String... values) {
            return withQueryParam(name, asList(values));
        }

        public SimpleRequest build() {
            return new SimpleRequest(method, path, headers.asMap(), queryParams.asMap(), body);
        }

        private static Multimap<String, String> multimapBuilder(final boolean caseSensitive) {
            return caseSensitive ? MultimapBuilder.hashKeys().arrayListValues().build() :
                    MultimapBuilder.treeKeys(String.CASE_INSENSITIVE_ORDER).arrayListValues().build();
        }
    }
}
