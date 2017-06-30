package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleResponse;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

public class WireMockResponse {

    private WireMockResponse() {
    }

    /**
     * Builds a {@link Response} for the Swagger validator out of the
     * original {@link com.github.tomakehurst.wiremock.http.Response}.
     *
     * @param originalResponse the original {@link com.github.tomakehurst.wiremock.http.Response}
     */
    @Nonnull
    static Response of(@Nonnull final com.github.tomakehurst.wiremock.http.Response originalResponse) {
        requireNonNull(originalResponse, "An original response is required");
        final SimpleResponse.Builder builder = new SimpleResponse.Builder(originalResponse.getStatus())
                .withBody(originalResponse.getBodyAsString());
        originalResponse.getHeaders().all().forEach(header -> builder.withHeader(header.key(), header.values()));
        return builder.build();
    }
}
