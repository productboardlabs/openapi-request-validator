package com.atlassian.oai.validator.schema.keyword;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.NodeType;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.core.keyword.syntax.checkers.AbstractSyntaxChecker;
import com.github.fge.jsonschema.core.processing.Processor;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.jsonschema.keyword.digest.AbstractDigester;
import com.github.fge.jsonschema.keyword.validator.AbstractKeywordValidator;
import com.github.fge.jsonschema.library.Keyword;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;

import java.util.Collection;

/**
 * Support for the {@code nullable} keyword introduced in OpenAPI v3
 *
 * @see <a href="https://swagger.io/specification/#schemaNullable">OpenAPI specification</a>
 */
public class Nullable {

    public static final String KEYWORD = "nullable";

    private static final Keyword INSTANCE = Keyword.newBuilder(KEYWORD)
            .withSyntaxChecker(NullableSyntaxChecker.getInstance())
            .withDigester(NullableDigester.getInstance())
            .withValidatorClass(NullableKeywordValidator.class)
            .freeze();

    public static Keyword getInstance() {
        return INSTANCE;
    }

    public static class NullableDigester extends AbstractDigester {

        private static final NullableDigester INSTANCE = new NullableDigester();

        public static NullableDigester getInstance() {
            return INSTANCE;
        }

        private NullableDigester() {
            super(KEYWORD, NodeType.OBJECT, NodeType.values());
        }

        @Override
        public JsonNode digest(final JsonNode schema) {
            setupNullableTypes(schema);
            setupNullableEnums(schema);
            return schema;
        }

        private static void setupNullableTypes(final JsonNode schemaObject) {
            // If the node is marked as nullable, and the type for this node is not
            // already "null", then we need to turn the "type" field into a list of
            // the currently specified type and "null", so that it will be properly
            // handled by the JSON-schema validation routine.
            final String typeKey = "type";
            final String nullableKey = "nullable";
            final String nullType = NodeType.NULL.toString();
            schemaObject
                    .findParents(typeKey)
                    .stream()
                    .filter(jsonNode -> jsonNode.path(nullableKey).asBoolean(false))
                    .filter(jsonNode -> !alreadySupportsNullType(jsonNode))
                    .forEach(jsonNode -> {
                        final JsonNode type = jsonNode.get(typeKey);

                        if (type.isTextual()) {
                            // If we are here, it means the type is a value
                            // like "string". So we need to transform it into
                            // an array type like [ "null", "string" ].
                            ((ObjectNode) jsonNode).putArray(typeKey).add(nullType).add(type);
                        } else if (type.isArray()) {
                            // If we are here, it means the type is already an
                            // array of types, like [ "integer", "string" ]. We
                            // just need to append the null type on the end.
                            ((ArrayNode) type).add(nullType);
                        }
                    });
        }

        private static void setupNullableEnums(final JsonNode schemaObject) {
            // If the node is marked as nullable, and this node is an enum, then we
            // need to extend the set of enumerated values to include null, so that
            // it will be properly handled by the JSON-schema validation routine.
            final String enumKey = "enum";
            final String nullableKey = "nullable";
            schemaObject
                    .findParents(enumKey)
                    .stream()
                    .filter(jsonNode -> jsonNode.path(nullableKey).asBoolean(false))
                    .forEach(jsonNode -> ((ArrayNode) jsonNode.get(enumKey)).addNull());
        }

        private static boolean alreadySupportsNullType(final JsonNode schemaObject) {
            final String nullType = NodeType.NULL.toString();
            final String typeKey = "type";
            final JsonNode typeNode = schemaObject.get(typeKey);

            if (typeNode.isTextual()) {
                return nullType.equals(typeNode.asText());
            } else if (typeNode.isArray()) {
                final ArrayNode typeNodeArr = (ArrayNode) typeNode;

                for (final JsonNode typeElem : typeNodeArr) {
                    if (nullType.equals(typeElem.asText())) {
                        return true;
                    }
                }
            }

            return false;
        }
    }

    public static class NullableKeywordValidator extends AbstractKeywordValidator {

        public NullableKeywordValidator(final JsonNode digest) {
            super(KEYWORD);
        }

        @Override
        public void validate(final Processor<FullData, FullData> processor,
                             final ProcessingReport report,
                             final MessageBundle bundle,
                             final FullData data) {
        }

        @Override
        public String toString() {
            return keyword;
        }
    }

    public static class NullableSyntaxChecker extends AbstractSyntaxChecker {

        private static final NullableSyntaxChecker INSTANCE = new NullableSyntaxChecker();

        static NullableSyntaxChecker getInstance() {
            return INSTANCE;
        }

        NullableSyntaxChecker() {
            super(KEYWORD, NodeType.BOOLEAN);
        }

        @Override
        protected void checkValue(final Collection<JsonPointer> pointers,
                                  final MessageBundle bundle,
                                  final ProcessingReport report,
                                  final SchemaTree tree) {
        }
    }

    private Nullable() {
    }
}