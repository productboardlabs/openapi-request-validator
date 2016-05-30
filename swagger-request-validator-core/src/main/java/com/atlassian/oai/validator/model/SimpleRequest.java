package com.atlassian.oai.validator.model;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

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

    private SimpleRequest(@Nonnull final Method method, @Nonnull final String path, @Nullable final String body) {
        this.method = requireNonNull(method, "A method is required");
        this.path = requireNonNull(path, "A request path is required");
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

    /**
     * A builder for constructing new {@link SimpleRequest} instances.
     */
    public static class Builder {

        private String path;
        private Method method;
        private String body;

        public static Builder get(final String path) {
            return new Builder(Method.GET, path);
        }

        public Builder(final Method method, final String path) {
            this.method = requireNonNull(method, "A method is required");
            this.path = requireNonNull(path, "A path is required");
        }

        public Builder withBody(final String body) {
            this.body = body;
            return this;
        }

        public SimpleRequest build() {
            return new SimpleRequest(method, path, body);
        }
    }
}
