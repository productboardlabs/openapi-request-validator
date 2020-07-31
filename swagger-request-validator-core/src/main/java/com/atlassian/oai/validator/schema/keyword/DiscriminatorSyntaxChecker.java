package com.atlassian.oai.validator.schema.keyword;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.NodeType;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jsonschema.core.exceptions.JsonReferenceException;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.keyword.syntax.checkers.AbstractSyntaxChecker;
import com.github.fge.jsonschema.core.ref.JsonRef;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.core.tree.SchemaTree;
import com.github.fge.msgsimple.bundle.MessageBundle;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * Syntax checker for the <code>discriminator</code> keyword introduced by the OpenAPI / Swagger specification.
 * <p>
 * According to https://swagger.io/specification/#discriminator-object:
 * <ul>
 *     <li>{@code propertyName} is required and defines the name of the property in the payload that will hold the discriminator value.</li>
 *     <li>{@code mapping} is an optional object containing a mapping between property values and schema names or references</li>
 * </ul>
 * In the case of composition via {@code allOf}, the property name must exist in the object in which the discriminator is defined.
 * For {@code oneOf} and {@code anyOf} composition, the property name must exist in <em>all</em> of the referenced schemas.
 * <p>
 * If a {@code mapping} is used, the listed schemas <em>must</em> match a schema definition.
 *
 * @see <a href="https://swagger.io/specification/#discriminator-object">Swagger specification</a>
 */
public class DiscriminatorSyntaxChecker extends AbstractSyntaxChecker {

    private static final DiscriminatorSyntaxChecker INSTANCE = new DiscriminatorSyntaxChecker();

    static DiscriminatorSyntaxChecker getInstance() {
        return INSTANCE;
    }

    DiscriminatorSyntaxChecker() {
        super(Discriminator.KEYWORD, NodeType.OBJECT);
    }

    @Override
    protected void checkValue(final Collection<JsonPointer> pointers,
                              final MessageBundle bundle,
                              final ProcessingReport report,
                              final SchemaTree tree) throws ProcessingException {
        // "propertyName" is required and must be non-empty
        final JsonNode propertyNameNode = getNode(tree).get(Discriminator.PROPERTYNAME_KEYWORD);
        if (propertyNameNode == null) {
            report.error(msg(tree, bundle, "err.swaggerv2.discriminator.propertyName.required"));
            return;
        }

        final String discriminatorPropertyName = propertyNameNode.textValue();
        if (discriminatorPropertyName == null || discriminatorPropertyName.isEmpty()) {
            report.error(msg(tree, bundle, "err.swaggerv2.discriminator.propertyName.empty"));
            return;
        }

        // if "mapping" is defined it must be an object
        final JsonNode mappingNode = getNode(tree).get(Discriminator.MAPPING_KEYWORD);
        if (mappingNode != null && !mappingNode.isObject()) {
            report.error(msg(tree, bundle, "err.swaggerv2.discriminator.mapping.wrongType"));
            return;
        }

        // TODO: The mapping node must reference valid schemas

        // For `anyOf` and `oneOf` composition, check each referenced schema for the discriminator property
        if (tree.getNode().get("oneOf") != null || tree.getNode().get("anyOf") != null) {
            // TODO: Check referenced schemas for the named property
            return;
        }

        // For `allOf` composition check the current schema for the discriminator property
        validatePropertyName(bundle, report, tree, tree.getNode(), discriminatorPropertyName);
    }

    private void validatePropertyName(final MessageBundle bundle,
                                      final ProcessingReport report,
                                      final SchemaTree tree,
                                      final JsonNode node,
                                      final String discriminatorPropertyName) throws ProcessingException {
        // The discriminator property must be a property on this schema
        final JsonNode properties = node.get("properties");
        final List<String> propertyNames = stream(properties.fieldNames()).collect(toList());
        if (!properties.has(discriminatorPropertyName)) {
            report.error(msg(tree, bundle, "err.swaggerv2.discriminator.noProperty")
                    .putArgument("fieldName", discriminatorPropertyName)
                    .putArgument("properties", propertyNames)
            );
            return;
        }

        // The discriminator property must be defined as a string
        final JsonNode property = properties.get(discriminatorPropertyName);
        final String type = getTypeOfProperty(tree, property);
        if (!"string".equalsIgnoreCase(type)) {
            report.error(msg(tree, bundle, "err.swaggerv2.discriminator.wrongType")
                    .putArgument("fieldName", discriminatorPropertyName)
            );
            return;
        }

        // The discriminator property must be marked as required
        final JsonNode requiredProperties = tree.getNode().get("required");
        if (requiredProperties == null ||
                !requiredProperties.isArray() ||
                requiredProperties.size() == 0 ||
                !arrayNodeContains(requiredProperties, discriminatorPropertyName)) {
            report.error(msg(tree, bundle, "err.swaggerv2.discriminator.notRequired")
                    .putArgument("fieldName", discriminatorPropertyName)
            );
        }
    }

    private ProcessingMessage msg(final SchemaTree tree, final MessageBundle bundle, final String key) {
        return newMsg(tree, bundle, key).put("key", key);
    }

    private String getTypeOfProperty(final SchemaTree tree, final JsonNode property) throws JsonReferenceException {
        if (property.has("type")) {
            return property.get("type").textValue();
        } else if (property.has("$ref")) {
            final JsonRef ref = JsonRef.fromString(property.get("$ref").textValue());
            final JsonNode referent = tree.matchingPointer(ref).get(tree.getBaseNode());
            if (referent == null || referent.get("type") == null) {
                return null;
            } else {
                return referent.get("type").textValue();
            }
        } else {
            return null;
        }
    }

    private static boolean arrayNodeContains(final JsonNode requiredProperties, final String element) {
        return stream(requiredProperties.elements()).anyMatch(e -> e.textValue().equals(element));
    }

    private static <T> Stream<T> stream(final Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }
}
