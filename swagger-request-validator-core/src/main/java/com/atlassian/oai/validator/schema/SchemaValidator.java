package com.atlassian.oai.validator.schema;

import com.atlassian.oai.validator.report.MutableValidationReport;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ListReportProvider;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.util.Json;

import javax.annotation.Nonnull;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Validate a value against the schema defined in a Swagger/OpenAPI specification.
 * <p>
 * Supports validation of properties and request/response bodies, and supports schema references.
 */
public class SchemaValidator {
    private final Swagger api;
    private JsonNode definitions;

    /**
     * Build a new validator for the given API specification.
     *
     * @param api The API to build the validator for. Used to retrieve schema definitions for use in references. (required)
     */
    public SchemaValidator(@Nonnull final Swagger api) {
        this.api = requireNonNull(api, "An API is required");
    }

    /**
     * Validate the given value against the given property schema.
     *
     * @param value The value to validate
     * @param schema The property schema to validate the value against
     *
     * @return A validation report containing accumulated validation errors
     */
    @Nonnull
    public ValidationReport validate(@Nonnull final String value, @Nonnull final Property schema) {
        return doValidate(value, schema);
    }

    /**
     * Validate the given value against the given model schema.
     *
     * @param value The value to validate
     * @param schema The model schema to validate the value against
     *
     * @return A validation report containing accumulated validation errors
     */
    @Nonnull
    public ValidationReport validate(@Nonnull final String value, @Nonnull final Model schema) {
        return doValidate(value, schema);
    }

    @Nonnull
    private ValidationReport doValidate(@Nonnull final String value, @Nonnull final Object schema) {
        requireNonNull(value, "A value is required");
        requireNonNull(schema, "A schema is required");

        final MutableValidationReport validationReport = new MutableValidationReport();
        ListProcessingReport processingReport = null;
        try {
            if (this.definitions == null) {
                this.definitions = Json.mapper().readTree(Json.pretty(api.getDefinitions()));
            }

            final JsonNode schemaObject = Json.mapper().readTree(Json.pretty(schema));
            ((ObjectNode)schemaObject).set("definitions", this.definitions);

            // Only emit ERROR and above from the JSON schema validation
            final JsonSchemaFactory factory = JsonSchemaFactory.newBuilder()
                    .setReportProvider(new ListReportProvider(LogLevel.ERROR, LogLevel.FATAL))
                    .freeze();

            final com.github.fge.jsonschema.main.JsonSchema jsonSchema = factory.getJsonSchema(schemaObject);
            final JsonNode content = Json.mapper().readTree(value);
            processingReport = (ListProcessingReport)jsonSchema.validate(content);
        }
        catch (JsonParseException e) {
            validationReport.addError("Unable to parse JSON - " + e.getMessage());
            return validationReport;
        }
        catch (Exception e) {
            e.printStackTrace();
        }


        if((processingReport != null) && !processingReport.isSuccess()) {
            validationReport.addError(format("Value does not match schema:\nValue:\n\t%s\n\nValidation report:\n\t%s",
                    value, Json.pretty(processingReport.asJson()).replace("\n", "\n\t")));
        }
        return validationReport;
    }
}
