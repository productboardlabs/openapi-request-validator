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
 * Deep probe for the unevaluatedProperties failure mode. Tries every
 * combination of meta-schema and schema shape to find what makes it work.
 */
public class UnevaluatedDeepProbeTest {

    private static final Logger LOG = LoggerFactory.getLogger(UnevaluatedDeepProbeTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static int run(final String label, final JsonSchemaFactory factory,
                           final SchemaValidatorsConfig cfg,
                           final String schemaJson, final String input) throws Exception {
        final JsonSchema schema = cfg == null ? factory.getSchema(schemaJson) : factory.getSchema(schemaJson, cfg);
        final JsonNode node = MAPPER.readTree(input);
        final Set<ValidationMessage> errs = schema.validate(node);
        LOG.error("[{}] errors: {}", label, errs.size());
        errs.forEach(e -> LOG.error("    - {}", e));
        return errs.size();
    }

    @Test
    void allOf_with_unevaluated_default_metaschema() throws Exception {
        final String schema = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"allOf\":["
                + "  {\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}},"
                + "  {\"type\":\"object\",\"properties\":{\"b\":{\"type\":\"string\"}}}"
                + "],"
                + "\"unevaluatedProperties\":false"
                + "}";
        final String bad = "{\"a\":\"x\",\"b\":\"y\",\"c\":\"z\"}";

        run("default 2020-12", JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012), null, schema, bad);
    }

    @Test
    void allOf_with_unevaluated_oas31_metaschema_NO_TYPE_SCHEMA_AT_TOP() throws Exception {
        // Reproduce what SchemaValidator does — uses OpenApi31 meta-schema as default
        final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> {
            builder.metaSchema(JsonMetaSchema.builder(OpenApi31.getInstance()).build())
                   .defaultMetaSchemaIri(OpenApi31.getInstance().getIri());
        });

        final String schema = "{"
                + "\"allOf\":["
                + "  {\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}},"
                + "  {\"type\":\"object\",\"properties\":{\"b\":{\"type\":\"string\"}}}"
                + "],"
                + "\"unevaluatedProperties\":false"
                + "}";
        final String bad = "{\"a\":\"x\",\"b\":\"y\",\"c\":\"z\"}";

        run("OpenApi31 metaschema, no top type", factory, null, schema, bad);
    }

    @Test
    void allOf_with_additionalProperties_true_BREAKS_unevaluated() throws Exception {
        // Reproduces the SRV behaviour: when each allOf branch has
        // additionalProperties:true, networknt treats all properties as evaluated
        // and unevaluatedProperties:false has nothing left to flag.
        final String schema = "{"
                + "\"$schema\":\"https://json-schema.org/draft/2020-12/schema\","
                + "\"allOf\":["
                + "  {\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},\"additionalProperties\":true},"
                + "  {\"type\":\"object\",\"properties\":{\"b\":{\"type\":\"string\"}},\"additionalProperties\":true}"
                + "],"
                + "\"unevaluatedProperties\":false"
                + "}";
        final String bad = "{\"a\":\"x\",\"b\":\"y\",\"c\":\"z\"}";

        run("default 2020-12 with additionalProperties:true on branches", JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012), null, schema, bad);
    }

    @Test
    void allOf_with_unevaluated_object_at_top_OAS31_metaschema() throws Exception {
        // What if we add type:object at the top level?
        final JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012, builder -> {
            builder.metaSchema(JsonMetaSchema.builder(OpenApi31.getInstance()).build())
                   .defaultMetaSchemaIri(OpenApi31.getInstance().getIri());
        });

        final String schema = "{"
                + "\"type\":\"object\","
                + "\"allOf\":["
                + "  {\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}}},"
                + "  {\"type\":\"object\",\"properties\":{\"b\":{\"type\":\"string\"}}}"
                + "],"
                + "\"unevaluatedProperties\":false"
                + "}";
        final String bad = "{\"a\":\"x\",\"b\":\"y\",\"c\":\"z\"}";

        run("OpenApi31 metaschema, type:object at top", factory, null, schema, bad);
    }
}
