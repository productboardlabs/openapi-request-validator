package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.github.tomakehurst.wiremock.common.Urls;
import com.github.tomakehurst.wiremock.http.QueryParameter;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class WireMockRequest implements Request {

    private final Request delegate;

    /**
     * @deprecated Use: {@link WireMockRequest#of(com.github.tomakehurst.wiremock.http.Request)}
     */
    @Deprecated
    public WireMockRequest(@Nonnull final com.github.tomakehurst.wiremock.http.Request originalRequest) {
        delegate = WireMockRequest.of(originalRequest);
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
     * original {@link com.github.tomakehurst.wiremock.http.Request}.
     *
     * @param originalRequest the original {@link com.github.tomakehurst.wiremock.http.Request}
     */
    @Nonnull
    public static Request of(@Nonnull final com.github.tomakehurst.wiremock.http.Request originalRequest) {
        requireNonNull(originalRequest, "An original request is required");

        final URI uri = URI.create(originalRequest.getUrl());
        final Map<String, QueryParameter> queryParameterMap = Urls.splitQuery(uri);

        final SimpleRequest.Builder builder =
                new SimpleRequest.Builder(originalRequest.getMethod().getName(), uri.getPath())
                        .withBody(originalRequest.getBody());
        originalRequest.getHeaders().all().forEach(header -> builder.withHeader(header.key(), header.values()));
        queryParameterMap.forEach((key, value) -> builder.withQueryParam(key, value.values()));
        return builder.build();
    }
}
