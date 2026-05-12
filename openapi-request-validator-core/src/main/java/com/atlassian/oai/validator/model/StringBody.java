package com.atlassian.oai.validator.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.Charset;

public class StringBody implements Body {
    private final String content;
    private final Charset charset;

    public StringBody(@Nonnull final String content, @Nonnull final Charset charset) {
        this.content = content;
        this.charset = charset;
    }

    @Override
    public boolean hasBody() {
        return content.length() > 0;
    }

    @Override
    public JsonNode toJsonNode() throws IOException {
        return Json.mapper().readTree(content);
    }

    @Override
    public String toString(@Nonnull final Charset encoding) {
        if (charset == encoding) {
            return content; // no conversion necessary
        }
        return new String(content.getBytes(charset), encoding);
    }
}
