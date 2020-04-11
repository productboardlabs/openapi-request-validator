package com.atlassian.oai.validator.schema.transform;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Transformer that adjusts the "required" fields list for nodes to remove fields marked as `readOnly` or `writeOnly`
 * for request / response validation as appropriate.
 */
public class RequiredFieldTransformer extends SchemaTransformer {

    private static final RequiredFieldTransformer INSTANCE = new RequiredFieldTransformer();

    public static RequiredFieldTransformer getInstance() {
        return INSTANCE;
    }

    @Override
    public void apply(final JsonNode schemaObject, final SchemaTransformationContext context) {
        if (schemaObject == null) {
            return;
        }

        if (!(context.isRequest() || context.isResponse())) {
            return;
        }

        if (hasRequiredFields(schemaObject)) {
            final List<String> adjustedRequired = getRequiredFieldNames(schemaObject)
                    .stream()
                    .filter(fieldName ->
                            isNotReadOnlyInRequest(context, schemaObject, fieldName) ||
                                    isNotWriteOnlyInResponse(context, schemaObject, fieldName)
                    )
                    .collect(toList());

            setRequiredFieldNames(schemaObject, adjustedRequired);
        }

        applyToChildSchemas(schemaObject, child -> apply(child, context));
    }

    private boolean isNotWriteOnlyInResponse(final SchemaTransformationContext context,
                                             final JsonNode schemaObject,
                                             final String fieldName) {
        return context.isResponse() && !isWriteOnly(property(schemaObject, fieldName));
    }

    private boolean isNotReadOnlyInRequest(final SchemaTransformationContext context,
                                           final JsonNode schemaObject,
                                           final String fieldName) {
        return context.isRequest() && !isReadOnly(property(schemaObject, fieldName));
    }

}
