package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Factory for OpenAPI / Swagger assertions.
 */
public class OpenApiMatchers {

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
     */
    public ResultMatcher isValid(final String specUrlOrPayload) {
        final OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createFor(specUrlOrPayload)
                .build();

        return isValid(validator);
    }

    /**
     * Assert the result can be validated using the given pre-configured validator.
     *
     * @param validator The pre-configured validator to use
     */
    public ResultMatcher isValid(final OpenApiInteractionValidator validator) {
        return result -> {
            final MockHttpServletRequest request = result.getRequest();
            final MockHttpServletResponse response = result.getResponse();
            final ValidationReport validationReport = validator.validate(MockMvcRequest.of(request), MockMvcResponse.of(response));
            if (validationReport.hasErrors()) {
                throw new OpenApiValidationException(validationReport);
            }
        };
    }

    public static class OpenApiValidationException extends RuntimeException {

        private final ValidationReport report;

        public OpenApiValidationException(final ValidationReport report) {
            super(JsonValidationReportFormat.getInstance().apply(report));
            this.report = report;
        }

        public ValidationReport getValidationReport() {
            return report;
        }
    }

}
