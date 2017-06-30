package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.github.tomakehurst.wiremock.common.Urls;
import com.github.tomakehurst.wiremock.http.QueryParameter;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class WireMockRequest {

    private WireMockRequest() {
    }

    /**
     * Builds a {@link Request} for the swagger validator out of the
     * original {@link com.github.tomakehurst.wiremock.http.Request}.
     *
     * @param originalRequest the original {@link com.github.tomakehurst.wiremock.http.Request}
     */
    @Nonnull
    static Request of(@Nonnull final com.github.tomakehurst.wiremock.http.Request originalRequest) {
        requireNonNull(originalRequest, "An original request is required");

        final URI uri = URI.create(originalRequest.getUrl());
        final Map<String, QueryParameter> queryParameterMap = Urls.splitQuery(uri);

        final SimpleRequest.Builder builder =
                new SimpleRequest.Builder(originalRequest.getMethod().getName(), uri.getPath())
                        .withBody(originalRequest.getBodyAsString());
        originalRequest.getHeaders().all().forEach(header -> builder.withHeader(header.key(), header.values()));
        queryParameterMap.forEach((key, value) -> builder.withQueryParam(key, value.values()));
        return builder.build();
    }
}
