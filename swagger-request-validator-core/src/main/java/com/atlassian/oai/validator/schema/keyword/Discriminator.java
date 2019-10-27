package com.atlassian.oai.validator.schema.keyword;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.NodeType;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.keyword.syntax.checkers.AbstractSyntaxChecker;
import com.github.fge.jsonschema.core.processing.Processor;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.jsonschema.keyword.digest.AbstractDigester;
import com.github.fge.jsonschema.keyword.validator.AbstractKeywordValidator;
import com.github.fge.jsonschema.library.Keyword;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Streams.stream;
import static java.util.stream.Collectors.toList;

public class Discriminator {

    public static final String KEYWORD = "discriminator";

    private static final String PROPERTYNAME_KEYWORD = "propertyName";
    private static final String MAPPING_KEYWORD = "mapping";

    private static final Keyword INSTANCE = Keyword.newBuilder(KEYWORD)
            .withSyntaxChecker(DiscriminatorSyntaxChecker.getInstance())
            .withDigester(DiscriminatorDigester.getInstance())
            .withValidatorClass(DiscriminatorKeywordValidator.class)
            .freeze();

    public static Keyword getInstance() {
        return INSTANCE;
    }

    /**
     * Syntax checker for the <code>discriminator</code> keyword introduced by the OpenAPI / Swagger specification.
     *
     * @see <a href="http://swagger.io/specification/#composition-and-inheritance--polymorphism--83">Swagger specification</a>
     */
    public static class DiscriminatorSyntaxChecker extends AbstractSyntaxChecker {

        private static final DiscriminatorSyntaxChecker INSTANCE = new DiscriminatorSyntaxChecker();

        static DiscriminatorSyntaxChecker getInstance() {
            return INSTANCE;
        }

        DiscriminatorSyntaxChecker() {
            super(KEYWORD, NodeType.OBJECT);
        }

        @Override
        protected void checkValue(final Collection<JsonPointer> pointers,
                                  final MessageBundle bundle,
                                  final ProcessingReport report,
                                  final SchemaTree tree) throws ProcessingException {

            final String discriminatorFieldName = getNode(tree).get(PROPERTYNAME_KEYWORD).textValue();
            if (discriminatorFieldName.isEmpty()) {
                report.error(msg(tree, bundle, "err.swaggerv2.discriminator.empty"));
                return;
            }

            final JsonNode properties = tree.getNode().get("properties");
            final List<String> propertyNames = stream(properties.fieldNames()).collect(toList());
            if (!properties.has(discriminatorFieldName)) {
                report.error(msg(tree, bundle, "err.swaggerv2.discriminator.noProperty")
                        .putArgument("fieldName", discriminatorFieldName)
                        .putArgument("properties", propertyNames)
                );
                return;
            }

            final JsonNode property = properties.get(discriminatorFieldName);
            if (!property.has("type") ||
                    !property.get("type").textValue().equalsIgnoreCase("string")) {
                report.error(msg(tree, bundle, "err.swaggerv2.discriminator.wrongType")
                        .putArgument("fieldName", discriminatorFieldName)
                );
                return;
            }

            final JsonNode requiredProperties = tree.getNode().get("required");
            if (requiredProperties == null ||
                    !requiredProperties.isArray() ||
                    requiredProperties.size() == 0 ||
                    !arrayNodeContains(requiredProperties, discriminatorFieldName)) {
                report.error(msg(tree, bundle, "err.swaggerv2.discriminator.notRequired")
                        .putArgument("fieldName", discriminatorFieldName)
                );
                return;
            }
        }

        private ProcessingMessage msg(final SchemaTree tree, final MessageBundle bundle, final String key) {
            return newMsg(tree, bundle, key).put("key", key);
        }
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

    /**
     * Keyword validator for the <code>discriminator</code> keyword introduced by the OpenAPI / Swagger specification.
     *
     * @see <a href="http://swagger.io/specification/#composition-and-inheritance--polymorphism--83">Swagger specification</a>
     */
    public static class DiscriminatorKeywordValidator extends AbstractKeywordValidator {

        private final ThreadLocal<Set<ObjectNode>> visitedNodes = ThreadLocal.withInitial(HashSet::new);

        private final String fieldName;
        private final JsonNode mappingNode;

        public DiscriminatorKeywordValidator(final JsonNode digest) {
            super(KEYWORD);
            fieldName = digest.get(keyword).get(PROPERTYNAME_KEYWORD).textValue();
            mappingNode = digest.get(keyword).get(MAPPING_KEYWORD);
        }

        @Override
        public void validate(final Processor<FullData, FullData> processor,
                             final ProcessingReport report,
                             final MessageBundle bundle,
                             final FullData data) throws ProcessingException {

            if (visitedNodes.get().contains(data.getSchema().getNode())) {
                // We have already validated the discriminator of this node.
                // We need to bail out to avoid a validation loop.
                visitedNodes.get().remove(data.getSchema().getNode());
                return;
            }

            JsonNode discriminatorNode = data.getInstance().getNode().get(fieldName);
            if (discriminatorNode == null) {
                report.error(
                        msg(data, bundle, "err.swaggerv2.discriminator.missing")
                                .putArgument("discriminatorField", fieldName)
                );
                return;
            }
            if (!discriminatorNode.isTextual()) {
                report.error(
                        msg(data, bundle, "err.swaggerv2.discriminator.nonText")
                                .putArgument("discriminatorField", fieldName)
                );
                return;
            }
            if (discriminatorNode.textValue().isEmpty()) {
                report.error(
                        msg(data, bundle, "err.swaggerv2.discriminator.missing")
                                .putArgument("discriminatorField", fieldName)
                );
                return;
            }

            // Valid 'subclasses' should use allOf to reference the parent schema definition
            final SchemaTree schemaTree = data.getSchema();
            final String parentDefinitionRef = "#" + schemaTree.getPointer().toString();
            final Map<String, JsonNode> validDiscriminatorValues = new HashMap<>();

            definitionsNode(data).fields().forEachRemaining(e -> {
                final JsonNode def = e.getValue();
                if (!def.has("allOf")) {
                    return;
                }

                def.get("allOf").forEach(n -> {
                    if (n.has("$ref") && n.get("$ref").textValue().equals(parentDefinitionRef)) {
                        validDiscriminatorValues.put(e.getKey(), def);
                    }
                });

            });

            if (mappingNode != null && mappingNode.get(discriminatorNode.textValue()) != null) {
                discriminatorNode = mappingNode.get(discriminatorNode.textValue());
            }

            if (!validDiscriminatorValues.containsKey(discriminatorNode.textValue())) {
                report.error(
                        msg(data, bundle, "err.swaggerv2.discriminator.invalid")
                                .putArgument("discriminatorField", fieldName)
                                .putArgument("value", discriminatorNode.textValue())
                                .putArgument("allowedValues", validDiscriminatorValues.keySet())
                );
            }

            final ListProcessingReport subReport = new ListProcessingReport(report.getLogLevel(), LogLevel.FATAL);
            final JsonPointer ptr = pointerToDiscriminator(data, discriminatorNode);
            final FullData newData = data.withSchema(schemaTree.setPointer(ptr));

            // Mark the node to ensure we don't get in a validation loop
            visitedNodes.get().add((ObjectNode) schemaTree.getNode());

            // Validate against the sub-schema
            processor.process(subReport, newData);

            if (!subReport.isSuccess()) {
                report.error(msg(data, bundle, "err.swaggerv2.discriminator.fail")
                        .putArgument("schema", ptr.toString())
                        .put("report", subReport.asJson()));
            }

        }

        private JsonPointer pointerToDiscriminator(final FullData data, final JsonNode discriminatorNode) {
            // Swagger 2.0 used 'definitions' while OpenAPI uses 'components/schemas'
            if (data.getSchema().getBaseNode().has("components")) {
                return JsonPointer.of("components", "schemas", discriminatorNode.textValue());
            }
            return JsonPointer.of("definitions", discriminatorNode.textValue());
        }

        private JsonNode definitionsNode(final FullData data) {
            // Swagger 2.0 used 'definitions' while OpenAPI uses 'components/schemas'
            final JsonNode baseNode = data.getSchema().getBaseNode();
            if (baseNode.has("components")) {
                return baseNode.get("components").get("schemas");
            }
            return baseNode.get("definitions");
        }

        @Override
        public String toString() {
            return keyword;
        }

        private ProcessingMessage msg(final FullData data, final MessageBundle bundle, final String key) {
            return newMsg(data, bundle, key).put("key", key);
        }
    }

    private static boolean arrayNodeContains(final JsonNode requiredProperties, final String element) {
        return stream(requiredProperties.elements()).anyMatch(e -> e.textValue().equals(element));
    }

    private Discriminator() {
    }
}
