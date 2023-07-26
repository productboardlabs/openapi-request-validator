package com.atlassian.oai.validator.schema;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.transform.AdditionalPropertiesInjectionTransformer;
import com.atlassian.oai.validator.schema.transform.RequiredFieldTransformer;
import com.atlassian.oai.validator.schema.transform.SchemaDefinitionsInjectionTransformer;
import com.atlassian.oai.validator.schema.transform.SchemaRefInjectionTransformer;
import com.atlassian.oai.validator.schema.transform.SchemaTransformationContext;
import com.atlassian.oai.validator.schema.transform.SchemaTransformer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.StreamSupport.stream;

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

    private static final String ALLOF_FIELD = "allOf";

    private final MessageResolver messages;
    private final LoadingCache<JsonSchemaKey, JsonSchema> jsonSchemaCache;
    private final ProcessingMessageConverter messageConverter;

    private final ValidationConfiguration validationConfiguration;

    private final JsonNode definitions;

    private final JsonSchemaFactory schemaFactory;

    /**
     * Transformations applied to the schema before validation.
     * <p>
     * Order is important here - the mutations from one transformation are passed through to the subsequent transformers.
     */
    private final List<SchemaTransformer> transformers = asList(
            SchemaDefinitionsInjectionTransformer.getInstance(),
            SchemaRefInjectionTransformer.getInstance(),
            AdditionalPropertiesInjectionTransformer.getInstance(),
            RequiredFieldTransformer.getInstance()
    );

    /**
     * Build a new validator for the given API specification.
     *
     * @param api The API to build the validator for.
     * @param messages The message resolver to use.
     */
    public SchemaValidator(final OpenAPI api,
                           @Nonnull final MessageResolver messages) {
        this(api, messages, SwaggerV20Library::schemaFactory, new ValidationConfiguration());
    }

    /**
     * Build a new validator for the given API specification.
     *
     * @param api The API to build the validator for.
     * @param messages The message resolver to use.
     * @param schemaFactorySupplier A supplier function to get JsonSchemaFactory.
     */
    public SchemaValidator(final OpenAPI api,
        @Nonnull final MessageResolver messages,
        @Nonnull final Supplier<JsonSchemaFactory> schemaFactorySupplier) {
        this(api, messages, schemaFactorySupplier, new ValidationConfiguration());
    }

    /**
     * Build a new validator for the given API specification.
     *
     * @param api The API to build the validator for.
     * @param messages The message resolver to use.
     * @param schemaFactorySupplier A supplier function to get JsonSchemaFactory.
     */
    public SchemaValidator(final OpenAPI api,
                           @Nonnull final MessageResolver messages,
                           @Nonnull final Supplier<JsonSchemaFactory> schemaFactorySupplier,
                           @Nonnull final ValidationConfiguration validationConfiguration) {
        this.messages = requireNonNull(messages, "A message resolver is required");
        this.validationConfiguration = validationConfiguration;

        definitions = Optional.ofNullable(api.getComponents())
                .map(Components::getSchemas)
                .map(schemas -> Json.mapper().convertValue(schemas, JsonNode.class))
                .orElseGet(() -> Json.mapper().createObjectNode());
        schemaFactory = requireNonNull(schemaFactorySupplier.get(), "A JsonSchemaFactory is required");
        if (validationConfiguration.isCacheEnabled()) {
            this.jsonSchemaCache = CacheBuilder.newBuilder()
                .maximumSize(validationConfiguration.getMaxCacheSize())
                .build(new CacheLoader<JsonSchemaKey, JsonSchema>() {
                    @Override
                    public JsonSchema load(final JsonSchemaKey key) throws ProcessingException {
                        final JsonNode schemaObject = readAndTransformSchemaObject(key.schema,
                            key.forRequest, key.forResponse, definitions);
                        return schemaFactory.getJsonSchema(schemaObject);
                    }
                });
        } else {
            this.jsonSchemaCache = null;
        }
        this.messageConverter = new ProcessingMessageConverter(messages);
    }

    /**
     * Validate the given value against the given property schema. If the schema is null then any json is valid.
     *
     * @param value The value to validate
     * @param schema The schema to validate the value against
     * @param keyPrefix A prefix to apply to validation messages emitted by the validator
     *
     * @return A validation report containing accumulated validation errors
     */
    @Nonnull
    public ValidationReport validate(@Nonnull final String value,
                                     @Nullable final Schema schema,
                                     @Nullable final String keyPrefix) {
        requireNonNull(value, "A value is required");
        return validate(() -> readContent(value, schema), schema, keyPrefix);
    }

    /**
     * Validate the given value against the given property schema. If the schema is null then any json is valid.
     *
     * @param supplier Supplies the JsonNode to validate
     * @param schema The schema to validate the value against
     * @param keyPrefix A prefix to apply to validation messages emitted by the validator
     *
     * @return A validation report containing accumulated validation errors
     */
    @Nonnull
    public ValidationReport validate(@Nonnull final JsonNodeSupplier supplier,
                                     @Nullable final Schema schema,
                                     @Nullable final String keyPrefix) {
        if (schema == null) {
            return ValidationReport.empty();
        }

        try {
            final ListProcessingReport processingReport;
            try {
                final JsonNode content = supplier.get();

                final JsonSchema jsonSchema = resolveJsonSchema(schema, keyPrefix);
                processingReport = (ListProcessingReport) jsonSchema
                        .validate(content, true);

            } catch (final ProcessingException e) {
                return messageConverter.toValidationReport(e.getProcessingMessage(), "processingError", keyPrefix);
            } catch (final IOException e) {
                return ValidationReport.singleton(
                        messages.create(
                                "validation." + keyPrefix + ".schema.invalidJson",
                                messages.get(INVALID_JSON_KEY, e.getMessage()).getMessage()
                        )
                );
            }

            if (processingReport != null && !processingReport.isSuccess()) {
                return stream(processingReport.spliterator(), false)
                        .map(pm -> messageConverter.toValidationReport(pm, null, keyPrefix))
                        .reduce(ValidationReport.empty(), ValidationReport::merge);
            }
            return ValidationReport.empty();
        } catch (final RuntimeException e) {
            log.debug("Error during schema validation", e);
            return ValidationReport.singleton(
                    messages.create(
                            "validation." + keyPrefix + ".schema.unknownError",
                            messages.get(UNKNOWN_ERROR_KEY, e.getMessage()).getMessage()
                    )
            );
        }
    }

    private JsonSchema resolveJsonSchema(final Schema schema, @Nullable final String keyPrefix)
            throws ProcessingException {
        final boolean forRequest = "request.body".equalsIgnoreCase(keyPrefix);
        final boolean forResponse = "response.body".equalsIgnoreCase(keyPrefix);
        final JsonSchemaKey jsonSchemaKey = new JsonSchemaKey(schema, forRequest, forResponse);
        try {
            if (validationConfiguration.isCacheEnabled()) {
                return jsonSchemaCache.get(jsonSchemaKey);
            }
            final JsonNode schemaObject = readAndTransformSchemaObject(schema, forRequest, forResponse, definitions);
            return schemaFactory.getJsonSchema(schemaObject);
        } catch (final ExecutionException e) {
            final List<Throwable> causalChain = Throwables.getCausalChain(e);
            throw (ProcessingException) causalChain.stream()
                    .filter(exception -> exception instanceof ProcessingException)
                    .findFirst()
                    .orElseGet(() -> new ProcessingException("JsonSchema creation failed.", Iterables.getLast(causalChain)));
        }
    }

    private JsonNode readAndTransformSchemaObject(final Schema schema, final boolean forRequest,
                                                  final boolean forResponse, final JsonNode definitions) {
        final ObjectNode schemaObject = Json.mapper().convertValue(schema, ObjectNode.class);
        final SchemaTransformationContext transformationContext = SchemaTransformationContext.create()
                .forRequest(forRequest)
                .forResponse(forResponse)
                .withAdditionalPropertiesValidation(additionalPropertiesValidationEnabled())
                // Use a copy of the definitions. The JsonSchema validation process might change them
                // in its validation process. On concurrent validations it might even lead to
                // ConcurrentModificationException.
                .withDefinitions(definitions.deepCopy())
                .build();

        transformers.forEach(t -> t.apply(schemaObject, transformationContext));

        checkForKnownGotchasAndLogMessage(schemaObject);
        return schemaObject;
    }

    private static JsonNode readContent(@Nonnull final String value, @Nonnull final Schema schema) throws IOException {
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

    private static JsonNode createStringNode(final String value) {
        return new TextNode(value);
    }

    private static JsonNode createNumericNode(final String value) throws IOException {
        try {
            Double.parseDouble(value);
            // Valid number. Leave unquoted.
            return Json.mapper().readTree(value);
        } catch (final NumberFormatException e) {
            // Invalid number. Schema validator will generate appropriate errors.
            return createStringNode(value);
        }
    }

    private static String normaliseDateTime(final String dateTime) {
        // Re-format DateTime since Schema validator doesn't accept some valid RFC3339 date-times and throws:
        // ERROR - String "1996-12-19T16:39:57-08:00" is invalid against requested date format(s)
        // [yyyy-MM-dd'T'HH:mm:ssZ, yyyy-MM-dd'T'HH:mm:ss.[0-9]{1,12}Z]: []
        try {
            final LocalDateTime rfc3339dt = LocalDateTime.parse(dateTime, CustomDateTimeFormatter.getRFC3339Formatter());
            return rfc3339dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        } catch (final DateTimeParseException e) {
            // Could not parse to RFC3339 format. Schema validator will throw the appropriate error
            return dateTime;
        }
    }

    private boolean additionalPropertiesValidationEnabled() {
        return !messages.isIgnored(ADDITIONAL_PROPERTIES_KEY);
    }

    private void checkForKnownGotchasAndLogMessage(final JsonNode schemaObject) {
        if (additionalPropertiesValidationEnabled() && (schemaObject.has(ALLOF_FIELD))) {
            log.info("Note: Schema uses the 'allOf' keyword. " +
                    "Validation of 'additionalProperties' may fail with unexpected errors. " +
                    "See the project README FAQ for more information.");
        }
    }

    @FunctionalInterface
    public interface JsonNodeSupplier {
        JsonNode get() throws IOException;
    }

    private static class JsonSchemaKey {
        private final Schema schema;
        private final boolean forRequest;
        private final boolean forResponse;

        private JsonSchemaKey(final Schema schema, final boolean forRequest, final boolean forResponse) {
            this.schema = schema;
            this.forRequest = forRequest;
            this.forResponse = forResponse;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final JsonSchemaKey that = (JsonSchemaKey) o;
            return forRequest == that.forRequest && forResponse == that.forResponse
                    && Objects.equals(schema, that.schema);
        }

        @Override
        public int hashCode() {
            return Objects.hash(forRequest, forResponse, schema);
        }
    }
}
