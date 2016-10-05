package com.atlassian.oai.validator.pact;

import com.atlassian.oai.validator.model.Request;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * Adapter for using Pact requests in the Swagger validator
 */
public class PactRequest implements Request {

    private final au.com.dius.pact.model.Request internalRequest;

    public PactRequest(@Nonnull final au.com.dius.pact.model.Request internalRequest) {
        requireNonNull(internalRequest, "An Pact request is required");
        this.internalRequest = internalRequest;
        if (this.internalRequest.getQuery() == null) {
            this.internalRequest.setQuery(new HashMap<>());
        }
    }

    @Nonnull
    @Override
    public String getPath() {
        return internalRequest.getPath();
    }

    @Nonnull
    @Override
    public Method getMethod() {
        return Method.valueOf(internalRequest.getMethod().toUpperCase());
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return internalRequest.getBody().isPresent() ? of(internalRequest.getBody().getValue()) : empty();
    }

    @Nonnull
    @Override
    public Collection<String> getQueryParameters() {
        return internalRequest.getQuery().keySet();
    }

    @Nonnull
    @Override
    public Collection<String> getQueryParameterValues(String name) {

        Collection<String> c = internalRequest.getQuery().get(name);
        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }
}
