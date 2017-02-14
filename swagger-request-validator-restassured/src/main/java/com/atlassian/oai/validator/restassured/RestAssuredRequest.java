package com.atlassian.oai.validator.restassured;

import com.atlassian.oai.validator.model.Request;
import io.restassured.http.Header;
import io.restassured.specification.FilterableRequestSpecification;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
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
        final Set<String> result = new HashSet<>();
        result.addAll(internalRequest.getQueryParams().keySet());
        if (isGetRequest()) {
            result.addAll(internalRequest.getRequestParams().keySet());
        }
        return result;
    }

    @Nonnull
    @Override
    public Collection<String> getQueryParameterValues(String name) {
        if (internalRequest.getQueryParams().containsKey(name)) {
            return singleton(internalRequest.getQueryParams().get(name));
        }
        if (isGetRequest() && internalRequest.getRequestParams().containsKey(name)) {
            return singleton(internalRequest.getRequestParams().get(name));
        }
        return emptyList();
    }

    private boolean isGetRequest() {
        return getMethod() == Method.GET;
    }

    @Nonnull
    @Override
    public Map<String, String> getHeaders() {
        return internalRequest.getHeaders().asList().stream().collect(Collectors.toMap(Header::getName, Header::getValue));
    }
}
