package com.atlassian.oai.validator.schema;

import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.google.common.collect.ImmutableList;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.junit.Test;

import java.util.List;

import static com.atlassian.oai.validator.schema.SchemaValidator.ADDITIONAL_PROPERTIES_KEY;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFailWithoutContext;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SchemaValidatorTest {

    private final SchemaValidator classUnderTest = validator("/oai/v2/api-users.json");

    @Test(expected = IllegalArgumentException.class)
    public void validate_withNullValue_shouldThrowException() {
        final String value = null;
        final Schema schema = new Schema();

        classUnderTest.validate(value, schema, "prefix");
    }

    @Test(expected = IllegalArgumentException.class)
    public void validate_withEmptyValue_shouldThrowException() {
        final String value = "";
        final Schema schema = new Schema();

        classUnderTest.validate(value, schema, "prefix");
    }

    @Test
    public void validate_withNullSchema_shouldValidateAnyJson() {
        final List<String> values = ImmutableList.of("1", "\"string\"", "{\"prop\":3}", "[1,2,3]", "null");

        values.forEach(v -> assertPass(classUnderTest.validate(v, null, "prefix")));
    }

    @Test
    public void validate_withValidProperty_shouldPass() {
        final String value = "1";
        final Schema schema = new StringSchema();

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withInvalidProperty_shouldFail() {
        final String value = "1.0";
        final Schema schema = new IntegerSchema();

        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.type");
    }

    @Test
    public void validate_withUnquotedStringProperty_shouldPass() {
        final String value = "bob";
        final Schema schema = new StringSchema();

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withQuotedStringProperty_shouldPass() {
        final String value = "\"bob\"";
        final Schema schema = new StringSchema();

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withValidModel_shouldPass_whenModelInline() {
        final String value = "{\"foo\":\"bar\"}";
        final Schema schema = new ObjectSchema()
                .addProperties("foo", new StringSchema())
                .required(singletonList("foo"));

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withInvalidModel_shouldFail_whenModelInline() {
        final String value = "{\"foos\":\"bar\"}";
        final Schema schema = new ObjectSchema()
                .addProperties("foo", new StringSchema())
                .required(singletonList("foo"));

        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.required");
    }

    @Test
    public void validate_withValidModel_shouldPass_whenModelReferenced() {
        final String value = "{\"title\":\"bar\", \"message\":\"something\"}";
        final Schema schema = new Schema().$ref("#/components/schemas/Error");

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withInvalidModel_shouldFail_whenModelReferenced() {
        final String value = "{\"title\":\"bar\"}";
        final Schema schema = new Schema().$ref("#/components/schemas/Error");

        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.required");
    }

    @Test
    public void validate_withExtraFields_shouldFail_whenModelReferenced() {
        final String value = "{\"title\":\"bar\", \"message\":\"something\", \"extra\":\"value\"}";
        final Schema schema = new Schema().$ref("#/components/schemas/Error");

        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.additionalProperties");
    }

    @Test
    public void validate_withExtraFields_shouldFail_whenModelInline() {
        final String value = "{\"foo\":\"bar\", \"extra\":\"value\"}";
        final Schema schema = new ObjectSchema()
                .addProperties("foo", new StringSchema())
                .required(singletonList("foo"));

        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.additionalProperties");
    }

    @Test
    public void validate_withInvalidJsonSchema_shouldFail() {
        final String value = "{\"title\":\"bar\", \"message\":\"something\"}";
        final Schema schema = new Schema().$ref("#/definitions/{\"What\":\"This actually happened!\"}");

        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.processingError");
    }

    @Test
    public void validate_withOtherException_shouldFail() {
        final String value = "{\"title\":\"bar\", \"message\":\"something\"}";
        final Schema schema = new Schema().$ref("#/components/schemas/Error");

        final OpenAPI mockApi = mock(OpenAPI.class);
        when(mockApi.getComponents()).thenThrow(new IllegalStateException("Testing exception handling"));
        final SchemaValidator failingValidator = new SchemaValidator(mockApi, new MessageResolver());

        assertFailWithoutContext(failingValidator.validate(value, schema, "prefix"),
                "validation.prefix.schema.unknownError");
    }

    @Test
    public void validate_withJsonSchemaComposition_shouldWork_whenAdditionalPropertyValidationIgnored() {

        final SchemaValidator classUnderTest = validatorWithAdditionalPropertiesIgnored("/oai/v2/api-composition.yaml");

        final Schema schema = new Schema().$ref("#/components/schemas/User");
        final String value = "{\"firstname\":\"user_firstname\", \"lastname\":\"user_lastname\", \"city\":\"user_city\"}";

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withAllOf_shouldAddInfoOnNestedFailures_whenSubSchemaValidationFails() {

        final SchemaValidator classUnderTest = validatorWithAdditionalPropertiesIgnored("/oai/v2/api-composition.yaml");

        final Schema schema = new Schema().$ref("#/components/schemas/User");
        final String value = "{\"firstname\":\"user_firstname\", \"city\":1}";

        final ValidationReport report = classUnderTest.validate(value, schema, "prefix");
        assertFailWithoutContext(report, "validation.prefix.schema.allOf");

        final ValidationReport.Message message = report.getMessages().get(0);
        assertThat(message.getAdditionalInfo(), iterableWithSize(2));
        assertThat(message.getAdditionalInfo(), hasItem(containsString("/components/schemas/User/allOf/0")));
        assertThat(message.getAdditionalInfo(), hasItem(containsString("/components/schemas/User/allOf/1")));
    }

    @Test
    public void validate_withJsonSchemaComposition_shouldFail_whenAdditionalPropertyValidationNotIgnored() {

        final SchemaValidator classUnderTest = validator("/oai/v2/api-composition.yaml");

        final Schema schema = new Schema().$ref("#/components/schemas/User");
        final String value = "{\"firstname\":\"user_firstname\", \"lastname\":\"user_lastname\", \"city\":\"user_city\"}";

        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.additionalProperties");
    }

    @Test
    public void validate_withValidModel_shouldPass_whenContainsNullValues() {
        final String value =
                "{\"foo\":\"bar\"," +
                        "\"baz\": null," +
                        "\"int\": null," +
                        "\"obj\":{\"obj1\": null, \"obj2\": null, \"obj3\": \"val3\"}," +
                        "\"arr\":[null, \"val1\", \"val2\"]}";
        final Schema schema = new ObjectSchema()
                .addProperties("foo", new StringSchema())
                .addProperties("baz", new StringSchema())
                .addProperties("int", new IntegerSchema())
                .addProperties("obj", new ObjectSchema())
                .addProperties("arr", new ArraySchema())
                .required(singletonList("foo"));

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withValidModel_shouldPass_whenContainsNullValues_inArray() {
        final String value = "{\"arr\": [1, 2, null, 3]}";
        final Schema schema = new ObjectSchema()
                .addProperties("arr", new ArraySchema().items(new IntegerSchema()));

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withValidModel_shouldPass_whenContainsNullValues_inObjects_inArrays() {
        final String value =
                "{\"arr\": [" +
                        "{\"int\":null}," +
                        "{\"str\":null}," +
                        "{\"flt\":null}" +
                        "]}";

        final Schema schema = new Schema()
                .addProperties("arr", new ArraySchema().items(
                        new ObjectSchema()
                                .addProperties("int", new IntegerSchema())
                                .addProperties("str", new StringSchema())
                                .addProperties("flt", new NumberSchema().format("float"))
                ));

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withDiscriminator_shouldPass_whenValid() {

        final SchemaValidator classUnderTest = validatorWithAdditionalPropertiesIgnored("/oai/v2/api-discriminator.yaml");
        final Schema schema = new Schema().$ref("#/components/schemas/Pet");
        final String value = "{\"name\": \"Moggy\", \"petType\": \"Cat\", \"huntingSkill\":\"clueless\"}";

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withDiscriminator_shouldPass_everyTime_whenInvokedMultipleTimes() {

        final SchemaValidator classUnderTest = validatorWithAdditionalPropertiesIgnored("/oai/v2/api-discriminator.yaml");
        final Schema schema = new Schema().$ref("#/components/schemas/Pet");
        final String value = "{\"name\": \"Moggy\", \"petType\": \"Cat\", \"huntingSkill\":\"clueless\"}";

        assertPass(classUnderTest.validate(value, schema, "prefix"));
        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withDiscriminator_shouldFail_whenInvalid() {

        final SchemaValidator classUnderTest = validatorWithAdditionalPropertiesIgnored("/oai/v2/api-discriminator.yaml");
        final Schema schema = new Schema().$ref("#/components/schemas/Pet");
        final String value = "{\"name\": \"Moggy\", \"petType\": \"Cat\", \"huntingSkill\":\"ruthless\"}";

        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.discriminator");
    }

    @Test
    public void validate_withDiscriminator_shouldFail_everyTime_whenInvokedMultipleTimes() {

        final SchemaValidator classUnderTest = validatorWithAdditionalPropertiesIgnored("/oai/v2/api-discriminator.yaml");
        final Schema schema = new Schema().$ref("#/components/schemas/Pet");
        final String value = "{\"name\": \"Moggy\", \"petType\": \"Cat\", \"huntingSkill\":\"ruthless\"}";

        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.discriminator");
        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.discriminator");
        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.discriminator");
        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.discriminator");
        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.discriminator");
    }

    @Test
    public void validate_withDateProperty_shouldPass_whenValid() {
        final String value = "1985-04-12";
        final Schema schema = new DateSchema();

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withDateProperty_shouldFail_whenInvalid() {
        final String value = "1985-99-99";
        final Schema schema = new DateSchema();

        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withDateTimeProperty_shouldPass_whenValid() {
        final String value = "1985-04-12T23:20:50.52Z";
        final Schema schema = new DateTimeSchema();

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withDateTimeProperty_shouldPass_withTimezone() {
        final String value = "1990-12-31T15:59:59+08:00";
        final Schema schema = new DateTimeSchema();

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withDateTimeProperty_shouldFail_withWrongFormat() {
        final String value = "Wed Jul 19 14:21:33 UTC 2017";
        final Schema schema = new DateTimeSchema();

        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.format");
    }

    @Test
    public void validate_withNoDefinitionsBlock_shouldPass_whenValid() {
        final SchemaValidator classUnderTest = validator("/oai/v2/api-no-definitions.json");

        final String value = "{\"id\":123}";
        final Schema schema = new ObjectSchema().addProperties("id", new IntegerSchema());

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withNumberProperty_shouldPass_whenValid() {
        final String value = "1";
        final Schema schema = new NumberSchema();

        assertPass(classUnderTest.validate(value, schema, "prefix"));
    }

    @Test
    public void validate_withNumberProperty_shouldFail_withWrongType_whenInvalid() {
        final String value = "1,2";
        final Schema schema = new NumberSchema();

        assertFailWithoutContext(classUnderTest.validate(value, schema, "prefix"),
                "validation.prefix.schema.type");
    }

    private SchemaValidator validatorWithAdditionalPropertiesIgnored(final String api) {
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        return new SchemaValidator(
                new OpenAPIParser().readLocation(api, null, parseOptions).getOpenAPI(),
                new MessageResolver(
                        LevelResolver
                                .create()
                                .withLevel(ADDITIONAL_PROPERTIES_KEY, ValidationReport.Level.IGNORE)
                                .build())
        );
    }

    private SchemaValidator validator(final String api) {
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        return new SchemaValidator(new OpenAPIParser().readLocation(api, null, parseOptions).getOpenAPI(), new MessageResolver());
    }
}
