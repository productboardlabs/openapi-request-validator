package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;

import javax.annotation.Nonnull;

import static java.util.Objects.requireNonNull;

/**
 * Validate a response against an API operation
 */
public class ResponseValidator {

    private final SchemaValidator schemaValidator;
    private final MessageResolver messages = new MessageResolver();

    /**
     * Construct a new response validator with the given schema validator.
     *
     * @param schemaValidator The schema validator to use when validating response bodies
     */
    public ResponseValidator(@Nonnull final SchemaValidator schemaValidator) {
        this.schemaValidator = requireNonNull(schemaValidator, "A schema validator is required");
    }

    /**
     * Validate the given response against the API operation.
     *
     * @param response The response to validate
     * @param apiOperation The API operation to validate the response against
     *
     * @return A validation report containing validation errors
     */
    @Nonnull
    public ValidationReport validateResponse(@Nonnull final Response response, @Nonnull final ApiOperation apiOperation) {
        requireNonNull(response, "A response is required");
        requireNonNull(apiOperation, "An API operation is required");

        io.swagger.models.Response apiResponse = apiOperation.getOperation().getResponses().get(Integer.toString(response.getStatus()));
        if (apiResponse == null) {
            apiResponse = apiOperation.getOperation().getResponses().get("default"); // try the default response
        }

        final MutableValidationReport validationReport = new MutableValidationReport();
        if (apiResponse == null) {
            return validationReport.add(
                    messages.get("validation.response.status.unknown",
                            response.getStatus(), apiOperation.getPathString().original())
            );
        }

        if (apiResponse.getSchema() == null) {
            return validationReport;
        }

        if (!response.getBody().isPresent()) {
            return validationReport.add(
                    messages.get("validation.response.body.missing",
                            apiOperation.getMethod(), apiOperation.getPathString().original())
            );
        }

        return validationReport.merge(schemaValidator.validate(response.getBody().get(), apiResponse.getSchema()));
    }
}
