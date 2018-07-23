package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;

import javax.annotation.Nonnull;

/**
 * Validates a HTTP request/response pair with an OpenAPI / Swagger specification.
 * <p>
 * Validation errors are provided in a @{@link ValidationReport} that can be used to inspect the failures.
 * <p>
 * New instances should be created via the {@link OpenApiInteractionValidator#createFor(String)} method.
 *
 * @see #createFor(String)
 *
 * @deprecated Replaced with {@link OpenApiInteractionValidator}. This class will be removed in a future release.
 */
@Deprecated
public class SwaggerRequestResponseValidator {

    private OpenApiInteractionValidator validator;

    public static OpenApiInteractionValidator.Builder createFor(@Nonnull final String specUrlOrPayload) {
        return OpenApiInteractionValidator.createFor(specUrlOrPayload);
    }

    @Nonnull
    public ValidationReport validate(@Nonnull final Request request, @Nonnull final Response response) {
        return validator.validate(request, response);
    }

    @Nonnull
    public ValidationReport validateRequest(@Nonnull final Request request) {
        return validator.validateRequest(request);
    }

    @Nonnull
    public ValidationReport validateResponse(@Nonnull final String path, @Nonnull final Request.Method method, @Nonnull final Response response) {
        return validator.validateResponse(path, method, response);
    }

    public OpenApiInteractionValidator getValidator() {
        return validator;
    }
}
