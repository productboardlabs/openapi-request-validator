package com.atlassian.oai.validator.schema;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import io.swagger.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.StreamSupport;

import static com.atlassian.oai.validator.schema.SwaggerV20Library.OAI_V2_METASCHEMA_URI;
import static com.atlassian.oai.validator.schema.SwaggerV20Library.schemaFactory;
import static com.atlassian.oai.validator.util.StringUtils.capitalise;
import static com.atlassian.oai.validator.util.StringUtils.requireNonEmpty;
import static java.util.Objects.requireNonNull;

/**
 * Validate a value against the schema defined in an OpenAPI / Swagger specification.
 * <p>
 * Supports validation of properties and request/response bodies, and supports schema references.
 */
public class SchemaValidator {

    private static final Logger log = LoggerFactory.getLogger(SchemaValidator.class);

    public static final String ADDITIONAL_PROPERTIES_KEY = "validation.schema.additionalProperties";
    public static final String INVALID_JSON_KEY = "validation.schema.invalidJson";
    public static final String UNKNOWN_ERROR_KEY = "validation.schema.unknownError";

    private static final String ADDITIONAL_PROPERTIES_FIELD = "additionalProperties";
    private static final String DISCRIMINATOR_FIELD = "discriminator";
    private static final String PROPERTIES_FIELD = "properties";
    private static final String TYPE_FIELD = "type";
    private static final String COMPONENTS_FIELD = "components";
    private static final String SCHEMAS_FIELD = "schemas";
    private static final String ALLOF_FIELD = "allOf";
    private static final String SCHEMA_REF_FIELD = "$schema";

    private final OpenAPI api;
    private JsonNode definitions;
    private boolean definitionsContainAllOf;
    private final MessageResolver messages;

    /**
     * Build a new validator for the given API specification.
     *
     * @param api      The API to build the validator for.
     * @param messages The message resolver to use.
     */
    public SchemaValidator(final OpenAPI api, @Nonnull final MessageResolver messages) {
        this.api = api;
        this.messages = requireNonNull(messages, "A message resolver is required");
    }

    /**
     * Validate the given value against the given property schema. If the schema is null then any json is valid.
     *
     * @param value The value to validate
     * @param schema The schema to validate the value against
     * @param keyPrefix A prefix to apply to validation messages emitted by the validator
     * @return A validation report containing accumulated validation errors
     */
    @Nonnull
    public ValidationReport validate(@Nonnull final String value,
                                     @Nullable final Schema schema,
                                     @Nullable final String keyPrefix) {
        requireNonEmpty(value, "A value is required");

        if (schema == null) {
            return ValidationReport.empty();
        }

        try {
            final JsonNode schemaObject, content;
            try {
                schemaObject = readSchema(schema);
                final List<String> required = getRequired(schema, keyPrefix);
                if (required != null && !required.isEmpty()) {
                    setRequired((ObjectNode) schemaObject, required);
                }

                content = readContent(value, schema);

                checkForKnownGotchasAndLogMessage(schemaObject);
            } catch (final JsonParseException e) {
                return ValidationReport.singleton(
                        messages.create(
                                "validation." + keyPrefix + ".schema.invalidJson",
                                messages.get(INVALID_JSON_KEY, e.getMessage()).getMessage()
                        )
                );
            }

            final ListProcessingReport processingReport;
            try {
                processingReport = (ListProcessingReport) schemaFactory().getJsonSchema(schemaObject)
                        .validate(content, true);
            } catch (final ProcessingException e) {
                return getProcessingMessage(e.getProcessingMessage(), "processingError", keyPrefix);
            }

            if ((processingReport != null) && !processingReport.isSuccess()) {
                return StreamSupport.stream(processingReport.spliterator(), false)
                        .map(pm -> getProcessingMessage(pm, null, keyPrefix))
                        .reduce(ValidationReport.empty(), ValidationReport::merge);
            }
            return ValidationReport.empty();
        } catch (final Exception e) {
            log.debug("Error during schema validation", e);
            return ValidationReport.singleton(
                    messages.create(
                            "validation." + keyPrefix + ".schema.unknownError",
                            messages.get(UNKNOWN_ERROR_KEY, e.getMessage()).getMessage()
                    )
            );
        }
    }

    private void setRequired(final ObjectNode schemaObject, final List<String> required) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(out, required);
        schemaObject.set("required", Json.mapper().readTree(out.toString()));
    }

    private List<String> getRequired(final Schema schema, @Nullable final String keyPrefix) {
        if (keyPrefix == null || schema.getRequired() == null) {
            return null;
        }

        final Map<String, Schema> properties = schema.getProperties();
        final List<String> required = schema.getRequired();
        final List<String> requiredUpdated = new ArrayList<>(required);
        required.forEach(property -> {
            removeReadOnlyRequiredPropertyForRequestBody(keyPrefix, properties, requiredUpdated, property);
            removeWriteOnlyRequiredPropertyForResponeBody(keyPrefix, properties, requiredUpdated, property);
        });
        return requiredUpdated;
    }

    private void removeWriteOnlyRequiredPropertyForResponeBody(final String keyPrefix,
                                                               final Map<String, Schema> properties,
                                                               final List<String> requiredUpdated,
                                                               final String property) {
        if (keyPrefix.equals("response.body")
                && properties.get(property).getWriteOnly() != null
                && properties.get(property).getWriteOnly()) {
            requiredUpdated.remove(property);
        }
    }

    private void removeReadOnlyRequiredPropertyForRequestBody(final String keyPrefix,
                                                              final Map<String, Schema> properties,
                                                              final List<String> requiredUpdated,
                                                              final String property) {
        if (keyPrefix.equals("request.body")
                && properties.get(property).getReadOnly() != null
                && properties.get(property).getReadOnly()) {
            requiredUpdated.remove(property);
        }
    }

    private JsonNode readSchema(@Nonnull final Object schema) throws IOException {
        final JsonNode schemaObject = Json.mapper().readTree(Json.pretty(schema));
        setupSchemaDefinitionRefs(schemaObject);
        return schemaObject;
    }

    private void setupSchemaDefinitionRefs(final JsonNode schemaObject) throws IOException {
        final ObjectNode rootNode = (ObjectNode) schemaObject;

        rootNode.put(SCHEMA_REF_FIELD, OAI_V2_METASCHEMA_URI);
        if (additionalPropertiesValidationEnabled()) {
            injectAdditionalPropertiesDirectiveIntoTree(rootNode);
        }
        if (api != null) {
            if (definitions == null) {
                definitions = (api.getComponents() == null || api.getComponents().getSchemas() == null) ?
                        Json.mapper().createObjectNode() :
                        Json.mapper().readTree(Json.pretty(api.getComponents().getSchemas()));
                definitions.forEach(n -> {
                    if (additionalPropertiesValidationEnabled()) {
                        // Explicitly disable additionalProperties
                        // Calling code can choose what level to emit this failure at using
                        // validation.schema.additionalProperties
                        definitionsContainAllOf = injectAdditionalPropertiesDirectiveIntoTree(n);
                    }
                });
            }
            rootNode.putObject(COMPONENTS_FIELD).set(SCHEMAS_FIELD, definitions);
        }
    }

    private boolean injectAdditionalPropertiesDirectiveIntoTree(@Nonnull final JsonNode n) {
        if (!hasAdditionalFieldSet(n) && !hasDiscriminatorField(n)) {
            disableAdditionalProperties((ObjectNode) n);
        }
        boolean hasAllOfInChildDefinition = false;
        final Iterator<JsonNode> properties = properties(n);
        while (properties.hasNext()) {
            final JsonNode prop = properties.next();
            if (isObjectDefinition(prop)) {
                hasAllOfInChildDefinition = injectAdditionalPropertiesDirectiveIntoTree(prop) || hasAllOfInChildDefinition;
            } else if (isArrayDefinition(prop)) {
                final JsonNode items = itemsDefinition(prop);
                if (isObjectDefinition(items)) {
                    hasAllOfInChildDefinition = injectAdditionalPropertiesDirectiveIntoTree(items) || hasAllOfInChildDefinition;
                }
            }
        }
        return hasAllOfField(n) || hasAllOfInChildDefinition;
    }

    private static boolean hasAllOfField(final JsonNode n) {
        return n.has(ALLOF_FIELD);
    }

    @Nullable
    private static JsonNode itemsDefinition(final JsonNode n) {
        return n.get("items");
    }

    private static boolean isObjectDefinition(@Nullable final JsonNode n) {
        if (n == null) {
            return false;
        }
        final JsonNode type = n.get(TYPE_FIELD);
        return type != null && type.textValue().equalsIgnoreCase("object");
    }

    private static boolean isArrayDefinition(@Nullable final JsonNode n) {
        if (n == null) {
            return false;
        }
        final JsonNode type = n.get(TYPE_FIELD);
        return type != null && type.textValue().equalsIgnoreCase("array");
    }

    private static void disableAdditionalProperties(final ObjectNode n) {
        n.set(ADDITIONAL_PROPERTIES_FIELD, BooleanNode.getFalse());
    }

    private static Iterator<JsonNode> properties(final JsonNode n) {
        if (n.has(PROPERTIES_FIELD)) {
            return n.get(PROPERTIES_FIELD).iterator();
        }
        return Collections.<JsonNode>emptyList().iterator();
    }

    private static boolean hasDiscriminatorField(final JsonNode n) {
        return n.has(DISCRIMINATOR_FIELD);
    }

    private static boolean hasAdditionalFieldSet(final JsonNode n) {
        return n.has(ADDITIONAL_PROPERTIES_FIELD);
    }

    private JsonNode readContent(@Nonnull final String value, @Nonnull final Schema schema) throws IOException {
        if ("null".equalsIgnoreCase(value)) {
            return Json.mapper().readTree("null");
        }

        if (schema instanceof DateTimeSchema) {
            return createStringNode(normaliseDateTime(value));
        }
        if ("string".equalsIgnoreCase(schema.getType())) {
            return createStringNode(value);
        }
        if ("number".equalsIgnoreCase(schema.getType()) ||
                "integer".equalsIgnoreCase(schema.getType())) {
            return createNumericNode(value);
        }

        return Json.mapper().readTree(value);
    }

    private JsonNode createStringNode(final String value) {
        return new TextNode(value);
    }

    private JsonNode createNumericNode(final String value) throws IOException {
        try {
            Double.parseDouble(value);
            // Valid number. Leave unquoted.
            return Json.mapper().readTree(value);
        } catch (final NumberFormatException e) {
            // Invalid number. Schema validator will generate appropriate errors.
            return createStringNode(value);
        }
    }

    private String normaliseDateTime(final String dateTime) {
        // Re-format DateTime since Schema validator doesn't accept some valid RFC3339 date-times and throws:
        // ERROR - String "1996-12-19T16:39:57-08:00" is invalid against requested date format(s)
        // [yyyy-MM-dd'T'HH:mm:ssZ, yyyy-MM-dd'T'HH:mm:ss.[0-9]{1,12}Z]: []
        String formatedDateTime = dateTime;
        try {
            final LocalDateTime rfc3339dt = LocalDateTime.parse(dateTime, CustomDateTimeFormatter.getRFC3339Formatter());
            formatedDateTime = rfc3339dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
            //CHECKSTYLE:OFF EmptyCatchBlock
        } catch (final DateTimeParseException e) {
            // Could not parse to RFC3339 format. Schema validator will throw the appropriate error
        }
        //CHECKSTYLE:ON
        return formatedDateTime;
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

    private ValidationReport getProcessingMessage(final ProcessingMessage pm,
                                                  final String keywordOverride,
                                                  final String keyPrefix) {

        return ValidationReport.singleton(
                toValidationReportMessage(pm.asJson(), keywordOverride, keyPrefix));
    }

    private ValidationReport.Message toValidationReportMessage(final JsonNode processingMessage,
                                                               final String keywordOverride,
                                                               final String keyPrefix) {

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
                        + capitalise(processingMessage.get("message").textValue());

        final ValidationReport.Message validationReportMessage = messages.create(
                "validation." + keyPrefix + ".schema." + validationKeyword,
                message, subReports.toArray(new String[0]));

        return withNestedMessages(processingMessage, keywordOverride, keyPrefix, validationReportMessage);
    }

    private ValidationReport.Message withNestedMessages(final JsonNode processingMessage,
                                                        final String keywordOverride,
                                                        final String keyPrefix,
                                                        final ValidationReport.Message validationReportMessage) {
        if (!processingMessage.has("reports")) {
            return validationReportMessage;
        }

        // Recursively convert 'reports' node children to ValidationReport.Message and add as nested messages
        final List<ValidationReport.Message> nestedMessages = new ArrayList<>();
        final JsonNode reports = processingMessage.get("reports");
        reports.fields().forEachRemaining(field -> {
            field.getValue().elements().forEachRemaining(report -> {
                nestedMessages.add(toValidationReportMessage(report, keywordOverride, keyPrefix));
            });
        });

        return validationReportMessage.withNestedMessages(nestedMessages);
    }

}
