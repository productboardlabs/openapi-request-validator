package com.atlassian.oai.validator.report;

import com.atlassian.oai.validator.model.ApiOperation;
import io.swagger.models.parameters.Parameter;

import java.util.Optional;

class ImmutableMessageContext implements ValidationReport.MessageContext {

    private final ApiOperation apiOperation;
    private final Parameter parameter;
    private final String requestPath;

    ImmutableMessageContext(final Builder builder) {
        apiOperation = builder.apiOperation;
        parameter = builder.parameter;
        requestPath = builder.requestPath;
    }

    @Override
    public Optional<String> getRequestPath() {
        return Optional.ofNullable(requestPath);
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
    public ValidationReport.MessageContext enhanceWith(final ValidationReport.MessageContext other) {
        return ValidationReport.MessageContext
                .from(this)
                .withAdditionalDataFrom(other)
                .build();
    }
}
