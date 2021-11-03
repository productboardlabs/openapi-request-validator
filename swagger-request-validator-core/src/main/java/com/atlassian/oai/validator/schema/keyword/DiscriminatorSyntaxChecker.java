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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Optional.empty;
import static java.util.Optional.of;

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
 * If a {@code mapping} is used, the listed schemas <em>must</em> match a schema definition, or be an external ref.
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

        validateMapping(bundle, report, tree);

        // For `anyOf` and `oneOf` composition, check each referenced schema for the discriminator property
        final Optional<JsonNode> maybeCompositionNode = getOneOrAnyOfCompositionNodeIfPresent(tree.getNode());
        if (maybeCompositionNode.isPresent()) {
            final JsonNode compositionNode = maybeCompositionNode.get();
            final Iterator<JsonNode> children = compositionNode.elements();
            while (children.hasNext()) {
                validatePropertyName(bundle, report, tree, children.next(), discriminatorPropertyName);
            }
            return;
        }

        // For `allOf` composition check the current schema for the discriminator property
        validatePropertyName(bundle, report, tree, tree.getNode(), discriminatorPropertyName);
    }

    private Optional<JsonNode> getOneOrAnyOfCompositionNodeIfPresent(final JsonNode node) {
        return Optional.ofNullable(node.has("oneOf") ? node.get("oneOf") : node.get("anyOf"));
    }

    private void validatePropertyName(final MessageBundle bundle,
                                      final ProcessingReport report,
                                      final SchemaTree tree,
                                      final JsonNode node,
                                      final String discriminatorPropertyName) throws ProcessingException {

        // The discriminator property must be defined here or in a referenced schema
        final Optional<PropertyLookupResult> maybePropertyLookupResult = findProperty(tree, node, discriminatorPropertyName, new HashSet<>());
        if (!maybePropertyLookupResult.isPresent()) {
            report.error(msg(tree, bundle, "err.swaggerv2.discriminator.propertyName.noProperty")
                    .putArgument("fieldName", discriminatorPropertyName)
            );
            return;
        }

        // The discriminator property must be defined as a string
        final JsonNode propertyNode = maybePropertyLookupResult.get().getPropertyNode();
        final String type = getTypeOfProperty(tree, propertyNode);
        if (!"string".equalsIgnoreCase(type)) {
            report.error(msg(tree, bundle, "err.swaggerv2.discriminator.wrongType")
                    .putArgument("fieldName", discriminatorPropertyName)
            );
            return;
        }

        // The discriminator property must be marked as required
        final JsonNode requiredProperties = maybePropertyLookupResult.get().getParentNode().get("required");
        if (requiredProperties == null ||
                !requiredProperties.isArray() ||
                requiredProperties.size() == 0 ||
                !arrayNodeContains(requiredProperties, discriminatorPropertyName)) {
            report.error(msg(tree, bundle, "err.swaggerv2.discriminator.notRequired")
                    .putArgument("fieldName", discriminatorPropertyName)
            );
        }
    }

    private void validateMapping(final MessageBundle bundle,
                                 final ProcessingReport report,
                                 final SchemaTree tree) throws ProcessingException {
        final JsonNode mappingNode = getNode(tree).get(Discriminator.MAPPING_KEYWORD);
        // Mapping is optional
        if (mappingNode == null) {
            return;
        }

        // If defined, it must be an object
        if (!mappingNode.isObject()) {
            report.error(msg(tree, bundle, "err.swaggerv2.discriminator.mapping.wrongType"));
            return;
        }

        // The values must be references to schemas
        final Iterator<Map.Entry<String, JsonNode>> mappings = mappingNode.fields();
        while (mappings.hasNext()) {
            final Map.Entry<String, JsonNode> mapping = mappings.next();

            // Mappings must be textual values
            if (!mapping.getValue().isTextual()) {
                report.error(msg(tree, bundle, "err.swaggerv2.discriminator.mapping.value.invalidType")
                        .putArgument("mappingName", mapping.getKey()));
            }

            // Must be a valid "shortname" OR a valid ref
            final String mappingValue = mapping.getValue().textValue();
            if (tree.matchingPointer(JsonRef.fromString(mappingValue)) != null ||
                    tree.matchingPointer(shortnameRef(tree, mappingValue)) != null) {
                continue;
            }

            report.error(msg(tree, bundle, "err.swaggerv2.discriminator.mapping.value.invalidRef")
                    .putArgument("mappingName", mapping.getKey())
                    .putArgument("mappingValue", mappingValue));
        }
    }

    private static class PropertyLookupResult {
        private final String propertyName;
        private final JsonNode propertyNode;
        private final JsonNode parentNode;

        public PropertyLookupResult(final String propertyName,
                                    final JsonNode propertyNode,
                                    final JsonNode parentNode) {
            this.propertyName = propertyName;
            this.propertyNode = propertyNode;
            this.parentNode = parentNode;
        }

        public JsonNode getParentNode() {
            return parentNode;
        }

        public JsonNode getPropertyNode() {
            return propertyNode;
        }

        public String getPropertyName() {
            return propertyName;
        }
    }

    /**
     * Find a property on the given node (in the `properties` block) OR by following $refs OR by resolving `allOf` refs.
     *
     * @return The node defining the property, or {@code empty} if none is found.
     */
    private Optional<PropertyLookupResult> findProperty(final SchemaTree tree,
                                                        final JsonNode node,
                                                        final String propertyName,
                                                        final Set<JsonNode> visitedNodes) throws JsonReferenceException {
        if (visitedNodes.contains(node)) {
            // We have already inspected this node; Bail out.
            return empty();
        }
        visitedNodes.add(node);

        if (node.has("properties")) {
            final JsonNode propertiesNode = node.get("properties");
            if (!propertiesNode.has(propertyName)) {
                return empty();
            }
            return of(new PropertyLookupResult(propertyName, propertiesNode.get(propertyName), node));
        }

        if (node.has("$ref")) {
            final JsonRef ref = JsonRef.fromString(node.get("$ref").textValue());
            final JsonPointer jsonPointer = tree.matchingPointer(ref);
            if (jsonPointer == null) {
                return empty();
            }
            final JsonNode referencedNode = jsonPointer.get(tree.getBaseNode());
            return findProperty(tree, referencedNode, propertyName, visitedNodes);
        }

        if (node.has("allOf")) {
            final JsonNode allOfNode = node.get("allOf");
            final Iterator<JsonNode> children = allOfNode.elements();
            while (children.hasNext()) {
                final Optional<PropertyLookupResult> maybeProperty = findProperty(tree, children.next(), propertyName, visitedNodes);
                if (maybeProperty.isPresent()) {
                    return maybeProperty;
                }
            }
        }

        return empty();
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

    private static boolean arrayNodeContains(final JsonNode arrayNode, final String element) {
        return stream(arrayNode.elements()).anyMatch(e -> e.textValue().equals(element));
    }

    private static <T> Stream<T> stream(final Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
    }

    private JsonRef shortnameRef(final SchemaTree tree, final String shortname) throws JsonReferenceException {
        // Swagger 2.0 used 'definitions' while OpenAPI uses 'components/schemas'
        if (shortname.startsWith("#/components") || shortname.startsWith("#/definitions")) {
            return JsonRef.fromString(shortname);
        }
        if (tree.getBaseNode().has("components")) {
            return JsonRef.fromString("#/components/schemas/" + shortname);
        }
        return JsonRef.fromString("#/definitions/" + shortname);
    }
}
