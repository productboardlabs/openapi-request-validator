package com.atlassian.oai.validator.pact;

import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleResponse;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class PactResponse implements Response {

    private final Response delegate;

    /**
     * @deprecated Use: {@link PactResponse#of(au.com.dius.pact.core.model.Response)}
     */
    @Deprecated
    public PactResponse(@Nonnull final au.com.dius.pact.core.model.Response originalResponse) {
        delegate = PactResponse.of(originalResponse);
    }

    @Override
    public int getStatus() {
        return delegate.getStatus();
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return delegate.getBody();
    }

    @Nonnull
    @Override
    public Collection<String> getHeaderValues(final String name) {
        return delegate.getHeaderValues(name);
    }

    /**
     * Builds a {@link Response} for the OpenAPI validator out of the
     * original {@link au.com.dius.pact.core.model.Response}.
     *
     * @param originalResponse the original {@link au.com.dius.pact.core.model.Response}
     */
    @Nonnull
    public static Response of(@Nonnull final au.com.dius.pact.core.model.IResponse originalResponse) {
        requireNonNull(originalResponse, "An original response is required");
        final SimpleResponse.Builder builder = new SimpleResponse.Builder(originalResponse.getStatus());
        if (originalResponse.getBody() != null && originalResponse.getBody().isPresent()) {
            builder.withBody(originalResponse.getBody().getValue());
        }
        if (originalResponse.getHeaders() != null) {
            originalResponse.getHeaders().forEach(builder::withHeader);
        }
        return builder.build();
    }
}
