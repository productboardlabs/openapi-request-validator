package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.model.Response;

import javax.annotation.Nonnull;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Adapter for using WireMock responses in the Swagger Request Validator
 */
public class WireMockResponse implements Response {

    private final com.github.tomakehurst.wiremock.http.Response internalResponse;

    public WireMockResponse(@Nonnull final com.github.tomakehurst.wiremock.http.Response internalResponse) {
        this.internalResponse = requireNonNull(internalResponse, "A WireMock response is required.");
    }

    @Override
    public int getStatus() {
        return internalResponse.getStatus();
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return Optional.ofNullable(internalResponse.getBodyAsString());
    }
}
