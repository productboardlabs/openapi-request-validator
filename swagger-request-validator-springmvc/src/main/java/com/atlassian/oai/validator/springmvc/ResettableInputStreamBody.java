package com.atlassian.oai.validator.springmvc;

import io.swagger.v3.core.util.Json;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.annotation.Nonnull;

import org.apache.commons.io.IOUtils;

import com.atlassian.oai.validator.model.Body;
import com.fasterxml.jackson.databind.JsonNode;

public class ResettableInputStreamBody implements Body {
    private final ResettableRequestServletWrapper.CachingServletInputStream resettableInputStream;

    public ResettableInputStreamBody(@Nonnull final ResettableRequestServletWrapper.CachingServletInputStream resettableInputStream) {
        this.resettableInputStream = resettableInputStream;
    }

    @Override
    public boolean hasBody() {
        try {
            final int firstSign = resettableInputStream.read();
            resettableInputStream.reset();
            return firstSign != -1;
        } catch (final IOException e) {
            // Can't read from stream. It is assumed the request has no body.
            return false;
        }
    }

    @Override
    public JsonNode toJsonNode() throws IOException {
        final JsonNode jsonNode = Json.mapper().readTree(resettableInputStream);
        // the stream has been read fully - reset it for the next usage
        resettableInputStream.reset();
        return jsonNode;
    }

    @Override
    public String toString(final Charset encoding) throws IOException {
        final String string = IOUtils.toString(resettableInputStream, encoding);
        // the stream has been read fully - reset it for the next usage
        resettableInputStream.reset();
        return string;
    }
}
