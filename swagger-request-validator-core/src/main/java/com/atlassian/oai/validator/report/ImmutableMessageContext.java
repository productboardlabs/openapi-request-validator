package com.atlassian.oai.validator.report;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import io.swagger.models.Response;
import io.swagger.models.parameters.Parameter;

import java.util.Optional;

class ImmutableMessageContext implements ValidationReport.MessageContext {

    private final String requestPath;
    private final Request.Method method;
    private final ApiOperation apiOperation;
    private final Parameter parameter;

    private final Integer responseStatus;
    private final Response apiResponseDefinition;

    private final Location location;

    ImmutableMessageContext(final Builder builder) {
        requestPath = builder.requestPath;
        method = builder.method;
        apiOperation = builder.apiOperation;
        parameter = builder.parameter;
        responseStatus = builder.responseStatus;
        apiResponseDefinition = builder.apiResponse;
        location = builder.location;
    }

    @Override
    public Optional<String> getRequestPath() {
        return Optional.ofNullable(requestPath);
    }

    @Override
    public Optional<Request.Method> getRequestMethod() {
        return Optional.ofNullable(method);
    }

    @Override
    public Optional<ApiOperation> getApiOperation() {
        return Optional.ofNullable(apiOperation);
    }

    @Override
    public Optional<Parameter> getParameter() {
        return Optional.ofNullable(parameter);
    }

    @Override
    public Optional<Integer> getResponseStatus() {
        return Optional.ofNullable(responseStatus);
    }

    @Override
    public Optional<Response> getApiResponseDefinition() {
        return Optional.ofNullable(apiResponseDefinition);
    }

    @Override
    public Optional<Location> getLocation() {
        return Optional.ofNullable(location);
    }

    @Override
    public ValidationReport.MessageContext enhanceWith(final ValidationReport.MessageContext other) {
        return ValidationReport.MessageContext
                .from(this)
                .withAdditionalDataFrom(other)
                .build();
    }
}
