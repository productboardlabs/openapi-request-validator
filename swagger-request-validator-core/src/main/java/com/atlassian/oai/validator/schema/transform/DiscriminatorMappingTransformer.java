package com.atlassian.oai.validator.schema.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Transformer that resolves OAS-style {@code discriminator + mapping} into
 * inline {@code enum} constraints on each composition branch's discriminator
 * property.
 *
 * <p><strong>Why this is needed:</strong> swagger-parser's {@code resolveFully}
 * option (enabled by default in this validator) inlines all {@code $ref}-d
 * component schemas. After resolution, the {@code discriminator.mapping} entries
 * still point to the original ref locations (e.g. {@code #/components/schemas/Circle})
 * but the corresponding {@code oneOf}/{@code anyOf} branches no longer carry
 * those refs — they have been replaced by inline schemas. networknt cannot then
 * match a payload's discriminator value to a branch by ref, so OAS-style
 * discriminator validation fails with "no alternative could be chosen".
 *
 * <p><strong>What this transformer does:</strong> for each composition that has
 * a {@code discriminator} with a {@code mapping}, walk the mapping entries. For
 * each {@code (discriminatorValue -> $ref)} pair, locate the matching inlined
 * branch by structural identity to the referenced component schema, then inject
 * an {@code enum: [discriminatorValue]} constraint into that branch's
 * {@code properties.<propertyName>} schema. After this, networknt's standard
 * {@code oneOf}/{@code anyOf} evaluation correctly disambiguates branches based
 * on the payload's discriminator property, regardless of whether refs survived
 * resolution.
 *
 * <p>Branches that already declare an {@code enum} or {@code const} on the
 * discriminator property are left alone.
 */
public class DiscriminatorMappingTransformer extends SchemaTransformer {

    private static final DiscriminatorMappingTransformer INSTANCE = new DiscriminatorMappingTransformer();

    private static final String DISCRIMINATOR_FIELD = "discriminator";
    private static final String PROPERTY_NAME_FIELD = "propertyName";
    private static final String MAPPING_FIELD = "mapping";
    private static final String ENUM_FIELD = "enum";
    private static final String CONST_FIELD = "const";

    public static DiscriminatorMappingTransformer getInstance() {
        return INSTANCE;
    }

    @Override
    public void apply(final JsonNode schemaObject, final SchemaTransformationContext context) {
        if (schemaObject == null || !(schemaObject instanceof ObjectNode)) {
            return;
        }

        applyAtNode((ObjectNode) schemaObject, context);

        applyToChildSchemas(schemaObject, child -> apply(child, context));
    }

    private void applyAtNode(final ObjectNode schemaObject, final SchemaTransformationContext context) {
        final JsonNode discriminator = schemaObject.get(DISCRIMINATOR_FIELD);
        if (discriminator == null || !discriminator.isObject()) {
            return;
        }
        final JsonNode propertyNameNode = discriminator.get(PROPERTY_NAME_FIELD);
        if (propertyNameNode == null || !propertyNameNode.isTextual()) {
            return;
        }
        final String propertyName = propertyNameNode.textValue();

        // Build value -> branch-key index from explicit mapping (if any).
        final Map<String, String> valueToRef = new HashMap<>();
        final JsonNode mapping = discriminator.get(MAPPING_FIELD);
        if (mapping != null && mapping.isObject()) {
            mapping.fields().forEachRemaining(entry -> {
                if (entry.getValue() != null && entry.getValue().isTextual()) {
                    valueToRef.put(entry.getKey(), entry.getValue().textValue());
                }
            });
        }

        // Walk allOf/anyOf/oneOf composition branches.
        final boolean injectedAny =
                injectIntoBranches(schemaObject.get(ALLOF_FIELD), propertyName, valueToRef, context)
              | injectIntoBranches(schemaObject.get(ANYOF_FIELD), propertyName, valueToRef, context)
              | injectIntoBranches(schemaObject.get(ONEOF_FIELD), propertyName, valueToRef, context);

        // After injecting per-branch enum constraints, the OAS-style
        // discriminator keyword is redundant — and worse, networknt's
        // discriminator support tries to match branches by $ref, which fails
        // when refs have been inlined by resolveFully. Strip the discriminator
        // so networknt falls back to standard oneOf evaluation, which the
        // injected enum constraints now correctly disambiguate.
        if (injectedAny) {
            schemaObject.remove(DISCRIMINATOR_FIELD);
        }
    }

    private boolean injectIntoBranches(final JsonNode branches, final String propertyName,
                                       final Map<String, String> valueToRef,
                                       final SchemaTransformationContext context) {
        if (branches == null || !branches.isArray()) {
            return false;
        }
        boolean injectedAny = false;
        final Iterator<JsonNode> it = branches.elements();
        while (it.hasNext()) {
            final JsonNode branch = it.next();
            if (!(branch instanceof ObjectNode)) {
                continue;
            }
            final ObjectNode branchObj = (ObjectNode) branch;

            final String discriminatorValue = pickDiscriminatorValueForBranch(branchObj, valueToRef, context);
            if (discriminatorValue == null) {
                continue;
            }

            final ObjectNode propertyNode = ensurePropertyExists(branchObj, propertyName);
            if (propertyNode == null) {
                continue;
            }

            // If the branch already constrains the property to a specific value,
            // leave it alone — the developer's explicit constraint wins.
            if (propertyNode.has(ENUM_FIELD) || propertyNode.has(CONST_FIELD)) {
                injectedAny = true;
                continue;
            }

            final ArrayNode enumArr = JsonNodeFactory.instance.arrayNode();
            enumArr.add(discriminatorValue);
            propertyNode.set(ENUM_FIELD, enumArr);
            injectedAny = true;
        }
        return injectedAny;
    }

    /**
     * Decide which discriminator value belongs to a branch.
     *
     * <p>Resolution order:
     * <ol>
     *   <li><strong>Branch still has $ref</strong> (no resolveFully): pick by
     *       matching the ref against the mapping, or use the ref tail if there
     *       is no mapping.</li>
     *   <li><strong>Mapping is declared</strong>: walk the mapping entries and
     *       find the inlined branch that structurally matches the mapped
     *       component schema in components.schemas.</li>
     *   <li><strong>No mapping</strong>: walk all components.schemas entries
     *       and return the schema name that structurally matches the inlined
     *       branch — per OAS spec, the discriminator value defaults to the
     *       schema name.</li>
     *   <li><strong>Nothing matched</strong>: bail without injecting; the
     *       existing oneOf/anyOf semantics will validate as best they can.</li>
     * </ol>
     */
    private String pickDiscriminatorValueForBranch(final JsonNode branch,
                                                   final Map<String, String> valueToRef,
                                                   final SchemaTransformationContext context) {
        // Case 1: branch still has $ref (resolveFully not applied).
        final JsonNode refNode = branch.get("$ref");
        if (refNode != null && refNode.isTextual()) {
            final String ref = refNode.textValue();
            if (!valueToRef.isEmpty()) {
                for (Map.Entry<String, String> e : valueToRef.entrySet()) {
                    if (ref.equals(e.getValue())) {
                        return e.getKey();
                    }
                }
            }
            // No mapping match — use the schema name from the ref tail.
            return tailOfRef(ref);
        }

        final JsonNode definitions = context.getSchemaDefinitions();

        // Case 2: ref was inlined and an explicit mapping was declared.
        // Match each mapping entry's referenced schema against the branch
        // by structural identity.
        if (definitions != null && !valueToRef.isEmpty()) {
            for (Map.Entry<String, String> e : valueToRef.entrySet()) {
                final JsonNode targetSchema = resolveRef(definitions, e.getValue());
                if (targetSchema != null && schemasStructurallyEqual(branch, targetSchema)) {
                    return e.getKey();
                }
            }
        }

        // Case 3: ref was inlined and no mapping declared. Per spec, the
        // discriminator value defaults to the matching schema name. Walk all
        // components.schemas entries and find the structural match.
        if (definitions != null && valueToRef.isEmpty() && definitions.isObject()) {
            final Iterator<String> names = definitions.fieldNames();
            while (names.hasNext()) {
                final String schemaName = names.next();
                if (schemasStructurallyEqual(branch, definitions.get(schemaName))) {
                    return schemaName;
                }
            }
        }

        // Case 4: nothing matched — bail without injecting.
        return null;
    }

    private static String tailOfRef(final String ref) {
        final int slash = ref.lastIndexOf('/');
        return slash >= 0 ? ref.substring(slash + 1) : ref;
    }

    /**
     * Resolves a JSON Pointer ref against the given definitions root. The root
     * is the value of {@code components.schemas} (a map of name -> schema), so
     * we extract the trailing schema name from the ref and look it up directly.
     * Supports {@code #/components/schemas/Name}, {@code #/$defs/Name}, and any
     * other ref form by using the tail segment.
     */
    private static JsonNode resolveRef(final JsonNode definitionsRoot, final String ref) {
        if (ref == null || !ref.startsWith("#/")) {
            return null;
        }
        return definitionsRoot.get(tailOfRef(ref));
    }

    /**
     * Checks structural equality between two schema nodes by comparing their
     * required-properties sets and properties keys. Cheap heuristic: enough to
     * distinguish two distinct discriminator branches that would normally have
     * different shapes (e.g. Circle has `radius`, Square has `side`).
     */
    private static boolean schemasStructurallyEqual(final JsonNode a, final JsonNode b) {
        if (a == null || b == null) {
            return false;
        }
        return propertiesKeysEqual(a, b) && requiredFieldsEqual(a, b);
    }

    private static boolean propertiesKeysEqual(final JsonNode a, final JsonNode b) {
        final JsonNode pa = a.get(PROPERTIES_FIELD);
        final JsonNode pb = b.get(PROPERTIES_FIELD);
        if (pa == null || pb == null) {
            return pa == pb;
        }
        if (pa.size() != pb.size()) {
            return false;
        }
        final Iterator<String> it = pa.fieldNames();
        while (it.hasNext()) {
            if (!pb.has(it.next())) {
                return false;
            }
        }
        return true;
    }

    private static boolean requiredFieldsEqual(final JsonNode a, final JsonNode b) {
        final JsonNode ra = a.get(REQUIRED_FIELD);
        final JsonNode rb = b.get(REQUIRED_FIELD);
        if (ra == null || rb == null) {
            return ra == rb;
        }
        if (!ra.isArray() || !rb.isArray() || ra.size() != rb.size()) {
            return false;
        }
        for (int i = 0; i < ra.size(); i++) {
            boolean found = false;
            for (int j = 0; j < rb.size(); j++) {
                if (ra.get(i).equals(rb.get(j))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    /**
     * Ensures the branch has a properties object containing a mutable entry for
     * the given property name. If the branch lacks `properties` or the named
     * property, returns null (we don't mutate aggressively — let the existing
     * validation report a clear error like "missing required property").
     */
    private static ObjectNode ensurePropertyExists(final ObjectNode branch, final String propertyName) {
        final JsonNode properties = branch.get(PROPERTIES_FIELD);
        if (properties == null || !properties.isObject()) {
            return null;
        }
        final JsonNode prop = properties.get(propertyName);
        if (!(prop instanceof ObjectNode)) {
            return null;
        }
        return (ObjectNode) prop;
    }
}
