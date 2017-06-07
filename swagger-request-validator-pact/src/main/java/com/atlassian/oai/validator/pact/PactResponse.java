package com.atlassian.oai.validator.pact;

import com.atlassian.oai.validator.model.Response;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

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

        if (this.internalResponse.getHeaders() != null) {
            final TreeMap<String, String> caseInsensitiveHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            caseInsensitiveHeaders.putAll(internalResponse.getHeaders());
            this.internalResponse.setHeaders(caseInsensitiveHeaders);
        }
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
        final Map<String, String> headers = internalResponse.getHeaders();
        if (headers != null && headers.containsKey(name.toLowerCase())) {
            return singleton(headers.get(name.toLowerCase()));
        }
        return emptyList();
    }
}
