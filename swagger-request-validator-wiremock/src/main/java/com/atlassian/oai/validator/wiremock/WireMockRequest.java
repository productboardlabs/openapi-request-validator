package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.model.Request;
import com.github.tomakehurst.wiremock.common.Urls;
import com.github.tomakehurst.wiremock.http.MultiValue;
import com.github.tomakehurst.wiremock.http.QueryParameter;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Adapter for using WireMock requests in the Swagger Request Validator
 */
public class WireMockRequest implements Request {

    private final com.github.tomakehurst.wiremock.http.Request internalRequest;
    private final String requestPath;
    private final Map<String, QueryParameter> queryParameterMap;

    public WireMockRequest(@Nonnull final com.github.tomakehurst.wiremock.http.Request internalRequest) {
        this.internalRequest = requireNonNull(internalRequest, "A WireMock request is required.");

        final URI uri = URI.create(internalRequest.getUrl());
        this.queryParameterMap = Urls.splitQuery(uri);
        this.requestPath = uri.getPath();
    }

    @Nonnull
    @Override
    public String getPath() {
        return requestPath;
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
        if (queryParameterMap.containsKey(name)) {
            return queryParameterMap.get(name).values();
        }
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Map<String, String> getHeaders() {
        return internalRequest.getHeaders().all().stream().collect(Collectors.toMap(MultiValue::key, MultiValue::firstValue));
    }
}
