package com.atlassian.oai.validator.schema.transform;

import com.fasterxml.jackson.databind.JsonNode;

import static com.atlassian.oai.validator.schema.SwaggerV20Library.OAI_V2_METASCHEMA_URI;

public class SchemaRefInjectionTransformer extends SchemaTransformer {

    private static final SchemaRefInjectionTransformer INSTANCE = new SchemaRefInjectionTransformer();

    public static SchemaRefInjectionTransformer getInstance() {
        return INSTANCE;
    }

    @Override
    public void apply(final JsonNode schemaObject, final SchemaTransformationContext context) {
        setSchemaRef(schemaObject, OAI_V2_METASCHEMA_URI);
    }
}
