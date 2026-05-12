package com.atlassian.oai.validator.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.nio.charset.Charset;

public interface Body {
    /**
     * @return {@code true} if the bodies content contains at least one sign, otherwise {@code false}
     */
    boolean hasBody();

    /**
     * @return the bodies content as {@link JsonNode}
     * @throws IOException in case the transformation fails
     */
    JsonNode toJsonNode() throws IOException;

    /**
     * @param encoding the {@link Charset} the bodies content shall be converted to
     *
     * @return the bodies content as {@link String} converted with the specified encoding
     * @throws IOException in case the conversion fails
     */
    String toString(final Charset encoding) throws IOException;
}
