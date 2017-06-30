package com.atlassian.oai.validator.restassured;

import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleResponse;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

public class RestAssuredResponse {

    private RestAssuredResponse() {
    }

    /**
     * Builds a {@link Response} for the Swagger validator out of the
     * original {@link io.restassured.response.Response}.
     *
     * @param originalResponse the original {@link io.restassured.response.Response}
     */
    @Nonnull
    static Response of(@Nonnull final io.restassured.response.Response originalResponse) {
        requireNonNull(originalResponse, "An original response is required");
        final SimpleResponse.Builder builder = new SimpleResponse.Builder(originalResponse.getStatusCode())
                .withBody(originalResponse.getBody().asString());
        if (originalResponse.getHeaders() != null) {
            originalResponse.getHeaders().forEach(header -> builder.withHeader(header.getName(), header.getValue()));
        }
        return builder.build();
    }
}
