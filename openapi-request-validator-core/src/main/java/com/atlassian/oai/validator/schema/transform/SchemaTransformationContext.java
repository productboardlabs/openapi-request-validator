package com.atlassian.oai.validator.schema.transform;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;

public class SchemaTransformationContext {

    private final boolean isRequest;
    private final boolean isResponse;
    private final boolean additionalPropertiesValidationEnabled;
    private final JsonNode schemaDefinitions;
    private final boolean isOpenApi30;

    private SchemaTransformationContext(final boolean isRequest,
                                        final boolean isResponse,
                                        final boolean additionalPropertiesValidationEnabled,
                                        final JsonNode schemaDefinitions,
                                        final boolean isOpenApi30) {
        this.isRequest = isRequest;
        this.isResponse = isResponse;
        this.additionalPropertiesValidationEnabled = additionalPropertiesValidationEnabled;
        this.schemaDefinitions = schemaDefinitions;
        this.isOpenApi30 = isOpenApi30;
    }

    public boolean isRequest() {
        return isRequest;
    }

    public boolean isResponse() {
        return isResponse;
    }

    public boolean isAdditionalPropertiesValidationEnabled() {
        return additionalPropertiesValidationEnabled;
    }

    public JsonNode getSchemaDefinitions() {
        return schemaDefinitions;
    }

    public boolean isOpenApi30() {
        return isOpenApi30;
    }

    public static Builder create() {
        return new Builder();
    }

    public static final class Builder {
        private boolean isRequest;
        private boolean isResponse;
        private boolean additionalPropertiesValidationEnabled;
        private JsonNode definitions;
        private boolean isOpenApi30;

        private Builder() {
        }

        public Builder forRequest(final boolean isRequest) {
            this.isRequest = isRequest;
            return this;
        }

        public Builder forResponse(final boolean isResponse) {
            this.isResponse = isResponse;
            return this;
        }

        public Builder withAdditionalPropertiesValidation(final boolean enabled) {
            this.additionalPropertiesValidationEnabled = enabled;
            return this;
        }

        public Builder withDefinitions(final JsonNode definitions) {
            this.definitions = definitions;
            if (this.definitions == null) {
                this.definitions = Json.mapper().createObjectNode();
            }
            return this;
        }

        public Builder withIsOpenApi30(final boolean isOpenApi30) {
            this.isOpenApi30 = isOpenApi30;
            return this;
        }

        public SchemaTransformationContext build() {
            return new SchemaTransformationContext(isRequest, isResponse, additionalPropertiesValidationEnabled, definitions, isOpenApi30);
        }
    }
}
