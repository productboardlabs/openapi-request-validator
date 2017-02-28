package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.model.Response;
import com.github.tomakehurst.wiremock.http.HttpHeader;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

import static java.util.Collections.emptyList;
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

    @Nonnull
    @Override
    public Collection<String> getHeaderValues(final String name) {
        final HttpHeader header = internalResponse.getHeaders().getHeader(name);
        if (header.isPresent()) {
            return header.values();
        }
        return emptyList();
    }
}
