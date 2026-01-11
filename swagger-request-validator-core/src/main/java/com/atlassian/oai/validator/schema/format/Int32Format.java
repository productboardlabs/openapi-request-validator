package com.atlassian.oai.validator.schema.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.Format;
import com.networknt.schema.JsonType;
import com.networknt.schema.TypeFactory;
import com.networknt.schema.ValidationContext;

public class Int32Format implements Format {
    @Override
    public String getName() {
        return "int32";
    }

    @Override
    public String getMessageKey() {
        return "format.int32";
    }

    @Override
    public boolean matches(final ExecutionContext executionContext, final ValidationContext validationContext, final JsonNode value) {
        final JsonType nodeType = TypeFactory.getValueNodeType(value, validationContext.getConfig());
        return nodeType != JsonType.INTEGER || value.canConvertToInt();
    }
}
