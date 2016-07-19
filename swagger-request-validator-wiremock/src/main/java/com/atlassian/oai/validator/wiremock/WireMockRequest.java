package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.model.Request;

import javax.annotation.Nonnull;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Adapter for using WireMock requests in the Swagger Request Validator
 */
public class WireMockRequest implements Request {

    private final com.github.tomakehurst.wiremock.http.Request internalRequest;

    public WireMockRequest(@Nonnull final com.github.tomakehurst.wiremock.http.Request internalRequest) {
        this.internalRequest = requireNonNull(internalRequest, "A WireMock request is required.");
    }

    @Nonnull
    @Override
    public String getPath() {
        return internalRequest.getUrl();
    }

    @Nonnull
    @Override
    public Method getMethod() {
        return Method.valueOf(internalRequest.getMethod().getName());
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return Optional.ofNullable(internalRequest.getBodyAsString());
    }
}
