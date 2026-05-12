package com.atlassian.oai.validator.v31analysis;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Probe to understand exactly what swagger-parser does (or refuses to do)
 * with boolean schemas at various positions.
 */
public class BooleanSchemaProbeTest {

    private static final Logger LOG = LoggerFactory.getLogger(BooleanSchemaProbeTest.class);

    private static void probe(final String label, final String spec) {
        final ParseOptions opts = new ParseOptions();
        opts.setResolve(true);
        opts.setResolveFully(true);
        opts.setExplicitObjectSchema(false);

        final SwaggerParseResult result = new OpenAPIParser().readContents(spec, null, opts);
        final OpenAPI api = result.getOpenAPI();
        final java.util.List<String> msgs = result.getMessages();
        LOG.error("[{}] api?={} messages={}", label, api != null, msgs == null ? "null" : msgs);
    }

    @Test
    void schema_true_at_root() {
        probe("schema:true at root", "openapi: 3.1.0\n"
                + "info: {title: t, version: '1'}\n"
                + "paths:\n"
                + "  /any:\n"
                + "    post:\n"
                + "      requestBody:\n"
                + "        required: true\n"
                + "        content:\n"
                + "          application/json:\n"
                + "            schema: true\n"
                + "      responses: { '200': {description: ok} }\n");
    }

    @Test
    void schema_false_at_root() {
        probe("schema:false at root", "openapi: 3.1.0\n"
                + "info: {title: t, version: '1'}\n"
                + "paths:\n"
                + "  /none:\n"
                + "    post:\n"
                + "      requestBody:\n"
                + "        required: true\n"
                + "        content:\n"
                + "          application/json:\n"
                + "            schema: false\n"
                + "      responses: { '200': {description: ok} }\n");
    }

    @Test
    void empty_schema_at_root() {
        // {} is valid JSON Schema and means "match anything", same as `true`
        probe("schema:{} at root", "openapi: 3.1.0\n"
                + "info: {title: t, version: '1'}\n"
                + "paths:\n"
                + "  /any:\n"
                + "    post:\n"
                + "      requestBody:\n"
                + "        required: true\n"
                + "        content:\n"
                + "          application/json:\n"
                + "            schema: {}\n"
                + "      responses: { '200': {description: ok} }\n");
    }

    @Test
    void nested_property_true() {
        probe("nested property:true", "openapi: 3.1.0\n"
                + "info: {title: t, version: '1'}\n"
                + "paths:\n"
                + "  /any:\n"
                + "    post:\n"
                + "      requestBody:\n"
                + "        required: true\n"
                + "        content:\n"
                + "          application/json:\n"
                + "            schema:\n"
                + "              type: object\n"
                + "              properties:\n"
                + "                anything: true\n"
                + "      responses: { '200': {description: ok} }\n");
    }
}
