package com.atlassian.oai.validator.schema;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ListReportProvider;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.swagger.models.Model;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import io.swagger.util.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.atlassian.oai.validator.util.StringUtils.capitalise;
import static com.atlassian.oai.validator.util.StringUtils.quote;
import static com.atlassian.oai.validator.util.StringUtils.requireNonEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Validate a value against the schema defined in a Swagger/OpenAPI specification.
 * <p>
 * Supports validation of properties and request/response bodies, and supports schema references.
 */
public class SchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(SchemaValidator.class);

    public static final String ADDITIONAL_PROPERTIES_KEY = "validation.schema.additionalProperties";
    public static final String INVALID_JSON_KEY = "validation.schema.invalidJson";
    public static final String UNKNOWN_ERROR_KEY = "validation.schema.unknownError";

    private static final String ADDITIONAL_PROPERTIES_FIELD = "additionalProperties";
    private static final String DEFINITIONS_FIELD = "definitions";
    private static final String ALLOF_FIELD = "allOf";

    private final Swagger api;
    private JsonNode definitions;
    private boolean definitionsContainAllOf;
    private final MessageResolver messages;

    /**
     * Build a new validator with no API specification.
     * <p>
     * This will not perform any validation of $ref references that reference local definitions.
     *
     * @param messages The message resolver to use
     */
    public SchemaValidator(@Nonnull final MessageResolver messages) {
        this(null, messages);
    }

    /**
     * Build a new validator for the given API specification.
     *
     * @param api The API to build the validator for. If provided, is used to retrieve schema definitions
     *            for use in references.
     * @param messages The message resolver to use.
     */
    public SchemaValidator(@Nullable final Swagger api, @Nonnull final MessageResolver messages) {
        this.api = api;
        this.messages = requireNonNull(messages, "A message resolver is required");
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
        requireNonEmpty(value, "A value is required");
        requireNonNull(schema, "A schema is required");

        final MutableValidationReport validationReport = new MutableValidationReport();

        try {
            final JsonNode schemaObject = readSchema(schema);
            final JsonNode content = readContent(value, schema);

            checkForKnownGotchasAndLogMessage(schemaObject);

            final ListProcessingReport processingReport =
                    (ListProcessingReport) getJsonSchemaFactory()
                            .getJsonSchema(schemaObject)
                            .validate(content, true);

            if((processingReport != null) && !processingReport.isSuccess()) {
                processingReport.forEach(pm -> addProcessingMessage(validationReport, pm, null));
            }
            return validationReport;

        } catch (final JsonParseException e) {
            validationReport.add(messages.get(INVALID_JSON_KEY, e.getMessage()));
            return validationReport;
        } catch (final ProcessingException e) {
            addProcessingMessage(validationReport, e.getProcessingMessage(), "processingError");
            return validationReport;
        } catch (final Exception e) {
            validationReport.add(messages.get(UNKNOWN_ERROR_KEY, e.getMessage()));
            return validationReport;
        }
    }

    private JsonSchemaFactory getJsonSchemaFactory() {
        return JsonSchemaFactory
                .newBuilder()
                .setReportProvider(
                    // Only emit ERROR and above from the JSON schema validation
                    new ListReportProvider(LogLevel.ERROR, LogLevel.FATAL))
                .freeze();
    }

    private JsonNode readSchema(@Nonnull final Object schema) throws IOException {
        final JsonNode schemaObject = Json.mapper().readTree(Json.pretty(schema));
        setupSchemaDefinitionRefs(schemaObject);
        return schemaObject;
    }

    private void setupSchemaDefinitionRefs(final JsonNode schemaObject) throws IOException {
        if (schemaObject instanceof ObjectNode && additionalPropertiesValidationEnabled()) {
            ((ObjectNode)schemaObject).set(ADDITIONAL_PROPERTIES_FIELD, BooleanNode.getFalse());
        }

        if (api != null) {
            if (this.definitions == null) {
                this.definitions = Json.mapper().readTree(Json.pretty(api.getDefinitions()));

                // Explicitly disable additionalProperties
                // Calling code can choose what level to emit this failure at using validation.schema.additionalProperties
                if (additionalPropertiesValidationEnabled()) {
                    this.definitions.forEach(n -> {
                        if (!n.has(ADDITIONAL_PROPERTIES_FIELD)) {
                            ((ObjectNode)n).set(ADDITIONAL_PROPERTIES_FIELD, BooleanNode.getFalse());
                        }
                        if (n.has(ALLOF_FIELD)) {
                            this.definitionsContainAllOf = true;
                        }
                    });
                }
            }
            ((ObjectNode)schemaObject).set(DEFINITIONS_FIELD, this.definitions);
        }
    }

    private JsonNode readContent(@Nonnull final String value, @Nonnull final Object schema) throws IOException {
        String normalisedValue = value;
        if (schema instanceof StringProperty) {
            normalisedValue = quote(value);
        }
        return cleanupNullValues(Json.mapper().readTree(normalisedValue));
    }

    /**
     * Cleans up null values (except arrays) from the given <code>JsonNode</code> and its sub-nodes.
     */
    private JsonNode cleanupNullValues(final JsonNode node) {
        final JsonNode result = node.deepCopy();

        final Deque<JsonNode> toClean = new ArrayDeque<>();
        toClean.add(result);

        while(!toClean.isEmpty()) {
            final JsonNode n = toClean.pop();
            if (!n.isObject()) {
                continue;
            }
            final Iterator<Map.Entry<String, JsonNode>> fields = n.fields();
            while (fields.hasNext()) {
                final Map.Entry<String, JsonNode> field = fields.next();
                if (field.getValue().isNull()) {
                    fields.remove();
                } else if (field.getValue().isObject()){
                    toClean.add(field.getValue());
                }
            }
        }

        return result;
    }

    private boolean additionalPropertiesValidationEnabled() {
        return !messages.isIgnored(ADDITIONAL_PROPERTIES_KEY);
    }

    private void checkForKnownGotchasAndLogMessage(final JsonNode schemaObject) {
        if (additionalPropertiesValidationEnabled() && (schemaObject.has(ALLOF_FIELD) || definitionsContainAllOf)) {
            log.info("Note: Schema uses the 'allOf' keyword. " +
                    "Validation of 'additionalProperties' may fail with unexpected errors. " +
                    "See the project README FAQ for more information.");
        }
    }

    private void addProcessingMessage(final MutableValidationReport validationReport,
                                      final ProcessingMessage pm,
                                      final String keywordOverride) {
        final JsonNode processingMessage = pm.asJson();
        final String validationKeyword = keywordOverride != null ? keywordOverride : processingMessage.get("keyword").textValue();
        final String pointer = processingMessage.has("instance") ? processingMessage.get("instance").get("pointer").textValue() : "";

        final List<String> subReports = new ArrayList<>();
        if (processingMessage.has("reports")) {
            final JsonNode reports = processingMessage.get("reports");
            reports.fields().forEachRemaining(field -> {
               field.getValue().elements().forEachRemaining(report -> {
                   subReports.add(field.getKey() + ": " + capitalise(report.get("message").textValue()));
               });
            });
        }

        final String message =
                (pointer.isEmpty() ? "" : "[Path '" + pointer + "'] ")
                + capitalise(pm.getMessage());

        validationReport.add(messages.create("validation.schema." + validationKeyword, message, subReports.toArray(new String[0])));
    }


}
