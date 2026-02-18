package com.atlassian.oai.validator.schema;

import com.atlassian.oai.validator.report.MessageResolver;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFailWithoutContext;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

public class SchemaValidatorV31Test {

    private final String apiWithPathRef = "/oai/v31/api-with-path-ref.yaml";
    private final String apiWithMultiSchema = "/oai/v31/api-with-multi-schema.yaml";

    private final SchemaValidator schemaValidatorWithPathRef = validator(apiWithPathRef);
    private final SchemaValidator schemaValidatorWithMultiSchema = validator(apiWithMultiSchema);

    private OpenAPI parserWithPathRef;
    private OpenAPI parserWithMultiSchema;

    @BeforeEach
    public void setup() {
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parserWithPathRef = new OpenAPIParser().readLocation(apiWithPathRef, null, parseOptions).getOpenAPI();
        parserWithMultiSchema = new OpenAPIParser().readLocation(apiWithMultiSchema, null, parseOptions).getOpenAPI();
    }

    @Test
    public void validate_pathParameter_withRef_shouldPass() {
        final String value = "admin";
        final Schema schema = parserWithPathRef.getPaths().get("/reports/{reportType}").getGet().getParameters().get(0).getSchema();
        assertPass(schemaValidatorWithPathRef.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_pathParameter_withRef_shouldFail() {
        final String value = "user";
        final Schema schema = parserWithPathRef.getPaths().get("/reports/{reportType}").getGet().getParameters().get(0).getSchema();
        assertFail(schemaValidatorWithPathRef.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_pathParameter_withMultiSchema_withRef_havingValidIntValue_shouldPass() {
        final String reportIdSchemaValue1 = "1";
        final String reportIdSchemaValue2 = "9";
        final String reportIdSchemaValue3 = "5";

        final Schema reportIdSchema = parserWithMultiSchema.getPaths().get("/reports/{reportId}/{detailedView}").getGet().getParameters().get(0).getSchema();
        assertPass(schemaValidatorWithMultiSchema.validate(reportIdSchemaValue1, reportIdSchema, "prefix"));
        assertPass(schemaValidatorWithMultiSchema.validate(reportIdSchemaValue2, reportIdSchema, "prefix"));
        assertPass(schemaValidatorWithMultiSchema.validate(reportIdSchemaValue3, reportIdSchema, "prefix"));
    }

    @Test
    public void validate_pathParameter_withMultiSchema_withRef_havingInvalidIntValue_shouldFail() {
        final String reportIdSchemaValue1 = "10";
        final String reportIdSchemaValue2 = "0";

        final Schema reportIdSchema = parserWithMultiSchema.getPaths().get("/reports/{reportId}/{detailedView}").getGet().getParameters().get(0).getSchema();
        assertFail(schemaValidatorWithMultiSchema.validate(reportIdSchemaValue1, reportIdSchema, "prefix"));
        assertFail(schemaValidatorWithMultiSchema.validate(reportIdSchemaValue2, reportIdSchema, "prefix"));
    }

    @Test
    public void validate_pathParameter_withMultiSchema_withRef_shouldPass() {
        final String detailedViewSchemaValue1 = "1";
        final String detailedViewSchemaValue2 = "true";
        final String detailedViewSchemaValue3 = "default";

        final Schema detailedViewSchema = parserWithMultiSchema.getPaths().get("/reports/{reportId}/{detailedView}").getGet().getParameters().get(1).getSchema();

        assertPass(schemaValidatorWithMultiSchema.validate(detailedViewSchemaValue1, detailedViewSchema, "prefix"));
        assertPass(schemaValidatorWithMultiSchema.validate(detailedViewSchemaValue2, detailedViewSchema, "prefix"));
        assertPass(schemaValidatorWithMultiSchema.validate(detailedViewSchemaValue3, detailedViewSchema, "prefix"));
    }

    @Test
    public void validate_pathParameter_withMultiSchema_withRef_havingValidBoolValue_shouldPass() {
        final String includeSummarySchemaValue1 = "true";
        final String includeSummarySchemaValue2 = "false";

        final Schema includeSummarySchema = parserWithMultiSchema.getPaths().get("/reports/{reportId}/{detailedView}").getGet().getParameters().get(2).getSchema();

        assertPass(schemaValidatorWithMultiSchema.validate(includeSummarySchemaValue1, includeSummarySchema, "prefix"));
        assertPass(schemaValidatorWithMultiSchema.validate(includeSummarySchemaValue2, includeSummarySchema, "prefix"));
    }

    @Test
    public void validate_pathParameter_withMultiSchema_withRef_havingInvalidBoolValue_shouldFail() {
        final String includeSummarySchemaValue1 = "test";

        final Schema includeSummarySchema = parserWithMultiSchema.getPaths().get("/reports/{reportId}/{detailedView}").getGet().getParameters().get(2).getSchema();

        assertFailWithoutContext(schemaValidatorWithMultiSchema.validate(includeSummarySchemaValue1, includeSummarySchema, "prefix"), "validation.prefix.schema.invalidJson");
    }

    private SchemaValidator validator(final String api) {
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        return new SchemaValidator(new OpenAPIParser().readLocation(api, null, parseOptions).getOpenAPI(), new MessageResolver());
    }
}
