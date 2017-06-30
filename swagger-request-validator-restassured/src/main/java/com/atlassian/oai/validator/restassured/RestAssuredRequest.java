package com.atlassian.oai.validator.restassured;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import io.restassured.specification.FilterableRequestSpecification;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class RestAssuredRequest {

    private RestAssuredRequest() {
    }

    /**
     * Builds a {@link Request} for the swagger validator out of the
     * original {@link FilterableRequestSpecification}.
     *
     * @param originalRequest the original {@link FilterableRequestSpecification}
     */
    @Nonnull
    static Request of(@Nonnull final FilterableRequestSpecification originalRequest) {
        requireNonNull(originalRequest, "An original request is required");
        final SimpleRequest.Builder builder =
                new SimpleRequest.Builder(originalRequest.getMethod(), originalRequest.getDerivedPath())
                        .withBody(originalRequest.getBody());
        if (originalRequest.getHeaders() != null) {
            originalRequest.getHeaders().forEach(header -> builder.withHeader(header.getName(), header.getValue()));
        }
        // the query params seems wrongly typed - they can contain either a list of strings or a string
        new HashMap<String, Object>(originalRequest.getQueryParams())
                .forEach((key, value) -> {
                    if (value instanceof List) {
                        builder.withQueryParam(key, (List) value);
                    } else if (value instanceof String) {
                        builder.withQueryParam(key, (String) value);
                    }
                });
        if ("GET".equalsIgnoreCase(originalRequest.getMethod())) {
            originalRequest.getRequestParams().forEach((key, value) -> {
                builder.withQueryParam(key, value);
            });
        }
        return builder.build();
    }
}
