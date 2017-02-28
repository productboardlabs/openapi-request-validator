package com.atlassian.oai.validator.pact;

import com.atlassian.oai.validator.model.Response;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * Adapter for using Pact responses in the Swagger validator
 */
public class PactResponse implements Response {

    private final au.com.dius.pact.model.Response internalResponse;

    public PactResponse(@Nonnull final au.com.dius.pact.model.Response pactResponse) {
        requireNonNull(pactResponse, "A Pact response is required");
        this.internalResponse = pactResponse;
    }

    @Override
    public int getStatus() {
        return internalResponse.getStatus();
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return internalResponse.getBody().isPresent() ? of(internalResponse.getBody().getValue()) : empty();
    }

    @Nonnull
    @Override
    public Collection<String> getHeaderValues(final String name) {
        if (internalResponse.getHeaders() != null && internalResponse.getHeaders().containsKey(name.toLowerCase())) {
            return singleton(internalResponse.getHeaders().get(name.toLowerCase()));
        }
        return emptyList();
    }
}
