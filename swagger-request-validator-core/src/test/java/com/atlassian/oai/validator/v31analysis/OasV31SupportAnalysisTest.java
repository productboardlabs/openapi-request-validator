package com.atlassian.oai.validator.v31analysis;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Empirical analysis of OpenAPI 3.1 feature coverage in this validator.
 *
 * Each nested class targets one OAS 3.1 feature area. Each test documents whether
 * the feature works today, partially works, or is unsupported. The suite is intended
 * to be read alongside the analysis report — it is not a regression suite that should
 * gate releases. A test that "passes" here may simply mean the validator silently
 * accepted a spec or request it should have rejected (we make that explicit per test).
 *
 * Conventions:
 *  - {@code assertPasses} = validator returned no errors
 *  - {@code assertFails}  = validator returned at least one error
 *  - tests prefixed with {@code documentsGap_*} record a known limitation
 *  - tests marked {@code @Disabled} cannot be exercised at all (e.g. parser rejects spec)
 */
public class OasV31SupportAnalysisTest {

    private static final Logger LOG = LoggerFactory.getLogger(OasV31SupportAnalysisTest.class);

    // ============================================================================
    // Helpers
    // ============================================================================

    private static OpenApiInteractionValidator validatorFor(final String spec) {
        return OpenApiInteractionValidator.createForInlineApiSpecification(spec).build();
    }

    private static void assertPasses(final ValidationReport report) {
        assertFalse(report.hasErrors(),
                () -> "Expected no errors but got: " + formatMessages(report));
    }

    private static void assertFails(final ValidationReport report) {
        assertTrue(report.hasErrors(),
                "Expected validation to fail but it passed");
    }

    private static String formatMessages(final ValidationReport report) {
        if (report.getMessages().isEmpty()) {
            return "<empty>";
        }
        final StringBuilder sb = new StringBuilder();
        report.getMessages().forEach(m -> sb.append("\n  [")
                .append(m.getLevel()).append("] ")
                .append(m.getKey()).append(": ")
                .append(m.getMessage()));
        return sb.toString();
    }

    private static Request jsonPost(final String path, final String body) {
        return SimpleRequest.Builder
                .post(path)
                .withContentType("application/json")
                .withBody(body)
                .build();
    }

    private static Request jsonGet(final String path) {
        return SimpleRequest.Builder.get(path).build();
    }

    // ============================================================================
    // 1. Basic spec parsing — does swagger-parser accept openapi: 3.1.x at all?
    // ============================================================================

    @Nested
    @DisplayName("1. Spec Parsing")
    class SpecParsing {

        @Test
        @DisplayName("OAS 3.1.0 spec parses without error")
        void parses_v310_spec() {
            final String spec =
                    "openapi: 3.1.0\n"
                  + "info:\n"
                  + "  title: t\n"
                  + "  version: '1'\n"
                  + "paths: {}\n";
            assertNotNull(validatorFor(spec));
        }

        @Test
        @DisplayName("OAS 3.1.1 spec parses without error")
        void parses_v311_spec() {
            final String spec =
                    "openapi: 3.1.1\n"
                  + "info:\n"
                  + "  title: t\n"
                  + "  version: '1'\n"
                  + "paths: {}\n";
            assertNotNull(validatorFor(spec));
        }

        @Test
        @DisplayName("Parsed spec exposes specVersion=V31 to downstream code")
        void exposes_v31_spec_version() {
            final String spec =
                    "openapi: 3.1.0\n"
                  + "info:\n"
                  + "  title: t\n"
                  + "  version: '1'\n"
                  + "paths: {}\n";
            final SwaggerParseResult result = new OpenAPIV3Parser().readContents(spec, null, null);
            final OpenAPI api = result.getOpenAPI();
            assertEquals("V31", api.getSpecVersion().toString());
        }
    }

    // ============================================================================
    // 2. Type unions (the headline JSON Schema 2020-12 / OAS 3.1 change)
    //    type: ["string", "null"] etc.
    // ============================================================================

    @Nested
    @DisplayName("2. Type Unions (mixed-type arrays)")
    class TypeUnions {

        private final String spec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths:\n"
              + "  /items:\n"
              + "    post:\n"
              + "      requestBody:\n"
              + "        required: true\n"
              + "        content:\n"
              + "          application/json:\n"
              + "            schema:\n"
              + "              type: object\n"
              + "              properties:\n"
              + "                id:    { type: [integer, string] }\n"
              + "                tag:   { type: [string, 'null'] }\n"
              + "                qty:   { type: [number, 'null'], minimum: 0 }\n"
              + "      responses: { '200': {description: ok} }\n";

        @Test
        @DisplayName("integer matches type:[integer,string]")
        void integer_matches_int_or_string() {
            assertPasses(validatorFor(spec).validateRequest(
                    jsonPost("/items", "{\"id\": 1, \"tag\": \"x\", \"qty\": 1}")));
        }

        @Test
        @DisplayName("string matches type:[integer,string]")
        void string_matches_int_or_string() {
            assertPasses(validatorFor(spec).validateRequest(
                    jsonPost("/items", "{\"id\": \"x\", \"tag\": \"x\", \"qty\": 1}")));
        }

        @Test
        @DisplayName("boolean is rejected by type:[integer,string]")
        void boolean_rejected_by_int_or_string() {
            assertFails(validatorFor(spec).validateRequest(
                    jsonPost("/items", "{\"id\": true, \"tag\": \"x\", \"qty\": 1}")));
        }

        @Test
        @DisplayName("null matches type:[string,'null']")
        void null_matches_string_or_null() {
            assertPasses(validatorFor(spec).validateRequest(
                    jsonPost("/items", "{\"id\": 1, \"tag\": null, \"qty\": 1}")));
        }

        @Test
        @DisplayName("constraints (minimum) apply to type:[number,'null']")
        void constraints_apply_within_type_union() {
            assertFails(validatorFor(spec).validateRequest(
                    jsonPost("/items", "{\"id\": 1, \"tag\": \"x\", \"qty\": -1}")));
        }

        @Test
        @DisplayName("null value rejected when 'null' not in union")
        void non_nullable_rejects_null() {
            assertFails(validatorFor(spec).validateRequest(
                    jsonPost("/items", "{\"id\": null, \"tag\": \"x\", \"qty\": 1}")));
        }
    }

    // ============================================================================
    // 3. nullable: true (OAS 3.0 keyword) — does it still work in 3.1?
    //    Per spec, 3.1 removed nullable in favour of type unions, but tooling
    //    often tolerates it for migration ease.
    // ============================================================================

    @Nested
    @DisplayName("3. Legacy nullable: true keyword in 3.1")
    class LegacyNullable {

        private final String spec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths:\n"
              + "  /items:\n"
              + "    post:\n"
              + "      requestBody:\n"
              + "        required: true\n"
              + "        content:\n"
              + "          application/json:\n"
              + "            schema:\n"
              + "              type: object\n"
              + "              properties:\n"
              + "                tag: { type: string, nullable: true }\n"
              + "      responses: { '200': {description: ok} }\n";

        @Test
        @DisplayName("documents whether nullable:true on string accepts null in 3.1")
        void documents_legacy_nullable_behaviour() {
            // No assertion on outcome — this records what the validator does today.
            final ValidationReport report = validatorFor(spec).validateRequest(
                    jsonPost("/items", "{\"tag\": null}"));
            LOG.error("[3.1 + nullable:true + null] errors? " + report.hasErrors()
                    + formatMessages(report));
        }
    }

    // ============================================================================
    // 4. const keyword (JSON Schema 2019-09+ — required by 3.1)
    // ============================================================================

    @Nested
    @DisplayName("4. const keyword")
    class ConstKeyword {

        // Schema declares a typed property with const value
        private final String typedSpec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths:\n"
              + "  /events:\n"
              + "    post:\n"
              + "      requestBody:\n"
              + "        required: true\n"
              + "        content:\n"
              + "          application/json:\n"
              + "            schema:\n"
              + "              type: object\n"
              + "              properties:\n"
              + "                kind: { type: string, const: 'order.created' }\n"
              + "      responses: { '200': {description: ok} }\n";

        // Schema declares a const value WITHOUT explicit type (legitimate per JSON Schema)
        private final String untypedSpec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths:\n"
              + "  /events:\n"
              + "    post:\n"
              + "      requestBody:\n"
              + "        required: true\n"
              + "        content:\n"
              + "          application/json:\n"
              + "            schema:\n"
              + "              type: object\n"
              + "              properties:\n"
              + "                kind: { const: 'order.created' }\n"
              + "      responses: { '200': {description: ok} }\n";

        @Test
        @DisplayName("typed property + const: matching value passes")
        void typed_matching_const_passes() {
            assertPasses(validatorFor(typedSpec).validateRequest(
                    jsonPost("/events", "{\"kind\": \"order.created\"}")));
        }

        @Test
        @DisplayName("typed property + const: non-matching value fails")
        void typed_non_matching_const_fails() {
            assertFails(validatorFor(typedSpec).validateRequest(
                    jsonPost("/events", "{\"kind\": \"order.deleted\"}")));
        }

        @Test
        @DisplayName("documents: untyped const property behaviour")
        void documents_untyped_const_behaviour() {
            final ValidationReport report = validatorFor(untypedSpec).validateRequest(
                    jsonPost("/events", "{\"kind\": \"order.created\"}"));
            LOG.error("[const without explicit type] errors? {}{}",
                    report.hasErrors(), formatMessages(report));
        }
    }

    // ============================================================================
    // 5. exclusiveMinimum / exclusiveMaximum as numbers (3.1)
    //    In 3.0 these were booleans; in 3.1 they take a numeric value.
    // ============================================================================

    @Nested
    @DisplayName("5. exclusiveMinimum / exclusiveMaximum as numbers")
    class NumericExclusiveBounds {

        private final String spec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths:\n"
              + "  /pct:\n"
              + "    post:\n"
              + "      requestBody:\n"
              + "        required: true\n"
              + "        content:\n"
              + "          application/json:\n"
              + "            schema:\n"
              + "              type: object\n"
              + "              properties:\n"
              + "                value: { type: number, exclusiveMinimum: 0, exclusiveMaximum: 100 }\n"
              + "      responses: { '200': {description: ok} }\n";

        @Test
        @DisplayName("value equal to exclusiveMinimum is rejected")
        void value_equal_to_exclusiveMin_fails() {
            assertFails(validatorFor(spec).validateRequest(
                    jsonPost("/pct", "{\"value\": 0}")));
        }

        @Test
        @DisplayName("value just above exclusiveMinimum passes")
        void value_above_exclusiveMin_passes() {
            assertPasses(validatorFor(spec).validateRequest(
                    jsonPost("/pct", "{\"value\": 0.0001}")));
        }

        @Test
        @DisplayName("value equal to exclusiveMaximum is rejected")
        void value_equal_to_exclusiveMax_fails() {
            assertFails(validatorFor(spec).validateRequest(
                    jsonPost("/pct", "{\"value\": 100}")));
        }
    }

    // ============================================================================
    // 6. examples (plural array) at schema level (3.1) vs example (singular, 3.0)
    //    Validator should not enforce examples — they are documentation. We just
    //    confirm the spec parses with the new keyword present.
    // ============================================================================

    @Nested
    @DisplayName("6. examples (plural) at schema level")
    class PluralExamples {

        @Test
        @DisplayName("spec with schema-level examples array parses cleanly")
        void parses_schema_examples_array() {
            final String spec =
                    "openapi: 3.1.0\n"
                  + "info: {title: t, version: '1'}\n"
                  + "paths:\n"
                  + "  /n:\n"
                  + "    post:\n"
                  + "      requestBody:\n"
                  + "        required: true\n"
                  + "        content:\n"
                  + "          application/json:\n"
                  + "            schema:\n"
                  + "              type: integer\n"
                  + "              examples: [1, 2, 3]\n"
                  + "      responses: { '200': {description: ok} }\n";
            // Just instantiate — parser must not blow up
            assertNotNull(validatorFor(spec));
        }
    }

    // ============================================================================
    // 7. if / then / else conditional schemas
    // ============================================================================

    @Nested
    @DisplayName("7. if / then / else")
    class ConditionalSchemas {

        private final String spec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths:\n"
              + "  /addr:\n"
              + "    post:\n"
              + "      requestBody:\n"
              + "        required: true\n"
              + "        content:\n"
              + "          application/json:\n"
              + "            schema:\n"
              + "              type: object\n"
              + "              properties:\n"
              + "                country: { type: string }\n"
              + "                postcode: { type: string }\n"
              + "              if:\n"
              + "                properties:\n"
              + "                  country: { const: 'US' }\n"
              + "              then:\n"
              + "                properties:\n"
              + "                  postcode: { pattern: '^[0-9]{5}$' }\n"
              + "      responses: { '200': {description: ok} }\n";

        @Test
        @DisplayName("US country with valid 5-digit postcode passes")
        void us_with_valid_postcode_passes() {
            assertPasses(validatorFor(spec).validateRequest(
                    jsonPost("/addr", "{\"country\": \"US\", \"postcode\": \"12345\"}")));
        }

        @Test
        @DisplayName("US country with invalid postcode fails")
        void us_with_invalid_postcode_fails() {
            assertFails(validatorFor(spec).validateRequest(
                    jsonPost("/addr", "{\"country\": \"US\", \"postcode\": \"abcde\"}")));
        }

        @Test
        @DisplayName("non-US country with arbitrary postcode passes")
        void non_us_with_any_postcode_passes() {
            assertPasses(validatorFor(spec).validateRequest(
                    jsonPost("/addr", "{\"country\": \"AU\", \"postcode\": \"abcde\"}")));
        }
    }

    // ============================================================================
    // 8. dependentRequired and dependentSchemas (replace 3.0 dependencies keyword)
    // ============================================================================

    @Nested
    @DisplayName("8. dependentRequired / dependentSchemas")
    class Dependent {

        private final String dependentRequiredSpec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths:\n"
              + "  /pay:\n"
              + "    post:\n"
              + "      requestBody:\n"
              + "        required: true\n"
              + "        content:\n"
              + "          application/json:\n"
              + "            schema:\n"
              + "              type: object\n"
              + "              properties:\n"
              + "                creditCard: { type: string }\n"
              + "                cvv: { type: string }\n"
              + "              dependentRequired:\n"
              + "                creditCard: [cvv]\n"
              + "      responses: { '200': {description: ok} }\n";

        @Test
        @DisplayName("creditCard without cvv is rejected")
        void missing_dependent_required_fails() {
            assertFails(validatorFor(dependentRequiredSpec).validateRequest(
                    jsonPost("/pay", "{\"creditCard\": \"4242\"}")));
        }

        @Test
        @DisplayName("creditCard with cvv passes")
        void all_dependent_required_present_passes() {
            assertPasses(validatorFor(dependentRequiredSpec).validateRequest(
                    jsonPost("/pay", "{\"creditCard\": \"4242\", \"cvv\": \"123\"}")));
        }

        @Test
        @DisplayName("absent trigger property bypasses dependentRequired")
        void absent_trigger_passes() {
            assertPasses(validatorFor(dependentRequiredSpec).validateRequest(
                    jsonPost("/pay", "{}")));
        }
    }

    // ============================================================================
    // 9. prefixItems (replaces 3.0 / Draft-4 tuple-style 'items' arrays)
    // ============================================================================

    @Nested
    @DisplayName("9. prefixItems (tuple validation)")
    class PrefixItems {

        private final String spec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths:\n"
              + "  /coords:\n"
              + "    post:\n"
              + "      requestBody:\n"
              + "        required: true\n"
              + "        content:\n"
              + "          application/json:\n"
              + "            schema:\n"
              + "              type: array\n"
              + "              prefixItems:\n"
              + "                - { type: number }\n"
              + "                - { type: number }\n"
              + "                - { type: string }\n"
              + "              items: false\n"
              + "      responses: { '200': {description: ok} }\n";

        @Test
        @DisplayName("matching tuple passes")
        void matching_tuple_passes() {
            assertPasses(validatorFor(spec).validateRequest(
                    jsonPost("/coords", "[1.0, 2.0, \"label\"]")));
        }

        @Test
        @DisplayName("wrong type at position fails")
        void wrong_type_at_position_fails() {
            assertFails(validatorFor(spec).validateRequest(
                    jsonPost("/coords", "[1.0, \"two\", \"label\"]")));
        }

        @Test
        @DisplayName("extra items rejected when items:false")
        void extra_items_rejected() {
            assertFails(validatorFor(spec).validateRequest(
                    jsonPost("/coords", "[1.0, 2.0, \"label\", \"extra\"]")));
        }
    }

    // ============================================================================
    // 10. unevaluatedProperties (JSON Schema 2019-09+, supported in 3.1)
    // ============================================================================

    @Nested
    @DisplayName("10. unevaluatedProperties")
    class UnevaluatedProperties {

        // Note: the validator auto-injects additionalProperties:false on object schemas
        // by default (AdditionalPropertiesInjectionTransformer). To exercise
        // unevaluatedProperties cleanly we must opt out of additional-properties
        // validation via withAdditionalPropertiesValidation(false), or use a top-level
        // type:object with explicit additionalProperties:true on each allOf branch.
        private final String spec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths:\n"
              + "  /thing:\n"
              + "    post:\n"
              + "      requestBody:\n"
              + "        required: true\n"
              + "        content:\n"
              + "          application/json:\n"
              + "            schema:\n"
              + "              allOf:\n"
              + "                - type: object\n"
              + "                  properties: { a: { type: string } }\n"
              + "                  additionalProperties: true\n"
              + "                - type: object\n"
              + "                  properties: { b: { type: string } }\n"
              + "                  additionalProperties: true\n"
              + "              unevaluatedProperties: false\n"
              + "      responses: { '200': {description: ok} }\n";

        private OpenApiInteractionValidator validator() {
            // Disable additionalProperties auto-injection so unevaluatedProperties
            // is the only constraint enforcing closed-schema behaviour.
            // The validator wires this via the LevelResolver — ignoring the
            // additionalProperties message-key skips the auto-injection.
            return OpenApiInteractionValidator.createForInlineApiSpecification(spec)
                    .withLevelResolver(LevelResolver.create()
                            .withLevel("validation.schema.additionalProperties",
                                    ValidationReport.Level.IGNORE)
                            .build())
                    .build();
        }

        @Test
        @DisplayName("documents: with auto-injection off, declared properties pass")
        void documents_declared_only_with_injection_off() {
            final ValidationReport report = validator().validateRequest(
                    jsonPost("/thing", "{\"a\": \"x\", \"b\": \"y\"}"));
            LOG.error("[unevaluated: declared only] errors? {}{}",
                    report.hasErrors(), formatMessages(report));
        }

        @Test
        @DisplayName("documents: whether unevaluatedProperties is actually enforced when auto-injection is off")
        void documents_unevaluated_with_injection_off() {
            final ValidationReport report = validator().validateRequest(
                    jsonPost("/thing", "{\"a\": \"x\", \"b\": \"y\", \"c\": \"z\"}"));
            LOG.error("[unevaluated: extra c, injection off] errors? {} (expected: true){}",
                    report.hasErrors(), formatMessages(report));
        }

        @Test
        @DisplayName("documents: default behaviour — additionalProperties:false auto-injected, conflicts with unevaluatedProperties")
        void documents_default_behaviour_with_auto_injection() {
            final OpenApiInteractionValidator v =
                    OpenApiInteractionValidator.createForInlineApiSpecification(spec).build();
            final ValidationReport report = v.validateRequest(
                    jsonPost("/thing", "{\"a\": \"x\", \"b\": \"y\"}"));
            LOG.error("[unevaluated + additionalProperties auto-injected, valid payload] errors? {}{}",
                    report.hasErrors(), formatMessages(report));
        }
    }

    // ============================================================================
    // 11. Boolean schemas (true / false as the entire schema)
    // ============================================================================

    @Nested
    @DisplayName("11. Boolean schemas (true/false)")
    class BooleanSchemas {

        @Test
        @DisplayName("documents: schema:true at top level — does the parser even accept it?")
        void documents_true_schema_loadability() {
            final String spec =
                    "openapi: 3.1.0\n"
                  + "info: {title: t, version: '1'}\n"
                  + "paths:\n"
                  + "  /any:\n"
                  + "    post:\n"
                  + "      requestBody:\n"
                  + "        required: true\n"
                  + "        content:\n"
                  + "          application/json:\n"
                  + "            schema: true\n"
                  + "      responses: { '200': {description: ok} }\n";
            try {
                validatorFor(spec);
                LOG.error("[boolean schema:true] PARSE OK");
            } catch (final Exception e) {
                LOG.error("[boolean schema:true] PARSE REJECTED: {}", e.getMessage());
            }
        }

        @Test
        @DisplayName("documents: schema:false at top level — does the parser even accept it?")
        void documents_false_schema_loadability() {
            final String spec =
                    "openapi: 3.1.0\n"
                  + "info: {title: t, version: '1'}\n"
                  + "paths:\n"
                  + "  /none:\n"
                  + "    post:\n"
                  + "      requestBody:\n"
                  + "        required: true\n"
                  + "        content:\n"
                  + "          application/json:\n"
                  + "            schema: false\n"
                  + "      responses: { '200': {description: ok} }\n";
            try {
                validatorFor(spec);
                LOG.error("[boolean schema:false] PARSE OK");
            } catch (final Exception e) {
                LOG.error("[boolean schema:false] PARSE REJECTED: {}", e.getMessage());
            }
        }

        @Test
        @DisplayName("documents: schema:true as nested property schema")
        void documents_nested_true_schema_loadability() {
            final String spec =
                    "openapi: 3.1.0\n"
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
                  + "      responses: { '200': {description: ok} }\n";
            try {
                validatorFor(spec);
                LOG.error("[nested boolean schema:true] PARSE OK");
            } catch (final Exception e) {
                LOG.error("[nested boolean schema:true] PARSE REJECTED: {}", e.getMessage());
            }
        }
    }

    // ============================================================================
    // 12. Top-level webhooks (NEW IN 3.1 — was completely absent in 3.0)
    // ============================================================================

    @Nested
    @DisplayName("12. Top-level webhooks (3.1 NEW)")
    class Webhooks {

        // Webhooks specs have NO paths (or empty paths). They define operations
        // the SERVER will send to clients. Validation should still be possible
        // via something like validateRequest("/webhook-name", ...) or similar.

        private final String spec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths: {}\n"
              + "webhooks:\n"
              + "  newOrder:\n"
              + "    post:\n"
              + "      requestBody:\n"
              + "        required: true\n"
              + "        content:\n"
              + "          application/json:\n"
              + "            schema:\n"
              + "              type: object\n"
              + "              required: [orderId]\n"
              + "              properties:\n"
              + "                orderId: { type: string }\n"
              + "      responses: { '200': {description: ok} }\n";

        @Test
        @DisplayName("documents whether webhook spec is loadable at all")
        void documents_webhook_loadability() {
            try {
                final OpenApiInteractionValidator v = validatorFor(spec);
                assertNotNull(v);
                LOG.error("[webhooks] spec loaded successfully (no validator entry point exists)");
            } catch (final Exception e) {
                LOG.error("[webhooks] spec load FAILED: " + e.getMessage());
                throw e;
            }
        }

        @Test
        @DisplayName("documents that webhook bodies cannot be validated via path lookup")
        void documents_webhook_validation_unsupported() {
            final OpenApiInteractionValidator v = validatorFor(spec);
            // Validator only knows about paths, not webhook keys. Try treating the
            // webhook key as a path and see what happens.
            final ValidationReport report = v.validateRequest(jsonPost("/newOrder",
                    "{\"orderId\": \"ord-1\"}"));
            LOG.error("[webhooks /newOrder] errors? " + report.hasErrors()
                    + formatMessages(report));
            // If this passes, it means there's no operation defined for /newOrder
            // and the validator silently skips. If it fails with "no path matched"
            // that confirms webhooks aren't reachable through the normal API.
        }
    }

    // ============================================================================
    // 13. components.pathItems (NEW IN 3.1 — reusable path items)
    // ============================================================================

    @Nested
    @DisplayName("13. Reusable components.pathItems (3.1 NEW)")
    class ReusablePathItems {

        private final String spec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths:\n"
              + "  /items: { $ref: '#/components/pathItems/itemsPath' }\n"
              + "components:\n"
              + "  pathItems:\n"
              + "    itemsPath:\n"
              + "      get:\n"
              + "        responses:\n"
              + "          '200':\n"
              + "            description: ok\n"
              + "            content:\n"
              + "              application/json:\n"
              + "                schema: { type: object }\n";

        @Test
        @DisplayName("documents whether components.pathItems can be referenced from paths")
        void documents_reusable_pathitem_resolution() {
            try {
                final OpenApiInteractionValidator v = validatorFor(spec);
                final ValidationReport report = v.validateResponse(
                        "/items",
                        Request.Method.GET,
                        SimpleResponse.Builder.ok().withContentType("application/json")
                                .withBody("{}").build());
                LOG.error("[components.pathItems] response validation errors? "
                        + report.hasErrors() + formatMessages(report));
                LOG.error("[components.pathItems] LOADED OK");
            } catch (final Exception e) {
                LOG.error("[components.pathItems] FAILED: " + e.getMessage());
            }
        }
    }

    // ============================================================================
    // 14. info.summary (3.1 metadata addition)
    // ============================================================================

    @Nested
    @DisplayName("14. info.summary metadata field")
    class InfoSummary {

        @Test
        @DisplayName("spec with info.summary parses cleanly")
        void parses_info_summary() {
            final String spec =
                    "openapi: 3.1.0\n"
                  + "info:\n"
                  + "  title: t\n"
                  + "  version: '1'\n"
                  + "  summary: 'A short summary of the API'\n"
                  + "paths: {}\n";
            assertNotNull(validatorFor(spec));
        }
    }

    // ============================================================================
    // 15. info.license.identifier (3.1 — SPDX identifier instead of URL)
    // ============================================================================

    @Nested
    @DisplayName("15. info.license.identifier (SPDX)")
    class LicenseIdentifier {

        @Test
        @DisplayName("spec with info.license.identifier parses cleanly")
        void parses_license_identifier() {
            final String spec =
                    "openapi: 3.1.0\n"
                  + "info:\n"
                  + "  title: t\n"
                  + "  version: '1'\n"
                  + "  license:\n"
                  + "    name: Apache 2.0\n"
                  + "    identifier: Apache-2.0\n"
                  + "paths: {}\n";
            assertNotNull(validatorFor(spec));
        }
    }

    // ============================================================================
    // 16. jsonSchemaDialect (top-level 3.1 declaration)
    // ============================================================================

    @Nested
    @DisplayName("16. jsonSchemaDialect")
    class JsonSchemaDialect {

        @Test
        @DisplayName("spec declaring jsonSchemaDialect parses")
        void parses_with_dialect_declaration() {
            final String spec =
                    "openapi: 3.1.0\n"
                  + "jsonSchemaDialect: 'https://json-schema.org/draft/2020-12/schema'\n"
                  + "info: {title: t, version: '1'}\n"
                  + "paths: {}\n";
            assertNotNull(validatorFor(spec));
        }
    }

    // ============================================================================
    // 17. requestBody on GET / DELETE (allowed in 3.1, was discouraged in 3.0)
    // ============================================================================

    @Nested
    @DisplayName("17. requestBody on GET / DELETE")
    class RequestBodyOnGet {

        private final String spec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths:\n"
              + "  /search:\n"
              + "    get:\n"
              + "      requestBody:\n"
              + "        required: true\n"
              + "        content:\n"
              + "          application/json:\n"
              + "            schema:\n"
              + "              type: object\n"
              + "              required: [q]\n"
              + "              properties: { q: { type: string } }\n"
              + "      responses: { '200': {description: ok} }\n";

        @Test
        @DisplayName("GET with valid requestBody passes")
        void get_with_request_body_validates() {
            final Request request = SimpleRequest.Builder.get("/search")
                    .withContentType("application/json")
                    .withBody("{\"q\": \"hello\"}")
                    .build();
            assertPasses(validatorFor(spec).validateRequest(request));
        }

        @Test
        @DisplayName("GET with invalid requestBody fails")
        void get_with_invalid_body_fails() {
            final Request request = SimpleRequest.Builder.get("/search")
                    .withContentType("application/json")
                    .withBody("{}") // missing required "q"
                    .build();
            assertFails(validatorFor(spec).validateRequest(request));
        }
    }

    // ============================================================================
    // 18. $ref siblings (3.1 allows extra keywords next to $ref)
    // ============================================================================

    @Nested
    @DisplayName("18. $ref with sibling keywords")
    class RefSiblings {

        @Test
        @DisplayName("spec with $ref + sibling description loads")
        void parses_ref_with_siblings() {
            final String spec =
                    "openapi: 3.1.0\n"
                  + "info: {title: t, version: '1'}\n"
                  + "paths:\n"
                  + "  /u:\n"
                  + "    post:\n"
                  + "      requestBody:\n"
                  + "        required: true\n"
                  + "        content:\n"
                  + "          application/json:\n"
                  + "            schema:\n"
                  + "              $ref: '#/components/schemas/User'\n"
                  + "              description: 'A user object'\n"
                  + "      responses: { '200': {description: ok} }\n"
                  + "components:\n"
                  + "  schemas:\n"
                  + "    User:\n"
                  + "      type: object\n"
                  + "      required: [name]\n"
                  + "      properties: { name: { type: string } }\n";
            assertNotNull(validatorFor(spec));
        }
    }

    // ============================================================================
    // 19. contentMediaType / contentEncoding (JSON Schema 2019-09+)
    // ============================================================================

    @Nested
    @DisplayName("19. contentMediaType / contentEncoding")
    class ContentEncoding {

        @Test
        @DisplayName("spec with contentMediaType + contentEncoding loads")
        void parses_content_keywords() {
            final String spec =
                    "openapi: 3.1.0\n"
                  + "info: {title: t, version: '1'}\n"
                  + "paths:\n"
                  + "  /upload:\n"
                  + "    post:\n"
                  + "      requestBody:\n"
                  + "        required: true\n"
                  + "        content:\n"
                  + "          application/json:\n"
                  + "            schema:\n"
                  + "              type: object\n"
                  + "              properties:\n"
                  + "                file:\n"
                  + "                  type: string\n"
                  + "                  contentEncoding: base64\n"
                  + "                  contentMediaType: image/png\n"
                  + "      responses: { '200': {description: ok} }\n";
            assertNotNull(validatorFor(spec));
        }
    }

    // ============================================================================
    // 20. Migration scenario — spec uses both 3.0 and 3.1 idioms simultaneously
    //     (e.g., a developer half-migrated). Document tolerance.
    // ============================================================================

    @Nested
    @DisplayName("20. Mixed 3.0 / 3.1 idioms (migration tolerance)")
    class MixedIdioms {

        private final String spec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths:\n"
              + "  /mixed:\n"
              + "    post:\n"
              + "      requestBody:\n"
              + "        required: true\n"
              + "        content:\n"
              + "          application/json:\n"
              + "            schema:\n"
              + "              type: object\n"
              + "              properties:\n"
              + "                # 3.0-style nullable\n"
              + "                a: { type: string, nullable: true }\n"
              + "                # 3.1-style type union\n"
              + "                b: { type: [integer, 'null'] }\n"
              + "                # 3.0-style example (singular)\n"
              + "                c: { type: string, example: 'x' }\n"
              + "                # 3.1-style examples (plural)\n"
              + "                d: { type: string, examples: ['x', 'y'] }\n"
              + "      responses: { '200': {description: ok} }\n";

        @Test
        @DisplayName("mixed spec loads and validates a clean payload")
        void mixed_idioms_clean_payload() {
            assertPasses(validatorFor(spec).validateRequest(
                    jsonPost("/mixed", "{\"a\": \"x\", \"b\": 1, \"c\": \"y\", \"d\": \"z\"}")));
        }

        @Test
        @DisplayName("documents which nullable form actually accepts null")
        void documents_nullable_in_mixed_spec() {
            final ValidationReport bothNull = validatorFor(spec).validateRequest(
                    jsonPost("/mixed", "{\"a\": null, \"b\": null, \"c\": \"y\", \"d\": \"z\"}"));
            LOG.error("[mixed: a=null (nullable:true) AND b=null (type union)] errors? "
                    + bothNull.hasErrors() + formatMessages(bothNull));

            final ValidationReport onlyAnull = validatorFor(spec).validateRequest(
                    jsonPost("/mixed", "{\"a\": null, \"b\": 1, \"c\": \"y\", \"d\": \"z\"}"));
            LOG.error("[mixed: a=null (nullable:true)] errors? "
                    + onlyAnull.hasErrors() + formatMessages(onlyAnull));

            final ValidationReport onlyBnull = validatorFor(spec).validateRequest(
                    jsonPost("/mixed", "{\"a\": \"x\", \"b\": null, \"c\": \"y\", \"d\": \"z\"}"));
            LOG.error("[mixed: b=null (type union)] errors? "
                    + onlyBnull.hasErrors() + formatMessages(onlyBnull));
        }
    }

    // ============================================================================
    // 21. discriminator (still works in 3.1, but with type unions added)
    // ============================================================================

    @Nested
    @DisplayName("21. discriminator (3.1 with oneOf)")
    class Discriminator {

        private final String spec =
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
        @DisplayName("documents: discriminator with mapping behaviour")
        void documents_discriminator_mapping() {
            final ValidationReport report = validatorFor(spec).validateRequest(
                    jsonPost("/shapes", "{\"kind\": \"circle\", \"radius\": 5}"));
            LOG.error("[discriminator: kind=circle (mapped to Circle)] errors? {}{}",
                    report.hasErrors(), formatMessages(report));
        }

        @Test
        @DisplayName("documents: discriminator using exact schema name")
        void documents_discriminator_using_schema_name() {
            // Per spec, when no mapping is given, the discriminator value
            // must equal the schema name. Test with capitalised "Circle".
            final ValidationReport report = validatorFor(spec).validateRequest(
                    jsonPost("/shapes", "{\"kind\": \"Circle\", \"radius\": 5}"));
            LOG.error("[discriminator: kind=Circle (matches schema name)] errors? {}{}",
                    report.hasErrors(), formatMessages(report));
        }

        @Test
        @DisplayName("circle payload missing radius fails (oneOf still triggers)")
        void circle_missing_radius_fails() {
            assertFails(validatorFor(spec).validateRequest(
                    jsonPost("/shapes", "{\"kind\": \"Circle\"}")));
        }
    }

    // ============================================================================
    // 22. Recursive $ref (e.g., tree structures)
    // ============================================================================

    @Nested
    @DisplayName("22. Recursive $ref")
    class Recursive {

        private final String spec =
                "openapi: 3.1.0\n"
              + "info: {title: t, version: '1'}\n"
              + "paths:\n"
              + "  /tree:\n"
              + "    post:\n"
              + "      requestBody:\n"
              + "        required: true\n"
              + "        content:\n"
              + "          application/json:\n"
              + "            schema: { $ref: '#/components/schemas/Node' }\n"
              + "      responses: { '200': {description: ok} }\n"
              + "components:\n"
              + "  schemas:\n"
              + "    Node:\n"
              + "      type: object\n"
              + "      required: [name]\n"
              + "      properties:\n"
              + "        name: { type: string }\n"
              + "        children:\n"
              + "          type: array\n"
              + "          items: { $ref: '#/components/schemas/Node' }\n";

        @Test
        @DisplayName("recursive structure validates")
        void recursive_structure_passes() {
            assertPasses(validatorFor(spec).validateRequest(jsonPost("/tree",
                    "{\"name\":\"root\",\"children\":[" +
                    "{\"name\":\"a\",\"children\":[{\"name\":\"a.1\"}]}," +
                    "{\"name\":\"b\"}" +
                    "]}")));
        }

        @Test
        @DisplayName("recursive structure with bad child fails")
        void recursive_structure_bad_child_fails() {
            assertFails(validatorFor(spec).validateRequest(jsonPost("/tree",
                    "{\"name\":\"root\",\"children\":[{\"children\":[]}]}"))); // missing name on child
        }
    }
}
