package com.atlassian.oai.validator.pact;

import com.atlassian.oai.validator.model.Request;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toMap;

/**
 * Adapter for using Pact requests in the Swagger validator
 */
public class PactRequest implements Request {

    private final au.com.dius.pact.model.Request internalRequest;

    public PactRequest(@Nonnull final au.com.dius.pact.model.Request internalRequest) {
        requireNonNull(internalRequest, "An Pact request is required");
        this.internalRequest = internalRequest;

        if (this.internalRequest.getHeaders() != null) {
            final TreeMap<String, String> caseInsensitiveHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitiveHeaders.putAll(internalRequest.getHeaders());
            this.internalRequest.setHeaders(caseInsensitiveHeaders);
        }

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
    public Collection<String> getQueryParameterValues(final String name) {
        return internalRequest.getQuery().getOrDefault(name, emptyList());
    }

    @Nonnull
    @Override
    public Map<String, Collection<String>> getHeaders() {
        return internalRequest.getHeaders().entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, v -> singleton(v.getValue())));
    }

    @Nonnull
    @Override
    public Collection<String> getHeaderValues(final String name) {
        final Map<String, String> headers = internalRequest.getHeaders();
        if (headers != null && headers.containsKey(name.toLowerCase())) {
            return singleton(headers.get(name.toLowerCase()));
        }
        return emptyList();
    }
}
