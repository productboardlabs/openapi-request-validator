package com.atlassian.oai.validator.schema.keyword;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.processing.Processor;
import com.github.fge.jsonschema.core.report.ListProcessingReport;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.jsonschema.keyword.validator.AbstractKeywordValidator;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keyword validator for the <code>discriminator</code> keyword introduced by the OpenAPI / Swagger specification.
 *
 * @see <a href="http://swagger.io/specification/#composition-and-inheritance--polymorphism--83">Swagger specification</a>
 */
@NotThreadSafe
public class DiscriminatorKeywordValidator extends AbstractKeywordValidator {

    private final Set<JsonNode> visitedNodes = new HashSet<>();

    private final String fieldName;
    private final JsonNode mappingNode;

    public DiscriminatorKeywordValidator(final JsonNode digest) {
        super(Discriminator.KEYWORD);
        fieldName = digest.get(keyword).get(Discriminator.PROPERTYNAME_KEYWORD).textValue();
        mappingNode = digest.get(keyword).get(Discriminator.MAPPING_KEYWORD);
    }

    @Override
    public void validate(final Processor<FullData, FullData> processor,
                         final ProcessingReport report,
                         final MessageBundle bundle,
                         final FullData data) throws ProcessingException {

        if (visitedNodes.contains(data.getSchema().getNode())) {
            // We have already validated the discriminator of this node.
            // We need to bail out to avoid a validation loop.
            visitedNodes.remove(data.getSchema().getNode());
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

        final boolean useMappingNode = mappingNode != null && mappingNode.get(discriminatorNode.textValue()) != null;
        if (useMappingNode) {
            mappingNode.fields().forEachRemaining(e -> validDiscriminatorValues.put(e.getKey(), e.getValue()));
        } else if (data.getSchema().getNode().has("oneOf")) {
            data.getSchema().getNode().get("oneOf").forEach(jsonNode ->
                    // the oneOf $refs are resolved already, so we have to look up theirs schema names
                    definitionsNode(data).fields().forEachRemaining(entry -> {
                        if (entry.getValue().equals(jsonNode)) {
                            validDiscriminatorValues.put(entry.getKey(), entry.getValue());
                        }
                    }));
        }

        if (!validDiscriminatorValues.containsKey(discriminatorNode.textValue())) {
            report.error(
                    msg(data, bundle, "err.swaggerv2.discriminator.invalid")
                            .putArgument("discriminatorField", fieldName)
                            .putArgument("value", discriminatorNode.textValue())
                            .putArgument("allowedValues", validDiscriminatorValues.keySet())
            );
        }

        if (useMappingNode) {
            discriminatorNode = mappingNode.get(discriminatorNode.textValue());
        }

        final ListProcessingReport subReport = new ListProcessingReport(report.getLogLevel(), LogLevel.FATAL);
        final JsonPointer ptr = pointerToDiscriminator(data, discriminatorNode);
        final FullData newData = data.withSchema(schemaTree.setPointer(ptr));

        if (newData.getSchema().getNode() == null) {
            report.error(msg(data, bundle, "err.swaggerv2.discriminator.reference.invalid")
                    .putArgument("schema", ptr.toString())
                    .put("report", subReport.asJson()));
            return;
        }

        // Mark the node to ensure we don't get in a validation loop
        visitedNodes.add(schemaTree.getNode());

        // Validate against the sub-schema
        processor.process(subReport, newData);

        if (!subReport.isSuccess()) {
            report.error(msg(data, bundle, "err.swaggerv2.discriminator.fail")
                    .putArgument("schema", ptr.toString())
                    .put("report", subReport.asJson()));
        }
    }

    private JsonPointer pointerToDiscriminator(final FullData data, final JsonNode discriminatorNode) {
        final String discriminatorNodeText = normalizeDiscriminatorNode(discriminatorNode.textValue());
        // Swagger 2.0 used 'definitions' while OpenAPI uses 'components/schemas'
        if (data.getSchema().getBaseNode().has("components")) {
            return JsonPointer.of("components", "schemas", discriminatorNodeText);
        }
        return JsonPointer.of("definitions", discriminatorNodeText);
    }

    private JsonNode definitionsNode(final FullData data) {
        // Swagger 2.0 used 'definitions' while OpenAPI uses 'components/schemas'
        final JsonNode baseNode = data.getSchema().getBaseNode();
        if (baseNode.has("components")) {
            return baseNode.get("components").get("schemas");
        }
        return baseNode.get("definitions");
    }

    private String normalizeDiscriminatorNode(final String discriminatorNodeText) {
        if (discriminatorNodeText.startsWith("#/")) {
            final int n = discriminatorNodeText.lastIndexOf('/');
            return discriminatorNodeText.substring(n + 1);
        }
        return discriminatorNodeText;
    }

    @Override
    public String toString() {
        return keyword;
    }

    private ProcessingMessage msg(final FullData data, final MessageBundle bundle, final String key) {
        return newMsg(data, bundle, key).put("key", key);
    }
}
