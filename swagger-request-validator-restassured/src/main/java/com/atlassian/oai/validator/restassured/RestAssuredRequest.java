package com.atlassian.oai.validator.restassured;

import com.atlassian.oai.validator.model.Request;
import io.restassured.specification.FilterableRequestSpecification;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

import static java.util.Collections.singleton;
import static java.util.Objects.requireNonNull;

/**
 * An adapter for using rest-assured {@link FilterableRequestSpecification} with the Swagger Request Validator.
 */
public class RestAssuredRequest implements Request {

    private final FilterableRequestSpecification internalRequest;

    public RestAssuredRequest(@Nonnull final FilterableRequestSpecification internalRequest) {
        this.internalRequest = requireNonNull(internalRequest, "A request is required");
    }

    @Nonnull
    @Override
    public String getPath() {
        return internalRequest.getDerivedPath();
    }

    @Nonnull
    @Override
    public Method getMethod() {
        return Method.valueOf(internalRequest.getMethod().toUpperCase());
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return Optional.ofNullable(internalRequest.getBody());
    }

    @Nonnull
    @Override
    public Collection<String> getQueryParameters() {
        return internalRequest.getQueryParams().keySet();
    }

    @Nonnull
    @Override
    public Collection<String> getQueryParameterValues(String name) {
        return singleton(internalRequest.getQueryParams().get(name));
    }
}
