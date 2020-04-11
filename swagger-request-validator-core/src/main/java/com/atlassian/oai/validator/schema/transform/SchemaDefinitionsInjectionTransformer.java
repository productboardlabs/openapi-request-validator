package com.atlassian.oai.validator.schema.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Simple transformer than injects schema definitions into the `#/components/schemas` path so that references resolve correctly.
 */
public class SchemaDefinitionsInjectionTransformer extends SchemaTransformer {

    private static final SchemaDefinitionsInjectionTransformer INSTANCE = new SchemaDefinitionsInjectionTransformer();

    public static SchemaDefinitionsInjectionTransformer getInstance() {
        return INSTANCE;
    }

    @Override
    public void apply(final JsonNode schemaObject, final SchemaTransformationContext context) {
        if (!(schemaObject instanceof ObjectNode)) {
            return;
        }

        ((ObjectNode) schemaObject).putObject(COMPONENTS_FIELD).set(SCHEMAS_FIELD, context.getSchemaDefinitions().deepCopy());
    }
}
