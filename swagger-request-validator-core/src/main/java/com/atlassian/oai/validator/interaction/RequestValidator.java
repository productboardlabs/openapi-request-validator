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

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Validate a request against a given API operation.
 */
public class RequestValidator {

    private final SchemaValidator schemaValidator;

    /**
     * Construct a new request validator with the given schema validator.
     *
     * @param schemaValidator The schema validator to use when validating request bodies
     */
    public RequestValidator(@Nonnull final SchemaValidator schemaValidator) {
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
    @Nonnull
    public ValidationReport validateRequest(@Nonnull final NormalisedPath requestPath,
                                            @Nonnull final Request request,
                                            @Nonnull final ApiOperation apiOperation) {
        requireNonNull(requestPath, "A request path is required");
        requireNonNull(request, "A request is required");
        requireNonNull(apiOperation, "An API operation is required");

        return validatePathParameters(requestPath, apiOperation)
                .merge(validateRequestBody(request.getBody(), apiOperation))
                .merge(validateQueryParameters(request, apiOperation));
    }

    @Nonnull
    private ValidationReport validateRequestBody(@Nonnull final Optional<String> requestBody,
                                                 @Nonnull final ApiOperation apiOperation) {
        final Optional<Parameter> bodyParameter = apiOperation.getOperation().getParameters()
                .stream().filter(p -> p.getIn().equalsIgnoreCase("body")).findFirst();

        final MutableValidationReport validationReport = new MutableValidationReport();
        if (requestBody.isPresent() && !bodyParameter.isPresent()) {
            return validationReport.addError(format("No request body is expected for %s on path '%s'",
                    apiOperation.getMethod(), apiOperation.getPathString().original()));
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

    @Nonnull
    private ValidationReport validatePathParameters(@Nonnull final NormalisedPath requestPath,
                                                    @Nonnull final ApiOperation apiOperation) {

        ValidationReport validationReport = ValidationReport.empty();
        for (int i = 0; i < apiOperation.getPathString().parts().size(); i++) {
            if (!apiOperation.getPathString().isParam(i)) {
                continue;
            }

            final String paramName = apiOperation.getPathString().paramName(i);
            final String paramValue = requestPath.part(i);

            final Optional<Parameter> parameter = apiOperation.getOperation().getParameters()
                    .stream()
                    .filter(p -> p.getIn().equalsIgnoreCase("PATH"))
                    .filter(p -> p.getName().equalsIgnoreCase(paramName))
                    .findFirst();

            if (parameter.isPresent()) {
                validationReport = validationReport.merge(ParameterValidators.validate(paramValue, parameter.get()));
            }
        }
        return validationReport;
    }

    private ValidationReport validateQueryParameters(@Nonnull final Request request,
                                                     @Nonnull final ApiOperation apiOperation) {
        final MutableValidationReport validationReport = new MutableValidationReport();
        apiOperation.getOperation()
                .getParameters()
                .stream()
                .filter(p -> p.getIn().equalsIgnoreCase("QUERY"))
                .forEach(p -> {
                    final Collection<String> queryParameterValues = request.getQueryParameterValues(p.getName());
                    if (queryParameterValues.isEmpty() && p.getRequired()) {
                        validationReport.addError(
                                format("Query parameter '%s' is required on path '%s' but not found in request.",
                                        p.getName(), apiOperation.getPathString().original()));
                        return;
                    }
                    queryParameterValues.stream().forEach( v -> {
                            validationReport.addAll(ParameterValidators.validate(v, p));
                    });
                });

        return validationReport;
    }
}
