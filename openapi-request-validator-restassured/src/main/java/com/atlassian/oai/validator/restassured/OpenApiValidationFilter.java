package com.atlassian.oai.validator.restassured;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

import java.util.Objects;

import static com.atlassian.oai.validator.util.StringUtils.requireNonEmpty;

/**
 * A {@link Filter} that performs OpenAPI / Swagger validation on a request/response interaction.
 * <p>
 * To use, simply add it as a filter to your rest-assured given-when-then interaction:
 * <pre>
 *     private final OpenApiValidationFilter validationFilter = new OpenApiValidationFilter(SPEC_URL);
 *     ...
 *     given()
 *          .filter(validationFilter)
 *     .when()
 *          .get("/my/path")
 *     .then()
 *          .assertThat()
 *          .statusCode(200);
 * </pre>
 * <p>
 * If validation fails, a {@link OpenApiValidationException} will be thrown describing the validation failure.
 */
public class OpenApiValidationFilter implements Filter {

    private final OpenApiInteractionValidator validator;

    public OpenApiValidationFilter(final String specUrlOrDefinition) {
        requireNonEmpty(specUrlOrDefinition, "A spec is required");

        validator = OpenApiInteractionValidator.createFor(specUrlOrDefinition).build();
    }

    public OpenApiValidationFilter(final OpenApiInteractionValidator validator) {
        Objects.requireNonNull(validator, "A validator is required");

        this.validator = validator;
    }

    @Override
    public Response filter(final FilterableRequestSpecification requestSpec,
                           final FilterableResponseSpecification responseSpec,
                           final FilterContext ctx) {

        final Response response = ctx.next(requestSpec, responseSpec);

        final ValidationReport validationReport =
                validator.validate(RestAssuredRequest.of(requestSpec), RestAssuredResponse.of(response));

        if (validationReport.hasErrors()) {
            throw new OpenApiValidationException(validationReport);
        }

        return response;
    }

    public static class OpenApiValidationException extends RuntimeException {
        private final ValidationReport report;

        public OpenApiValidationException(final ValidationReport report) {
            super(JsonValidationReportFormat.getInstance().apply(report));
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
