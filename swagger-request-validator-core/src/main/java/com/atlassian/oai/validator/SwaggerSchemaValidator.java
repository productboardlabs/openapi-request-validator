package com.atlassian.oai.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.util.Json;

import static java.lang.String.format;

public class SwaggerSchemaValidator {
    private final Swagger api;
    private JsonNode definitions;

    public SwaggerSchemaValidator(Swagger api) {
        this.api = api;
    }

    public void validate(final String value, final Property schema) {
        doValidate(value, schema);
    }

    public void validate(final String value, final Model schema) {
        doValidate(value, schema);
    }

    private void doValidate(final String value, final Object schema) {
        ListProcessingReport report = null;
        try {
            if (this.definitions == null) {
                this.definitions = Json.mapper().readTree(Json.pretty(api.getDefinitions()));
            }

            final JsonNode schemaObject = Json.mapper().readTree(Json.pretty(schema));
            ((ObjectNode)schemaObject).set("definitions", this.definitions);

            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            final com.github.fge.jsonschema.main.JsonSchema jsonSchema = factory.getJsonSchema(schemaObject);
            final JsonNode content = Json.mapper().readTree(value);
            report = (ListProcessingReport)jsonSchema.validate(content);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if((report != null) && !report.isSuccess()) {
            throw new SchemaValidationException(
                    format("Value does not match schema:\nValue: \n%s\n\nValidation report:\n%s",
                            value, Json.pretty(report.asJson())));
        }
    }

    static class SchemaValidationException extends RuntimeException {
        public SchemaValidationException(String message) {
            super(message);
        }
    }
}
