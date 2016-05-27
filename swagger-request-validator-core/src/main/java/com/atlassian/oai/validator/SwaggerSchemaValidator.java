package com.atlassian.oai.validator;

import com.atlassian.oai.validator.report.MutableValidationReport;
import com.atlassian.oai.validator.report.ValidationReport;
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

    public ValidationReport validate(final String value, final Property schema) {
        return doValidate(value, schema);
    }

    public ValidationReport validate(final String value, final Model schema) {
        return doValidate(value, schema);
    }

    private ValidationReport doValidate(final String value, final Object schema) {
        ListProcessingReport processingReport = null;
        try {
            if (this.definitions == null) {
                this.definitions = Json.mapper().readTree(Json.pretty(api.getDefinitions()));
            }

            final JsonNode schemaObject = Json.mapper().readTree(Json.pretty(schema));
            ((ObjectNode)schemaObject).set("definitions", this.definitions);

            final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
            final com.github.fge.jsonschema.main.JsonSchema jsonSchema = factory.getJsonSchema(schemaObject);
            final JsonNode content = Json.mapper().readTree(value);
            processingReport = (ListProcessingReport)jsonSchema.validate(content);
        }
        catch (Exception e) {
            // TODO
            e.printStackTrace();
        }

        final MutableValidationReport validationReport = new MutableValidationReport();
        if((processingReport != null) && !processingReport.isSuccess()) {
            validationReport.addError(format("Value does not match schema:\nValue:\n\t%s\n\nValidation report:\n\t%s",
                    value, Json.pretty(processingReport.asJson()).replace("\n", "\n\t")));
        }
        return validationReport;
    }
}
