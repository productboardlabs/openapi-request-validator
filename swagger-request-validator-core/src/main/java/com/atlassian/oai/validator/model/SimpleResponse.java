package com.atlassian.oai.validator.model;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Simple immutable {@link Response} implementation
 */
public class SimpleResponse implements Response {

    private final int response;
    private final Optional<String> responseBody;

    private SimpleResponse(int response, Optional<String> responseBody) {
        this.response = response;
        this.responseBody = responseBody;
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

    /**
     * A builder for constructing new {@link SimpleResponse} instances.
     */
    public static class Builder {

        private int status;
        private String body;

        public static Builder status(int status) {
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

        public SimpleResponse build() {
            return new SimpleResponse(status, Optional.ofNullable(body));
        }

    }
}
