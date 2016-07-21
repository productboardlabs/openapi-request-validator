package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.model.Request;
import com.github.tomakehurst.wiremock.common.Urls;
import com.github.tomakehurst.wiremock.http.QueryParameter;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Adapter for using WireMock requests in the Swagger Request Validator
 */
public class WireMockRequest implements Request {

    private final com.github.tomakehurst.wiremock.http.Request internalRequest;
    private final Map<String, QueryParameter> queryParameterMap;

    public WireMockRequest(@Nonnull final com.github.tomakehurst.wiremock.http.Request internalRequest) {
        this.internalRequest = requireNonNull(internalRequest, "A WireMock request is required.");
        this.queryParameterMap = Urls.splitQuery(URI.create(internalRequest.getUrl()));
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

    @Nonnull
    @Override
    public Collection<String> getQueryParameters() {
        return queryParameterMap.keySet();
    }

    @Nonnull
    @Override
    public Collection<String> getQueryParameterValues(String name) {
        return queryParameterMap.getOrDefault(name, QueryParameter.absent(name)).values();
    }
}
