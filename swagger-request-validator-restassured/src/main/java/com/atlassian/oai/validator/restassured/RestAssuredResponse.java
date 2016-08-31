package com.atlassian.oai.validator.restassured;

import com.atlassian.oai.validator.model.Response;

import javax.annotation.Nonnull;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * An adapter for using rest-assured {@link io.restassured.response.Response} with the Swagger Request Validator.
 */
public class RestAssuredResponse implements Response {

    private final io.restassured.response.Response internalResponse;

    public RestAssuredResponse(@Nonnull final io.restassured.response.Response internalResponse) {
        this.internalResponse = requireNonNull(internalResponse, "A response is required");
    }

    @Override
    public int getStatus() {
        return internalResponse.getStatusCode();
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return Optional.ofNullable(internalResponse.getBody().asString());
    }
}
