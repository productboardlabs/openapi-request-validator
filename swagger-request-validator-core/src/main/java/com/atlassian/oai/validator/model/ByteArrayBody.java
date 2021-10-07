package com.atlassian.oai.validator.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.Charset;

public class ByteArrayBody implements Body {
    private final byte[] content;

    public ByteArrayBody(@Nonnull final byte[] content) {
        this.content = content;
    }

    @Override
    public boolean hasBody() {
        return content.length > 0;
    }

    @Override
    public JsonNode toJsonNode() throws IOException {
        return Json.mapper().readTree(content);
    }

    @Override
    public String toString(final Charset encoding) {
        return new String(content, encoding);
    }
}
