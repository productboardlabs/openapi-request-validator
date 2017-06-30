package com.atlassian.oai.validator.pact;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

public class PactRequest {

    private PactRequest() {
    }

    /**
     * Builds a {@link Request} for the swagger validator out of the
     * original {@link au.com.dius.pact.model.Request}.
     *
     * @param originalRequest the original {@link au.com.dius.pact.model.Request}
     */
    @Nonnull
    static Request of(@Nonnull final au.com.dius.pact.model.Request originalRequest) {
        requireNonNull(originalRequest, "An original request is required");
        final SimpleRequest.Builder builder =
                new SimpleRequest.Builder(originalRequest.getMethod(), originalRequest.getPath());
        if (originalRequest.getBody().isPresent()) {
            builder.withBody(originalRequest.getBody().getValue());
        }
        if (originalRequest.getHeaders() != null) {
            originalRequest.getHeaders().forEach((key, value) -> builder.withHeader(key, value));
        }
        if (originalRequest.getQuery() != null) {
            originalRequest.getQuery().forEach((key, value) -> builder.withQueryParam(key, value));
        }
        return builder.build();
    }
}
