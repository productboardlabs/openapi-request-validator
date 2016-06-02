package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.parameter.ParameterValidators;
import com.atlassian.oai.validator.report.MutableValidationReport;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;

import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Validate a request against a given API operation.
 */
public class RequestValidator {

    private final SchemaValidator schemaValidator;

    public RequestValidator(final SchemaValidator schemaValidator) {
        this.schemaValidator = requireNonNull(schemaValidator, "A schema validator is required");
    }

    /**
     * Validate the request against the given API operation
     *
     * @param requestPath The normalised path the request is on
     * @param request The request to validate
     * @param apiOperation The operation to validate the request against
     *
     * @return A validation report containing validation errors
     */
    public ValidationReport validateRequest(final NormalisedPath requestPath,
                                            final Request request,
                                            final ApiOperation apiOperation) {
        return ValidationReport.empty()
                .merge(validateRequestParameters(apiOperation, requestPath))
                .merge(validateRequestBody(apiOperation, request.getBody()));
    }

    private ValidationReport validateRequestBody(final ApiOperation apiOperation, final Optional<String> requestBody) {
        final Optional<Parameter> bodyParameter = apiOperation.getOperation().getParameters()
                .stream().filter(p -> p.getIn().equalsIgnoreCase("body")).findFirst();

        final MutableValidationReport validationReport = new MutableValidationReport();
        if (requestBody.isPresent() && !bodyParameter.isPresent()) {
            validationReport.addError(format("No request body is expected for %s on path '%s'",
                    apiOperation.getMethod(), apiOperation.getPathString().original()));
            return validationReport;
        }

        if (!bodyParameter.isPresent()) {
            return validationReport;
        }

        if (!requestBody.isPresent()) {
            if (bodyParameter.get().getRequired()) {
                validationReport.addError(format("%s on path '%s' requires a request body. None found.",
                        apiOperation.getMethod(), apiOperation.getPathString().original()));
            }
            return validationReport;
        }

        return validationReport
                .merge(schemaValidator.validate(requestBody.get(), ((BodyParameter)bodyParameter.get()).getSchema()));
    }

    private ValidationReport validateRequestParameters(final ApiOperation apiOperation, final NormalisedPath requestPath) {

        ValidationReport validationReport = ValidationReport.empty();
        for (int i = 0; i < apiOperation.getPathString().parts().size(); i++) {
            final String part = apiOperation.getPathString().part(i);
            if (!apiOperation.getPathString().isParam(i)) {
                continue;
            }

            final String paramName = apiOperation.getPathString().paramName(i);
            final String paramValue = requestPath.part(i);

            final Optional<Parameter> parameter = apiOperation.getOperation().getParameters()
                    .stream()
                    .filter(p ->
                            p.getIn().equalsIgnoreCase("PATH") && p.getName().equalsIgnoreCase(paramName))
                    .findFirst();

            if (parameter.isPresent()) {
                validationReport = validationReport.merge(ParameterValidators.validate(paramValue, parameter.get()));
            }
        }
        return validationReport;
    }
}
