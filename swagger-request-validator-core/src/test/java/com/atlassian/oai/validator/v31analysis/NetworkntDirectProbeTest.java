package com.atlassian.oai.validator.v31analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonMetaSchema;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import com.networknt.schema.oas.OpenApi31;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Probes networknt directly (bypassing this validator's transformer
 * pipeline) to check what it can and can't enforce on its own.
 *
 * Used to isolate whether observed gaps live in:
 *   (a) networknt itself
 *   (b) the validator's transformer pipeline / dispatch logic
 */
public class NetworkntDirectProbeTest {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkntDirectProbeTest.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonSchemaFactory FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    private static int validate(final String schemaJson, final String input) throws Exception {
        final JsonSchema schema = FACTORY.getSchema(schemaJson);
        final JsonNode node = MAPPER.readTree(input);
        final Set<ValidationMessage> errs = schema.validate(node);
        errs.forEach(e -> LOG.error("  - {}", e));
        return errs.size();
    }

    /**
     * Note: in networknt 1.5.x, {@code SchemaValidatorsConfig} is immutable
     * after {@code builder().build()}, and the documented
     * {@code enableUnevaluatedAnalysis()} mutator throws
     * {@code UnsupportedOperationException}. The Builder also doesn't expose
     * the flag. Empirically, unevaluated analysis is automatic when the schema
     * is a valid 2020-12 schema with {@code unevaluatedProperties} declared
     * and the meta-schema chain supports it — see the Deep probe.
     */
    private static int validateWithUnevaluated(final String schemaJson, final String input) throws Exception {
        return validate(schemaJson, input);
    }

    @Test
    void unevaluatedProperties_extra_is_rejected_by_networknt() throws Exception {
        final String schema = "{"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
                + "  \"allOf\": ["
                + "    {\"type\": \"object\", \"properties\": {\"a\": {\"type\": \"string\"}}, \"additionalProperties\": true},"
                + "    {\"type\": \"object\", \"properties\": {\"b\": {\"type\": \"string\"}}, \"additionalProperties\": true}"
                + "  ],"
                + "  \"unevaluatedProperties\": false"
                + "}";

        // Default config — unevaluated analysis is OFF
        final int errsBadDefault = validate(schema, "{\"a\":\"x\",\"b\":\"y\",\"c\":\"z\"}");
        LOG.error("[direct/unevaluated extra c, default cfg] errors: {}", errsBadDefault);

        // With unevaluated analysis explicitly enabled
        final int errsBadEnabled = validateWithUnevaluated(schema, "{\"a\":\"x\",\"b\":\"y\",\"c\":\"z\"}");
        LOG.error("[direct/unevaluated extra c, ENABLED] errors: {}", errsBadEnabled);

        final int errsGoodEnabled = validateWithUnevaluated(schema, "{\"a\":\"x\",\"b\":\"y\"}");
        LOG.error("[direct/unevaluated only a,b, ENABLED] errors: {}", errsGoodEnabled);
    }

    @Test
    void const_without_type_is_handled_by_networknt() throws Exception {
        final String schema = "{"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
                + "  \"type\": \"object\","
                + "  \"properties\": {"
                + "    \"kind\": {\"const\": \"order.created\"}"
                + "  }"
                + "}";

        final int errsMatch = validate(schema, "{\"kind\":\"order.created\"}");
        LOG.error("[direct/const matching] errors: {}", errsMatch);

        final int errsMismatch = validate(schema, "{\"kind\":\"order.deleted\"}");
        LOG.error("[direct/const mismatch] errors: {}", errsMismatch);
    }

    @Test
    void boolean_schema_true_works_directly() throws Exception {
        final int errs = validate("true", "{\"anything\":[1,2,3]}");
        LOG.error("[direct/schema:true] errors: {}", errs);
    }

    @Test
    void boolean_schema_false_works_directly() throws Exception {
        final int errs = validate("false", "{\"anything\":[1,2,3]}");
        LOG.error("[direct/schema:false] errors: {}", errs);
    }

    @Test
    void discriminator_with_const_in_branch_works_directly() throws Exception {
        // Standard JSON Schema 2020-12 way to do discriminated unions
        final String schema = "{"
                + "  \"$schema\": \"https://json-schema.org/draft/2020-12/schema\","
                + "  \"oneOf\": ["
                + "    {\"type\": \"object\", \"required\": [\"kind\",\"radius\"], "
                + "     \"properties\": {\"kind\":{\"const\":\"circle\"},\"radius\":{\"type\":\"number\"}}},"
                + "    {\"type\": \"object\", \"required\": [\"kind\",\"side\"], "
                + "     \"properties\": {\"kind\":{\"const\":\"square\"},\"side\":{\"type\":\"number\"}}}"
                + "  ]"
                + "}";

        final int errsCircle = validate(schema, "{\"kind\":\"circle\",\"radius\":5}");
        LOG.error("[direct/oneOf+const circle] errors: {}", errsCircle);

        final int errsBad = validate(schema, "{\"kind\":\"circle\"}");
        LOG.error("[direct/oneOf+const circle missing radius] errors: {}", errsBad);
    }

    @Test
    void discriminator_with_mapping_via_oas31_metaschema() throws Exception {
        // OAS-style discriminator using the OAS 3.1 meta-schema (mirrors what
        // SchemaValidator does). The discriminator keyword is NOT JSON Schema
        // standard, so it requires the OAS dialect + discriminatorKeywordEnabled.
        final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> {
            builder.metaSchema(JsonMetaSchema.builder(OpenApi31.getInstance()).build())
                   .defaultMetaSchemaIri(OpenApi31.getInstance().getIri());
        });

        final String schema = "{"
                + "  \"oneOf\": ["
                + "    {\"$ref\":\"#/$defs/Circle\"},"
                + "    {\"$ref\":\"#/$defs/Square\"}"
                + "  ],"
                + "  \"discriminator\": {"
                + "    \"propertyName\": \"kind\","
                + "    \"mapping\": {"
                + "      \"circle\": \"#/$defs/Circle\","
                + "      \"square\": \"#/$defs/Square\""
                + "    }"
                + "  },"
                + "  \"$defs\": {"
                + "    \"Circle\": {\"type\":\"object\",\"required\":[\"kind\",\"radius\"],"
                + "      \"properties\":{\"kind\":{\"type\":\"string\"},\"radius\":{\"type\":\"number\"}}},"
                + "    \"Square\": {\"type\":\"object\",\"required\":[\"kind\",\"side\"],"
                + "      \"properties\":{\"kind\":{\"type\":\"string\"},\"side\":{\"type\":\"number\"}}}"
                + "  }"
                + "}";

        final SchemaValidatorsConfig cfg = SchemaValidatorsConfig.builder()
                .discriminatorKeywordEnabled(true).build();

        final JsonSchema s = factory.getSchema(schema, cfg);
        final JsonNode node = MAPPER.readTree("{\"kind\":\"circle\",\"radius\":5}");
        final Set<ValidationMessage> errs = s.validate(node);
        errs.forEach(e -> LOG.error("    - {}", e));
        LOG.error("[direct/discriminator+mapping circle] errors: {}", errs.size());
    }
}
