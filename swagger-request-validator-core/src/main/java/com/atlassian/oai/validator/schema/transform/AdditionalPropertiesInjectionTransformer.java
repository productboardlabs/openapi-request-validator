package com.atlassian.oai.validator.schema.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class AdditionalPropertiesInjectionTransformer extends SchemaTransformer {

    private static final AdditionalPropertiesInjectionTransformer INSTANCE = new AdditionalPropertiesInjectionTransformer();

    public static AdditionalPropertiesInjectionTransformer getInstance() {
        return INSTANCE;
    }

    @Override
    public void apply(final JsonNode schemaObject, final SchemaTransformationContext context) {
        if (schemaObject == null || !context.isAdditionalPropertiesValidationEnabled()) {
            return;
        }

        if (!hasAdditionalFieldSet(schemaObject) && !hasDiscriminatorField(schemaObject)) {
            disableAdditionalProperties((ObjectNode) schemaObject);
        }

        applyToChildSchemas(schemaObject, child -> apply(child, context));
    }
}
