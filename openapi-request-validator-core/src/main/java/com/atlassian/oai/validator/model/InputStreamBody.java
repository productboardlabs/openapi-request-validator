package com.atlassian.oai.validator.model;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.core.util.Json;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;

public class InputStreamBody implements Body {
    private final PushbackInputStream content;

    public InputStreamBody(@Nonnull final InputStream content) {
        this.content = new PushbackInputStream(content);
    }

    @Override
    public boolean hasBody() {
        try {
            final int firstSign = content.read();
            content.unread(firstSign);
            return firstSign != -1;
        } catch (final IOException e) {
            // Can't read from stream. It is assumed the request has no body.
            return false;
        }
    }

    @Override
    public JsonNode toJsonNode() throws IOException {
        return Json.mapper().readTree(content);
    }

    @Override
    public String toString(final Charset encoding) throws IOException {
        return IOUtils.toString(content, encoding);
    }
}
