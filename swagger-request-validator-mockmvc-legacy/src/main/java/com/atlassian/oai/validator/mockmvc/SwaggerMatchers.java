package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.report.SimpleValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Factory for OpenAPI / Swagger assertions.
 *
 * @deprecated Replaced with {@link OpenApiMatchers}
 */
@Deprecated
public class SwaggerMatchers {

    private final OpenApiMatchers matchers = new OpenApiMatchers();

    /**
     * Assert the result can be validated against the given Swagger v2 or OpenAPI v3 specification.
     * <p>
     * The specification can be provided as a URL or an inline spec in JSON or YAML format.
     * <p>
     * A URL can be an absolute HTTP/HTTPS URL, a File URL or a classpath location (without the classpath: scheme).
     * <p>
     * For example:
     * <pre>
     *     // Create from a publicly hosted HTTP location
     *     .isValid("http://api.myservice.com/swagger.json")
     *
     *     // Create from a file on the local filesystem
     *     .isValid("file://Users/myuser/tmp/api.yaml");
     *
     *     // Create from a classpath resource in the /api package
     *     .isValid("/api/swagger.json");
     *
     *     // Create from a swagger JSON payload
     *     .isValid("{\"swagger\": \"2.0\", ...}")
     * </pre>
     *
     * @param specUrlOrPayload The location of the Swagger JSON specification to use in the validator.
     *
     * @deprecated Use {@link OpenApiMatchers#isValid(String)} instead
     */
    public ResultMatcher isValid(final String specUrlOrPayload) {
        try {
            return matchers.isValid(specUrlOrPayload);
        } catch (final OpenApiMatchers.OpenApiValidationException e) {
            throw new SwaggerValidationException(e.getValidationReport());
        }
    }

    @Deprecated
    public static class SwaggerValidationException extends RuntimeException {

        private final ValidationReport report;

        public SwaggerValidationException(final ValidationReport report) {
            super(SimpleValidationReportFormat.getInstance().apply(report));
            this.report = report;
        }

        public ValidationReport getValidationReport() {
            return report;
        }
    }
}
