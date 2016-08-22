package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.parameter.ParameterValidators;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.ResourceBundle;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Validate a request against a given API operation.
 */
public class RequestValidator {

    private final SchemaValidator schemaValidator;
    private final ParameterValidators parameterValidators;

    private final ResourceBundle messages;

    /**
     * Construct a new request validator with the given schema validator.
     *
     * @param schemaValidator The schema validator to use when validating request bodies
     */
    public RequestValidator(@Nonnull final SchemaValidator schemaValidator) {
        this.schemaValidator = requireNonNull(schemaValidator, "A schema validator is required");
        this.parameterValidators = new ParameterValidators(schemaValidator);
        this.messages = ResourceBundle.getBundle("messages");
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

        if (requestBody.isPresent() && !requestBody.get().isEmpty() && !bodyParameter.isPresent()) {
            return ValidationReport.singleton(
                    format(messages.getString("validation.request.body.unexpected"),
                    apiOperation.getMethod(), apiOperation.getPathString().original())
            );
        }

        if (!bodyParameter.isPresent()) {
            return ValidationReport.empty();
        }

        if (!requestBody.isPresent() || requestBody.get().isEmpty()) {
            if (bodyParameter.get().getRequired()) {
                return ValidationReport.singleton(
                        format(messages.getString("validation.request.body.missing"),
                        apiOperation.getMethod(), apiOperation.getPathString().original())
                );
            }
            return ValidationReport.empty();
        }

        return schemaValidator.validate(requestBody.get(), ((BodyParameter)bodyParameter.get()).getSchema());
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
                validationReport = validationReport.merge(parameterValidators.validate(paramValue, parameter.get()));
            }
        }
        return validationReport;
    }

    private ValidationReport validateQueryParameters(@Nonnull final Request request,
                                                     @Nonnull final ApiOperation apiOperation) {
        return apiOperation
                .getOperation()
                .getParameters()
                .stream()
                .filter(p -> p.getIn().equalsIgnoreCase("QUERY"))
                .map(p -> validateQueryParameter(request, apiOperation, p))
                .reduce(ValidationReport.empty(), ValidationReport::merge);
    }

    private ValidationReport validateQueryParameter(@Nonnull final Request request,
                                                    @Nonnull final ApiOperation apiOperation,
                                                    @Nonnull final Parameter queryParameter) {

        final Collection<String> queryParameterValues = request.getQueryParameterValues(queryParameter.getName());

        if (queryParameterValues.isEmpty() && queryParameter.getRequired()) {
            return ValidationReport.singleton(
                    format(messages.getString("validation.request.parameter.query.missing"),
                            queryParameter.getName(), apiOperation.getPathString().original())
            );
        }

        return queryParameterValues
                .stream()
                .map((v) -> parameterValidators.validate(v, queryParameter))
                .reduce(ValidationReport.empty(), ValidationReport::merge);
    }
}
