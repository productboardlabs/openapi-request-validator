package com.atlassian.oai.validator.interaction.request;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.report.ValidationReport;

import javax.annotation.Nonnull;

/**
 * User-defined validation for a request.
 */
@FunctionalInterface
public interface CustomRequestValidator {

    /**
     * Validates a request against a given api operation.
     *
     * @param request The request to validate
     * @param apiOperation The operation to validate the request against
     *
     * @return A validation report containing validation errors
     */
    ValidationReport validate(@Nonnull Request request, @Nonnull ApiOperation apiOperation);
}
