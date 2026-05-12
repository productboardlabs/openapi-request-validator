package com.atlassian.oai.validator.schema.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Transformer that injects `additionalProperties: false` into the nodes in the schema tree if additional properties validation is enabled.
 * <p>
 * Won't affect any node that already has `additionalProperties` set.
 * <p>
 * <strong>OAS 3.1 closed-schema interaction:</strong> when a schema declares
 * {@code unevaluatedProperties} (a JSON Schema 2020-12 keyword used by OAS 3.1
 * to express closed-schema semantics across {@code allOf}/{@code anyOf}/{@code oneOf}
 * compositions), this transformer skips injection on that subtree's composition
 * branches. Otherwise the injected {@code additionalProperties: false} on each
 * branch would reject any property declared in a sibling branch, causing valid
 * payloads to fail before {@code unevaluatedProperties} ever runs.
 */
public class AdditionalPropertiesInjectionTransformer extends SchemaTransformer {

    private static final String UNEVALUATED_PROPERTIES_FIELD = "unevaluatedProperties";

    private static final AdditionalPropertiesInjectionTransformer INSTANCE = new AdditionalPropertiesInjectionTransformer();

    public static AdditionalPropertiesInjectionTransformer getInstance() {
        return INSTANCE;
    }

    @Override
    public void apply(final JsonNode schemaObject, final SchemaTransformationContext context) {
        if (schemaObject == null || !context.isAdditionalPropertiesValidationEnabled()) {
            return;
        }

        if (!hasAdditionalFieldSet(schemaObject) && !hasDiscriminatorField(schemaObject) && hasPropertiesField(schemaObject)) {
            disableAdditionalProperties((ObjectNode) schemaObject);
        }

        // 3.1 closed-schema guard: when this node uses unevaluatedProperties,
        // its composition branches will be evaluated as part of the
        // unevaluated-analysis pass. Injecting additionalProperties:false into
        // each branch would generate spurious failures because each branch only
        // declares its own properties, not its siblings'. Recursing into nested
        // properties is still safe — they are independent subtrees.
        if (hasUnevaluatedPropertiesField(schemaObject)) {
            properties(schemaObject).forEachRemaining(child -> apply(child, context));
            return;
        }

        applyToChildSchemas(schemaObject, child -> apply(child, context));
    }

    private static boolean hasUnevaluatedPropertiesField(final JsonNode n) {
        return n != null && n.has(UNEVALUATED_PROPERTIES_FIELD);
    }
}
