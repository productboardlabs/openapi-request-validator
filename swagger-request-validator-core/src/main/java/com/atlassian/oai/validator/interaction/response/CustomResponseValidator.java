package com.atlassian.oai.validator.interaction.response;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;

import javax.annotation.Nonnull;

/**
 * User-defined validation for a response.
 */
@FunctionalInterface
public interface CustomResponseValidator {

    /**
     * Validates a response against a given api operation.
     *
     * @param response The response to validate
     * @param apiOperation The operation to validate the request against
     *
     * @return A validation report containing validation errors
     */
    ValidationReport validate(@Nonnull Response response, @Nonnull ApiOperation apiOperation);
}
