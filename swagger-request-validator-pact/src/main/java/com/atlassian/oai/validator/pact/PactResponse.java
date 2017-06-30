package com.atlassian.oai.validator.pact;

import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleResponse;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

public class PactResponse {

    private PactResponse() {
    }

    /**
     * Builds a {@link Response} for the Swagger validator out of the
     * original {@link au.com.dius.pact.model.Response}.
     *
     * @param originalResponse the original {@link au.com.dius.pact.model.Response}
     */
    @Nonnull
    static Response of(@Nonnull final au.com.dius.pact.model.Response originalResponse) {
        requireNonNull(originalResponse, "An original response is required");
        final SimpleResponse.Builder builder = new SimpleResponse.Builder(originalResponse.getStatus());
        if (originalResponse.getBody().isPresent()) {
            builder.withBody(originalResponse.getBody().getValue());
        }
        if (originalResponse.getHeaders() != null) {
            originalResponse.getHeaders().forEach((key, value) -> builder.withHeader(key, value));
        }
        return builder.build();
    }
}
