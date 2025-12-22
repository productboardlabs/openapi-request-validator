package com.atlassian.oai.validator.schema.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.annotation.Nonnull;

/**
 * A {@link SchemaTransformer} that normalizes {@code exclusiveMaximum} and {@code exclusiveMinimum} fields.
 * <p>
 * In older JSON Schema drafts (e.g., Draft 4, used in Swagger 2.0), {@code exclusiveMaximum} and
 * {@code exclusiveMinimum} were boolean flags that indicated whether the corresponding
 * {@code maximum} or {@code minimum} values were exclusive.
 * <p>
 * In newer drafts (e.g., Draft 5, OpenAPI 3.0), these fields are numeric values representing
 * the exclusive limit itself.
 * <p>
 * This transformer converts the legacy boolean usage into the modern numeric usage.
 */
public class ExclusiveMinMaxTransformer extends SchemaTransformer {

    private static final ExclusiveMinMaxTransformer INSTANCE = new ExclusiveMinMaxTransformer();

    private static final String EXCLUSIVE_MAXIMUM = "exclusiveMaximum";
    private static final String MAXIMUM = "maximum";
    private static final String EXCLUSIVE_MINIMUM = "exclusiveMinimum";
    private static final String MINIMUM = "minimum";

    /**
     * Gets the singleton instance of the transformer.
     *
     * @return The singleton instance.
     */
    public static ExclusiveMinMaxTransformer getInstance() {
        return INSTANCE;
    }

    /**
     * Transforms the given schema object by converting boolean exclusive constraints to numeric ones.
     * <p>
     * If {@code exclusiveMaximum} is a boolean:
     * <ul>
     * <li>If {@code true}, it is replaced with the value of the {@code maximum} field.</li>
     * <li>If {@code false}, the field is removed.</li>
     * </ul>
     * <p>
     * If {@code exclusiveMinimum} is a boolean:
     * <ul>
     * <li>If {@code true}, it is replaced with the value of the {@code minimum} field.</li>
     * </ul>
     *
     * @param schemaObject The JSON node representing the schema definition.
     * @param context      The transformation context.
     */
    @Override
    public void apply(@Nonnull final JsonNode schemaObject, @Nonnull final SchemaTransformationContext context) {
        if (!context.isOpenApi30()) {
            return;
        }
        if (!(schemaObject instanceof ObjectNode)) {
            return;
        }
        final ObjectNode objectNode = (ObjectNode) schemaObject;

        // Handle exclusiveMaximum
        if (objectNode.has(EXCLUSIVE_MAXIMUM) && objectNode.get(EXCLUSIVE_MAXIMUM).isBoolean()) {
            if (objectNode.get(EXCLUSIVE_MAXIMUM).asBoolean()) {
                final JsonNode maximum = objectNode.get(MAXIMUM);
                objectNode.set(EXCLUSIVE_MAXIMUM, maximum);
            } else {
                objectNode.remove(EXCLUSIVE_MAXIMUM);
            }
        }

        // Handle exclusiveMinimum
        if (objectNode.has(EXCLUSIVE_MINIMUM) && objectNode.get(EXCLUSIVE_MINIMUM).isBoolean()) {
            if (objectNode.get(EXCLUSIVE_MINIMUM).asBoolean()) {
                final JsonNode minimum = objectNode.get(MINIMUM);
                objectNode.set(EXCLUSIVE_MINIMUM, minimum);
            }
        } else {
            objectNode.remove(EXCLUSIVE_MINIMUM);
        }
    }
}