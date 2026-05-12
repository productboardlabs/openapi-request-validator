package com.atlassian.oai.validator.wiremock.junit5;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.github.tomakehurst.wiremock.common.Urls;
import com.github.tomakehurst.wiremock.http.QueryParameter;

import javax.annotation.Nonnull;
import java.net.URI;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public final class WireMockRequestResponseUtil {

    /**
     * Builds a {@link Request} for the OpenAPI validator out of the
     * original {@link com.github.tomakehurst.wiremock.http.Request}.
     *
     * @param loggedRequest the original WireMock {@link com.github.tomakehurst.wiremock.verification.LoggedRequest }
     */
    @Nonnull
    public static Request toRequest(@Nonnull final com.github.tomakehurst.wiremock.verification.LoggedRequest loggedRequest) {
        requireNonNull(loggedRequest, "An original request is required");

        final URI uri = URI.create(loggedRequest.getUrl());
        final Map<String, QueryParameter> queryParameterMap = Urls.splitQuery(uri);

        final SimpleRequest.Builder builder =
                new SimpleRequest.Builder(loggedRequest.getMethod().getName(), uri.getPath())
                        .withBody(loggedRequest.getBody());
        loggedRequest.getHeaders().all().forEach(header -> builder.withHeader(header.key(), header.values()));
        queryParameterMap.forEach((key, value) -> builder.withQueryParam(key, value.values()));
        return builder.build();
    }

    /**
     * Builds a {@link Response} for the OpenAPI validator out of the
     * original {@link com.github.tomakehurst.wiremock.http.LoggedResponse}.
     *
     * @param loggedResponse the original WireMock {@link com.github.tomakehurst.wiremock.http.LoggedResponse}
     */
    @Nonnull
    public static Response toResponse(@Nonnull final com.github.tomakehurst.wiremock.http.LoggedResponse loggedResponse) {
        requireNonNull(loggedResponse, "An original response is required");
        final SimpleResponse.Builder builder = new SimpleResponse.Builder(loggedResponse.getStatus())
                .withBody(loggedResponse.getBody());
        loggedResponse.getHeaders().all().forEach(header -> builder.withHeader(header.key(), header.values()));
        return builder.build();
    }

    private WireMockRequestResponseUtil() {
    }
}
