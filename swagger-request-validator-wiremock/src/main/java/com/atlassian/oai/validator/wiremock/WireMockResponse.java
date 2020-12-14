package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleResponse;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class WireMockResponse implements Response {

    private final Response delegate;

    /**
     * @deprecated Use: {@link WireMockResponse#of(com.github.tomakehurst.wiremock.http.Response)}
     */
    @Deprecated
    public WireMockResponse(@Nonnull final com.github.tomakehurst.wiremock.http.Response originalResponse) {
        delegate = WireMockResponse.of(originalResponse);
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
     * original {@link com.github.tomakehurst.wiremock.http.Response}.
     *
     * @param originalResponse the original {@link com.github.tomakehurst.wiremock.http.Response}
     */
    @Nonnull
    public static Response of(@Nonnull final com.github.tomakehurst.wiremock.http.Response originalResponse) {
        requireNonNull(originalResponse, "An original response is required");
        final SimpleResponse.Builder builder = new SimpleResponse.Builder(originalResponse.getStatus())
                .withBody(originalResponse.getBody());
        originalResponse.getHeaders().all().forEach(header -> builder.withHeader(header.key(), header.values()));
        return builder.build();
    }
}
