package com.atlassian.oai.validator.schema.transform;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;

public class SchemaTransformationContext {

    private final boolean isRequest;
    private final boolean isResponse;
    private final boolean additionalPropertiesValidationEnabled;
    private final JsonNode schemaDefinitions;

    private SchemaTransformationContext(final boolean isRequest,
                                        final boolean isResponse,
                                        final boolean additionalPropertiesValidationEnabled,
                                        final JsonNode schemaDefinitions) {
        this.isRequest = isRequest;
        this.isResponse = isResponse;
        this.additionalPropertiesValidationEnabled = additionalPropertiesValidationEnabled;
        this.schemaDefinitions = schemaDefinitions;
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

    public static Builder create() {
        return new Builder();
    }

    public static final class Builder {
        private boolean isRequest;
        private boolean isResponse;
        private boolean additionalPropertiesValidationEnabled;
        private JsonNode definitions;

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

        public SchemaTransformationContext build() {
            return new SchemaTransformationContext(isRequest, isResponse, additionalPropertiesValidationEnabled, definitions);
        }
    }
}
