package com.atlassian.oai.validator.pact;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class PactRequest implements Request {

    private final Request delegate;

    /**
     * @deprecated Use: {@link PactRequest#of(au.com.dius.pact.core.model.Request)}
     */
    @Deprecated
    public PactRequest(@Nonnull final au.com.dius.pact.core.model.Request originalRequest) {
        delegate = PactRequest.of(originalRequest);
    }

    @Nonnull
    @Override
    public String getPath() {
        return delegate.getPath();
    }

    @Nonnull
    @Override
    public Request.Method getMethod() {
        return delegate.getMethod();
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return delegate.getBody();
    }

    @Nonnull
    @Override
    public Collection<String> getQueryParameters() {
        return delegate.getQueryParameters();
    }

    @Nonnull
    @Override
    public Collection<String> getQueryParameterValues(final String name) {
        return delegate.getQueryParameterValues(name);
    }

    @Nonnull
    @Override
    public Map<String, Collection<String>> getHeaders() {
        return delegate.getHeaders();
    }

    @Nonnull
    @Override
    public Collection<String> getHeaderValues(final String name) {
        return delegate.getHeaderValues(name);
    }

    /**
     * Builds a {@link Request} for the OpenAPI validator out of the
     * original {@link au.com.dius.pact.core.model.Request}.
     *
     * @param originalRequest the original {@link au.com.dius.pact.core.model.Request}
     */
    @Nonnull
    public static Request of(@Nonnull final au.com.dius.pact.core.model.IRequest originalRequest) {
        requireNonNull(originalRequest, "An original request is required");
        final SimpleRequest.Builder builder =
                new SimpleRequest.Builder(originalRequest.getMethod(), originalRequest.getPath());
        if (originalRequest.getBody() != null && originalRequest.getBody().isPresent()) {
            builder.withBody(originalRequest.getBody().getValue());
        }
        if (originalRequest.getHeaders() != null) {
            originalRequest.getHeaders().forEach(builder::withHeader);
        }
        if (originalRequest.getQuery() != null) {
            originalRequest.getQuery().forEach(builder::withQueryParam);
        }
        return builder.build();
    }
}
