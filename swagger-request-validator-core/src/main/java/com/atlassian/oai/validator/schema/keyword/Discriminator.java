package com.atlassian.oai.validator.schema.keyword;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.NodeType;
import com.github.fge.jsonschema.keyword.digest.AbstractDigester;
import com.github.fge.jsonschema.library.Keyword;

public class Discriminator {

    public static final String KEYWORD = "discriminator";

    static final String PROPERTYNAME_KEYWORD = "propertyName";
    static final String MAPPING_KEYWORD = "mapping";

    private static final Keyword INSTANCE = Keyword.newBuilder(KEYWORD)
            .withSyntaxChecker(DiscriminatorSyntaxChecker.getInstance())
            .withDigester(DiscriminatorDigester.getInstance())
            .withValidatorClass(DiscriminatorKeywordValidator.class)
            .freeze();

    public static Keyword getInstance() {
        return INSTANCE;
    }

    /**
     * Digester for the <code>discriminator</code> keyword introduced by the OpenAPI / Swagger specification.
     */
    public static class DiscriminatorDigester extends AbstractDigester {

        private static final DiscriminatorDigester INSTANCE = new DiscriminatorDigester();

        public static DiscriminatorDigester getInstance() {
            return INSTANCE;
        }

        private DiscriminatorDigester() {
            super(KEYWORD, NodeType.OBJECT);
        }

        @Override
        public JsonNode digest(final JsonNode schema) {
            final ObjectNode ret = FACTORY.objectNode();
            ret.put(keyword, schema.get(keyword));
            return ret;
        }
    }

    private Discriminator() {
    }
}
