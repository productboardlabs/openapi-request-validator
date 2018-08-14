package com.atlassian.oai.validator.restassured;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.report.SimpleValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * @deprecated Replaced with {@link OpenApiValidationFilter}
 */
@Deprecated
public class SwaggerValidationFilter implements Filter {

    private final OpenApiValidationFilter delegate;

    public SwaggerValidationFilter(final String swaggerJsonUrl) {
        delegate = new OpenApiValidationFilter(swaggerJsonUrl);
    }

    public SwaggerValidationFilter(final SwaggerRequestResponseValidator validator) {
        delegate = new OpenApiValidationFilter(validator.getValidator());
    }

    @Override
    public Response filter(final FilterableRequestSpecification requestSpec,
                           final FilterableResponseSpecification responseSpec,
                           final FilterContext ctx) {
        try {
            return delegate.filter(requestSpec, responseSpec, ctx);
        } catch (final OpenApiValidationFilter.OpenApiValidationException e) {
            throw new SwaggerValidationException(e.getValidationReport());
        }
    }

    /**
     * @deprecated Replaced with {@link OpenApiValidationFilter.OpenApiValidationException}
     */
    @Deprecated
    static class SwaggerValidationException extends RuntimeException {
        private final ValidationReport report;

        public SwaggerValidationException(final ValidationReport report) {
            super(SimpleValidationReportFormat.getInstance().apply(report));
            this.report = report;
        }

        /**
         * @return The validation report that generating this exception
         */
        public ValidationReport getValidationReport() {
            return report;
        }
    }
}
