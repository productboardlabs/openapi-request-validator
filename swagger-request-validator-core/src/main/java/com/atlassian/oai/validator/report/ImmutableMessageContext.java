package com.atlassian.oai.validator.report;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.Objects;
import java.util.Optional;

class ImmutableMessageContext implements ValidationReport.MessageContext {

    private final String requestPath;
    private final Request.Method method;
    private final ApiOperation apiOperation;
    private final Parameter parameter;

    private final Integer responseStatus;
    private final ApiResponse apiResponseDefinition;

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
    public Optional<ApiResponse> getApiResponseDefinition() {
        return Optional.ofNullable(apiResponseDefinition);
    }

    @Override
    public Optional<Location> getLocation() {
        return Optional.ofNullable(location);
    }

    @Override
    public boolean hasData() {
        return requestPath != null ||
                method != null ||
                apiOperation != null ||
                parameter != null ||
                responseStatus != null ||
                apiResponseDefinition != null ||
                location != null;
    }

    @Override
    public ValidationReport.MessageContext enhanceWith(final ValidationReport.MessageContext other) {
        return ValidationReport.MessageContext
                .from(this)
                .withAdditionalDataFrom(other)
                .build();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ImmutableMessageContext that = (ImmutableMessageContext) o;
        return Objects.equals(requestPath, that.requestPath) &&
                method == that.method &&
                Objects.equals(apiOperation, that.apiOperation) &&
                Objects.equals(parameter, that.parameter) &&
                Objects.equals(responseStatus, that.responseStatus) &&
                Objects.equals(apiResponseDefinition, that.apiResponseDefinition) &&
                location == that.location;
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestPath, method, apiOperation, parameter, responseStatus, apiResponseDefinition, location);
    }
}
