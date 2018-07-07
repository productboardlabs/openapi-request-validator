package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReportFormatter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Factory for Swagger assertions.
 */
public class SwaggerMatchers {

    /**
     * Assert the result can be validated against the Swagger JSON specification at the given location OR actual swagger JSON String payload.
     * <p>
     * The URL can be an absolute HTTP/HTTPS URL, a File URL or a classpath location (without the classpath: scheme).
     * <p>
     * For example:
     * <pre>
     *     // Create from a publicly hosted HTTP location
     *     .createFor("http://api.myservice.com/swagger.json")
     *
     *     // Create from a file on the local filesystem
     *     .createFor("file://Users/myuser/tmp/swagger.json");
     *
     *     // Create from a classpath resource in the /api package
     *     .createFor("/api/swagger.json");
     *
     *     // Create from a swagger JSON payload
     *     .createFor("{\"swagger\": \"2.0\", ...}")
     * </pre>
     *
     * @param swaggerJsonUrlOrPayload The location of the Swagger JSON specification to use in the validator.
     */
    public ResultMatcher isValid(final String swaggerJsonUrlOrPayload) {
        final SwaggerRequestResponseValidator validator = SwaggerRequestResponseValidator
                .createFor(swaggerJsonUrlOrPayload)
                .build();

        return result -> {
            final MockHttpServletRequest request = result.getRequest();
            final MockHttpServletResponse response = result.getResponse();
            final ValidationReport validationReport = validator.validate(MockMvcRequest.of(request), MockMvcResponse.of(response));
            if (validationReport.hasErrors()) {
                throw new SwaggerValidationException(validationReport);
            }
        };
    }

    public static class SwaggerValidationException extends RuntimeException {

        private final ValidationReport report;

        public SwaggerValidationException(final ValidationReport report) {
            super(ValidationReportFormatter.format(report));
            this.report = report;
        }

        public ValidationReport getValidationReport() {
            return report;
        }
    }

}
