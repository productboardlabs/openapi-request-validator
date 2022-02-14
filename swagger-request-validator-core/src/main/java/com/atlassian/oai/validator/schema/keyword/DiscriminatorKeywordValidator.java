package com.atlassian.oai.validator.schema.keyword;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.TokenResolver;
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
import org.slf4j.Logger;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Keyword validator for the <code>discriminator</code> keyword introduced by the OpenAPI / Swagger specification.
 * <p>
 * Implements the following validations:
 * <ol>
 *     <li>The defined discriminator {@code propertyName} must exist and must be a non-empty String value</li>
 *     <li>The defined discriminator {@code propertyName} value must be the shorthand name of the "child" schema,
 *     or a valid value from the mapping node</li>
 * </ol>
 *
 * @see <a href="http://swagger.io/specification/#composition-and-inheritance--polymorphism--83">Swagger specification</a>
 */
@NotThreadSafe
public class DiscriminatorKeywordValidator extends AbstractKeywordValidator {

    private static final Logger log = getLogger(DiscriminatorKeywordValidator.class);
    private static final String VALIDATION_PROPERTY_NAME = "_discriminatorValidation";

    private final String propertyName;
    private final JsonNode mappingNode;

    public DiscriminatorKeywordValidator(final JsonNode digest) {
        super(Discriminator.KEYWORD);
        propertyName = digest.get(keyword).get(Discriminator.PROPERTYNAME_KEYWORD).textValue();
        mappingNode = digest.get(keyword).get(Discriminator.MAPPING_KEYWORD);
    }

    @Override
    public void validate(final Processor<FullData, FullData> processor,
                         final ProcessingReport report,
                         final MessageBundle bundle,
                         final FullData data) throws ProcessingException {
        doValidate(processor, report, bundle, data);
    }

    public void doValidate(final Processor<FullData, FullData> processor,
                           final ProcessingReport report,
                           final MessageBundle bundle,
                           final FullData data) throws ProcessingException {
        // The defined discriminator property must exist
        final JsonNode discriminatorNode = data.getInstance().getNode().get(propertyName);
        if (discriminatorNode == null) {
            report.error(
                    msg(data, bundle, "err.swaggerv2.discriminator.missing")
                            .putArgument("discriminatorField", propertyName)
            );
            return;
        }

        // And it must be a String value
        if (!discriminatorNode.isTextual()) {
            report.error(
                    msg(data, bundle, "err.swaggerv2.discriminator.nonText")
                            .putArgument("discriminatorField", propertyName)
            );
            return;
        }

        // And it must not be empty
        final String discriminatorPropertyValue = discriminatorNode.textValue();
        if (discriminatorPropertyValue.isEmpty()) {
            report.error(
                    msg(data, bundle, "err.swaggerv2.discriminator.missing")
                            .putArgument("discriminatorField", propertyName)
            );
            return;
        }

        // TODO: `oneOf` and `anyOf` composition validation logic
        final JsonNode currentSchemaNode = data.getSchema().getNode();
        if (currentSchemaNode.has("oneOf") || currentSchemaNode.has("anyOf")) {
            log.debug("Support for discriminators with oneOf/anyOf not implemented yet. Validation may be inaccurate.");
            return;
        }

        validateAllOfComposition(processor, report, bundle, data, discriminatorNode);
    }

    /**
     * In <code>allOf</code> composition we are starting at the "parent" schema and using the discriminator
     * to select a "child" schema to validate the data against.
     * <p>
     * The complication is that, by necessity, the "child" schema references the "parent" schema and thus creates
     * a validation loop that needs to be handled.
     * <p>
     * Selection of a "child" schema is as follows:
     * <ol>
     *     <li>
     *         If no <code>mapping</code> is defined, the discriminator value is used as a "shortname" to match with
     *         e.g. "Dog" -> "#/components/schemas/Dog"
     *     </li>
     *     <li>
     *         If a <code>mapping</code> is defined, the discriminator value is used to lookup the appropriate schema.
     *         This could be a "shortname", a relative ref, or an external ref.
     *     </li>
     * </ol>
     */
    private void validateAllOfComposition(final Processor<FullData, FullData> processor,
                                          final ProcessingReport report,
                                          final MessageBundle bundle,
                                          final FullData data,
                                          final JsonNode discriminatorNode) throws ProcessingException {
        final SchemaTree schemaTree = data.getSchema();

        final String discriminatorPropertyValue = discriminatorNode.textValue();

        // The given discriminator value must either match a mapping OR a valid child schema
        final Map<String, JsonNode> validDiscriminatorValues = findValidDiscriminatorValues(data,
                "#" + filterDiscriminatorValidationNodes(schemaTree.getPointer()).toString()
        );
        if (!validDiscriminatorValues.containsKey(discriminatorPropertyValue)) {
            report.error(
                    msg(data, bundle, "err.swaggerv2.discriminator.invalid")
                            .putArgument("discriminatorField", propertyName)
                            .putArgument("value", discriminatorPropertyValue)
                            .putArgument("allowedValues", validDiscriminatorValues.keySet())
            );
        }

        // Select the child schema based on the discriminator
        final JsonPointer ptrToChildSchema = pointerToDiscriminatedSchema(
                data, mappedDiscriminatorNode(discriminatorNode, discriminatorPropertyValue)
        );

        final SchemaTree childSchemaTree = schemaTree.setPointer(ptrToChildSchema);
        final ListProcessingReport subReport = new ListProcessingReport(report.getLogLevel(), LogLevel.FATAL);
        if (childSchemaTree.getNode() == null || childSchemaTree.getNode().isMissingNode()) {
            report.error(msg(data, bundle, "err.swaggerv2.discriminator.reference.invalid")
                    .putArgument("schema", ptrToChildSchema.toString())
                    .put("report", subReport.asJson()));
            return;
        }

        /*
         * See https://bitbucket.org/atlassian/swagger-request-validator/issues/269/validation-loop-error-occurred-when-allof
         * We can have a validation loop by saying "A Car must be a Vehicle. A Vehicle must be EITHER a Car or a Plane".
         * This causes this code to get called to check that a Vehicle is a Car - BUT we've already visited the Vehicle
         * schema for the same object, so we can't do so a second time on the same parser stack.
         *
         * To prevent a validation loop from occurring in that situation, we need a "different" schema for Car. The way
         * we make one is by attaching a #/definitions/Car/_discriminatorValidation/Vehicle~1myCar JSON node with a complete copy of
         * the Car schema, and using THAT to do the child schema validation here.
         *
         * We avoid allowing this to produce an infinite loop by checking if the JSON node for this combination of
         * subschema and document instance. If it exists, we have already validated the discriminator, and we can bail
         * out early.
         */

        final ObjectNode childSchemaAsObject = (ObjectNode) childSchemaTree.getNode();
        if (!childSchemaAsObject.has(VALIDATION_PROPERTY_NAME)) {
            final JsonNode emptyJsonObject = childSchemaAsObject.deepCopy().removeAll();
            childSchemaAsObject.set(VALIDATION_PROPERTY_NAME, emptyJsonObject);
        }

        final ObjectNode validationPropertyNode = (ObjectNode) childSchemaAsObject.get(VALIDATION_PROPERTY_NAME);

        final String discriminatorValidationContextString = new VisitedInfo(
                data.getInstance().getPointer(),
                data.getSchema().getPointer()
        ).toString();

        if (validationPropertyNode.has(discriminatorValidationContextString)) {
            // We have been here before for this exact instance object. Nothing more to do.
            return;
        }

        validationPropertyNode.set(discriminatorValidationContextString, childSchemaAsObject.deepCopy());

        final SchemaTree childSchemaTreeWithRewrittenPointer = childSchemaTree.setPointer(
                ptrToChildSchema.append(VALIDATION_PROPERTY_NAME).append(discriminatorValidationContextString)
        );

        // Validate against the selected child schema
        try {
            final FullData newData = data.withSchema(childSchemaTreeWithRewrittenPointer);
            processor.process(subReport, newData);
        } finally {
            validationPropertyNode.remove(discriminatorValidationContextString);
        }

        if (!subReport.isSuccess()) {
            final String stringToReplace = "/" + VALIDATION_PROPERTY_NAME +
                    "/" + discriminatorValidationContextString.replaceAll("\\/", "~1");

            final JsonNode reportAsJson = subReport.asJson();

            replaceReportOutput(reportAsJson, stringToReplace, "");

            report.error(msg(data, bundle, "err.swaggerv2.discriminator.fail")
                    .putArgument("schema", ptrToChildSchema.toString())
                    .put("reports", reportAsJson));
        }
    }

    /**
     * Remove discriminator validation nodes from a JSON pointer
     *
     * @param originalPointer Original JSON pointer to filter
     * @return New JSON pointer with discriminator validation nodes removed
     */
    private JsonPointer filterDiscriminatorValidationNodes(final JsonPointer originalPointer) {
        JsonPointer ret = JsonPointer.empty();
        final Iterator<TokenResolver<JsonNode>> pointerPartIterator = originalPointer.iterator();
        while (pointerPartIterator.hasNext()) {
            final TokenResolver<JsonNode> pointerPart = pointerPartIterator.next();
            if (VALIDATION_PROPERTY_NAME.equals(pointerPart.toString())) {
                // Skip this AND the next token after it
                pointerPartIterator.next();
            } else {
                ret = ret.append(pointerPart.toString());
            }
        }
        return ret;
    }

    /**
     * Walk a JSON object representing a report and replace all instances of the search string
     * in schema pointers or report paths with the given replacement value
     */
    private void replaceReportOutput(
            final JsonNode reportInput,
            final String searchString,
            final String replaceString
    ) {
        if (reportInput.isArray()) {
            reportInput.forEach(it -> {
                replaceReportOutput(it, searchString, replaceString);
            });
            return;
        }
        if (!reportInput.isObject()) {
            return;
        }
        if (reportInput.has("reports")) {
            if (reportInput.get("reports").isObject()) {
                final ObjectNode reportsAsJson = (ObjectNode) reportInput.get("reports");

                final ArrayList<String> propertiesToModify = new ArrayList<>();
                reportsAsJson.fields().forEachRemaining(it -> {
                    final String reportFieldName = it.getKey();
                    final JsonNode reportValue = it.getValue();
                    replaceReportOutput(reportValue, searchString, replaceString);

                    if (reportFieldName.contains(searchString)) {
                        propertiesToModify.add(reportFieldName);
                    }

                });

                propertiesToModify.forEach(reportFieldName -> {
                    final String replacementFieldName = reportFieldName.replaceAll(searchString, replaceString);
                    reportsAsJson.set(replacementFieldName, reportsAsJson.get(reportFieldName));
                    reportsAsJson.remove(reportFieldName);
                });
            }
        }
        if (reportInput.has("schema")) {
            if (reportInput.get("schema").isObject()) {
                final ObjectNode schemaObject = (ObjectNode) reportInput.get("schema");
                if (schemaObject.has("pointer")) {
                    schemaObject.put("pointer", schemaObject.get("pointer").textValue().replaceAll(searchString, replaceString));
                }
            }
        }
    }

    private JsonNode mappedDiscriminatorNode(final JsonNode originalDiscriminatorNode,
                                             final String discriminatorPropertyValue) {
        if (mappingNode != null && mappingNode.get(discriminatorPropertyValue) != null) {
            return mappingNode.get(discriminatorPropertyValue);
        }
        return originalDiscriminatorNode;
    }

    /**
     * Find the valid values for the discriminator property.
     * <p>
     * These will be:
     * <ol>
     *     <li>The contents of the mapping node (if it exists)</li>
     *     <li>Any schema using `allOf` composition referencing the "parent" schema</li>
     * </ol>
     * The returned map will be keyed by the short name of the schema (e.g. un-qualified name).
     *
     * @return A mapping between schema name and schema object for candidate schemas
     */
    private Map<String, JsonNode> findValidDiscriminatorValues(final FullData data,
                                                               final String parentDefinitionRef) {
        final Map<String, JsonNode> validDiscriminatorValues = new HashMap<>();

        // Find definitions that reference the "parent" via allOf
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

        if (mappingNode != null) {
            // TODO: These may be fully-qualified refs here
            mappingNode.fields().forEachRemaining(e -> validDiscriminatorValues.put(e.getKey(), e.getValue()));
        }

        return validDiscriminatorValues;
    }

    private JsonPointer pointerToDiscriminatedSchema(final FullData data, final JsonNode discriminatorNode) {
        // TODO: Handle absolute/external refs
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

    /**
     * Container used to track which nodes in the instance have been validated against which nodes in the schema.
     * <p>
     * Used to avoid validation cycles caused by the fact that in the <code>allOf</code> scenario we end up with a cycle
     * <code>parent -> child -> parent</code>
     */
    private static class VisitedInfo {
        private final JsonPointer instancePointer;
        private final JsonPointer schemaPointer;

        public VisitedInfo(final JsonPointer instancePointer, final JsonPointer schemaPointer) {
            this.instancePointer = instancePointer;
            this.schemaPointer = schemaPointer;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final VisitedInfo that = (VisitedInfo) o;
            return Objects.equals(instancePointer, that.instancePointer) &&
                    Objects.equals(schemaPointer, that.schemaPointer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(instancePointer, schemaPointer);
        }

        @Override
        public String toString() {
            return schemaPointer.toString() + "//" + instancePointer.toString();
        }
    }

    @Override
    public String toString() {
        return keyword;
    }

    private ProcessingMessage msg(final FullData data, final MessageBundle bundle, final String key) {
        return newMsg(data, bundle, key).put("key", key);
    }
}
