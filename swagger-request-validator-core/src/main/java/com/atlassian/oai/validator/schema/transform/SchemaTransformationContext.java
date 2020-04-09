package com.atlassian.oai.validator.schema.transform;

public class SchemaTransformationContext {

    private final boolean isRequest;
    private final boolean isResponse;

    private SchemaTransformationContext(final boolean isRequest, final boolean isResponse) {
        this.isRequest = isRequest;
        this.isResponse = isResponse;
    }

    public boolean isRequest() {
        return isRequest;
    }

    public boolean isResponse() {
        return isResponse;
    }

    public static Builder create() {
        return new Builder();
    }

    public static final class Builder {
        private boolean isRequest;
        private boolean isResponse;

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

        public SchemaTransformationContext build() {
            return new SchemaTransformationContext(isRequest, isResponse);
        }
    }
}
