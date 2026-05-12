package com.atlassian.oai.validator.v31analysis;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.transform.DiscriminatorMappingTransformer;
import com.atlassian.oai.validator.schema.transform.SchemaTransformationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.swagger.v3.core.util.Json31;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Probe to understand exactly what schema is being sent to networknt for
 * the discriminator+mapping case.
 */
public class DiscriminatorProbeTest {

    private static final Logger LOG = LoggerFactory.getLogger(DiscriminatorProbeTest.class);

    private static final String SPEC =
            "openapi: 3.1.0\n"
          + "info: {title: t, version: '1'}\n"
          + "paths:\n"
          + "  /shapes:\n"
          + "    post:\n"
          + "      requestBody:\n"
          + "        required: true\n"
          + "        content:\n"
          + "          application/json:\n"
          + "            schema:\n"
          + "              oneOf:\n"
          + "                - $ref: '#/components/schemas/Circle'\n"
          + "                - $ref: '#/components/schemas/Square'\n"
          + "              discriminator:\n"
          + "                propertyName: kind\n"
          + "                mapping:\n"
          + "                  circle: '#/components/schemas/Circle'\n"
          + "                  square: '#/components/schemas/Square'\n"
          + "      responses: { '200': {description: ok} }\n"
          + "components:\n"
          + "  schemas:\n"
          + "    Circle:\n"
          + "      type: object\n"
          + "      required: [kind, radius]\n"
          + "      properties:\n"
          + "        kind: { type: string }\n"
          + "        radius: { type: number }\n"
          + "    Square:\n"
          + "      type: object\n"
          + "      required: [kind, side]\n"
          + "      properties:\n"
          + "        kind: { type: string }\n"
          + "        side: { type: number }\n";

    @Test
    void show_what_swagger_parser_produces() throws Exception {
        final ParseOptions opts = new ParseOptions();
        opts.setResolve(true);
        opts.setResolveFully(true);
        opts.setResolveCombinators(false);
        opts.setExplicitObjectSchema(false);

        final OpenAPI api = new OpenAPIParser().readContents(SPEC, null, opts).getOpenAPI();
        final ObjectMapper mapper = Json31.mapper().copy().enable(SerializationFeature.INDENT_OUTPUT);
        LOG.error("=== parsed POST /shapes requestBody schema ===\n{}",
                mapper.writeValueAsString(
                        api.getPaths().get("/shapes").getPost().getRequestBody()
                           .getContent().get("application/json").getSchema()));
        LOG.error("=== parsed components.schemas.Circle ===\n{}",
                mapper.writeValueAsString(api.getComponents().getSchemas().get("Circle")));
    }

    @Test
    void run_validator_and_see_what_happens() {
        final OpenApiInteractionValidator v =
                OpenApiInteractionValidator.createForInlineApiSpecification(SPEC).build();
        final ValidationReport report = v.validateRequest(
                SimpleRequest.Builder.post("/shapes")
                        .withContentType("application/json")
                        .withBody("{\"kind\": \"circle\", \"radius\": 5}")
                        .build());
        report.getMessages().forEach(m -> LOG.error("  - {}: {}", m.getKey(), m.getMessage()));
        LOG.error("[discriminator with mapping circle] errors: {}", report.getMessages().size());
    }

    @Test
    void show_what_transformer_produces() throws Exception {
        // Run the schema through the new DiscriminatorMappingTransformer and dump the result.
        final ParseOptions opts = new ParseOptions();
        opts.setResolve(true);
        opts.setResolveFully(true);
        opts.setResolveCombinators(false);
        opts.setExplicitObjectSchema(false);
        final OpenAPI api = new OpenAPIParser().readContents(SPEC, null, opts).getOpenAPI();

        final ObjectMapper mapper = Json31.mapper().copy().enable(SerializationFeature.INDENT_OUTPUT);

        // Convert request body schema to JsonNode the way SchemaValidator does.
        final JsonNode bodySchemaNode = mapper.valueToTree(
                api.getPaths().get("/shapes").getPost().getRequestBody()
                   .getContent().get("application/json").getSchema());

        // Build a definitions tree similar to SchemaValidator's wiring.
        final JsonNode definitionsRoot = mapper.createObjectNode();

        final SchemaTransformationContext ctx = SchemaTransformationContext.create()
                .forRequest(true)
                .withAdditionalPropertiesValidation(false)
                .withDefinitions(definitionsRoot)
                .withIsOpenApi30(false)
                .build();
        DiscriminatorMappingTransformer.getInstance().apply(bodySchemaNode, ctx);

        LOG.error("=== schema AFTER DiscriminatorMappingTransformer ===\n{}",
                mapper.writeValueAsString(bodySchemaNode));
    }
}
