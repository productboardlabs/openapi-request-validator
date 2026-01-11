package com.atlassian.oai.validator.schema.format;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.networknt.schema.ExecutionContext;
import com.networknt.schema.Format;
import com.networknt.schema.JsonType;
import com.networknt.schema.TypeFactory;
import com.networknt.schema.ValidationContext;

public class FloatFormat implements Format {
    @Override
    public String getName() {
        return "float";
    }

    @Override
    public String getMessageKey() {
        return "format.float";
    }

    @Override
    public boolean matches(final ExecutionContext executionContext, final ValidationContext validationContext, final JsonNode value) {
        final JsonType nodeType = TypeFactory.getValueNodeType(value, validationContext.getConfig());

        if (nodeType != JsonType.NUMBER) {
            return true;
        }

        final NumericNode numericValue = (NumericNode) value;

        if (numericValue.isNaN()) {
            return false;
        }

        final float f = numericValue.floatValue();
        final String original = String.valueOf(numericValue.decimalValue());
        final String parsed = String.valueOf(f);

        return original.equals(parsed);
    }
}
