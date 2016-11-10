package com.atlassian.oai.validator.schema;

import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import io.swagger.parser.SwaggerParser;
import org.junit.Test;

import static com.atlassian.oai.validator.schema.SchemaValidator.ADDITIONAL_PROPERTIES_KEY;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SchemaValidatorTest {

    private SchemaValidator classUnderTest = validator("/oai/api-users.json");

    @Test(expected = IllegalArgumentException.class)
    public void validate_withNullValue_shouldThrowException() {
        final String value = null;
        final Model schema = new ModelImpl();

        classUnderTest.validate(value, schema);
    }

    @Test(expected = IllegalArgumentException.class)
    public void validate_withEmptyValue_shouldThrowException() {
        final String value = "";
        final Model schema = new ModelImpl();

        classUnderTest.validate(value, schema);
    }

    @Test(expected = NullPointerException.class)
    public void validate_withNullModel_shouldThrowNPE() {
        final String value = "1";
        final Model schema = null;

        classUnderTest.validate(value, schema);
    }

    @Test(expected = NullPointerException.class)
    public void validate_withNullProperty_shouldThrowNPE() {
        final String value = "1";
        final Property schema = null;

        classUnderTest.validate(value, schema);
    }

    @Test
    public void validate_withValidProperty_shouldPass() {
        final String value = "1";
        final Property schema = new IntegerProperty();

        assertPass(classUnderTest.validate(value, schema));
    }

    @Test
    public void validate_withInvalidProperty_shouldFail() {
        final String value = "1.0";
        final Property schema = new IntegerProperty();

        assertFail(classUnderTest.validate(value, schema), "validation.schema.type");
    }

    @Test
    public void validate_withUnquotedStringProperty_shouldPass() {
        final String value = "bob";
        final Property schema = new StringProperty();

        assertPass(classUnderTest.validate(value, schema));
    }

    @Test
    public void validate_withQuotedStringProperty_shouldPass() {
        final String value = "\"bob\"";
        final Property schema = new StringProperty();

        assertPass(classUnderTest.validate(value, schema));
    }

    @Test
    public void validate_withValidModel_shouldPass_whenModelInline() {
        final String value = "{\"foo\":\"bar\"}";
        final Model schema = new ModelImpl().property("foo", new StringProperty()).required("foo");

        assertPass(classUnderTest.validate(value, schema));
    }

    @Test
    public void validate_withInvalidModel_shouldFail_whenModelInline() {
        final String value = "{\"foos\":\"bar\"}";
        final Model schema = new ModelImpl().property("foo", new StringProperty()).required("foo");

        assertFail(classUnderTest.validate(value, schema), "validation.schema.required");
    }

    @Test
    public void validate_withValidModel_shouldPass_whenModelReferenced() {
        final String value = "{\"title\":\"bar\", \"message\":\"something\"}";
        final Model schema = new RefModel("#/definitions/Error");

        assertPass(classUnderTest.validate(value, schema));
    }

    @Test
    public void validate_withInvalidModel_shouldFail_whenModelReferenced() {
        final String value = "{\"title\":\"bar\"}";
        final Model schema = new RefModel("#/definitions/Error");

        assertFail(classUnderTest.validate(value, schema), "validation.schema.required");
    }

    @Test
    public void validate_withExtraFields_shouldFail_whenModelReferenced() {
        final String value = "{\"title\":\"bar\", \"message\":\"something\", \"extra\":\"value\"}";
        final Model schema = new RefModel("#/definitions/Error");

        assertFail(classUnderTest.validate(value, schema), "validation.schema.additionalProperties");
    }

    @Test
    public void validate_withExtraFields_shouldFail_whenModelInline() {
        final String value = "{\"foo\":\"bar\", \"extra\":\"value\"}";
        final Model schema = new ModelImpl().property("foo", new StringProperty()).required("foo");

        assertFail(classUnderTest.validate(value, schema), "validation.schema.additionalProperties");
    }

    @Test
    public void validate_withInvalidJsonSchema_shouldFail() {
        final String value = "{\"title\":\"bar\", \"message\":\"something\"}";
        final Model schema = new RefModel("#/definitions/{\"What\":\"This actually happened!\"}");

        assertFail(classUnderTest.validate(value, schema), "validation.schema.processingError");
    }

    @Test
    public void validate_withOtherException_shouldFail() {
        final String value = "{\"title\":\"bar\", \"message\":\"something\"}";
        final Model schema = new RefModel("#/definitions/Error}");

        final Swagger mockApi = mock(Swagger.class);
        when(mockApi.getDefinitions()).thenThrow(new IllegalStateException("Testing exception handling"));
        final SchemaValidator failingValidator = new SchemaValidator(mockApi, new MessageResolver());

        assertFail(failingValidator.validate(value, schema), "validation.schema.unknownError");
    }

    @Test
    public void validate_withJsonSchemaComposition_shouldWork_whenAdditionalPropertyValidationIgnored() {

        final SchemaValidator classUnderTest = validatorWithAdditionalPropertiesIgnored("/oai/api-composition.yaml");

        final Model schema = new RefModel("#/definitions/User");
        final String value = "{\"firstname\":\"user_firstname\", \"lastname\":\"user_lastname\", \"city\":\"user_city\"}";

        assertPass(classUnderTest.validate(value, schema));
    }

    @Test
    public void validate_withJsonSchemaComposition_shouldFail_whenAdditionalPropertyValidationNotIgnored() {

        final SchemaValidator classUnderTest = validator("/oai/api-composition.yaml");

        final Model schema = new RefModel("#/definitions/User");
        final String value = "{\"firstname\":\"user_firstname\", \"lastname\":\"user_lastname\", \"city\":\"user_city\"}";

        assertFail(classUnderTest.validate(value, schema), "validation.schema.additionalProperties");
    }

    @Test
    public void validate_withValidModel_shouldPass_whenContainsNullValues() {
        final String value =
                "{\"foo\":\"bar\"," +
                "\"baz\": null," +
                "\"obj\":{\"obj1\": null, \"obj2\": null, \"obj3\": \"val3\"}," +
                "\"arr\":[null, \"val1\", \"val2\"]}";
        final Model schema = new ModelImpl()
                .property("foo", new StringProperty())
                .property("baz", new StringProperty())
                .property("obj", new ObjectProperty())
                .property("arr", new ArrayProperty())
                .required("foo");

        assertPass(classUnderTest.validate(value, schema));
    }

    private SchemaValidator validatorWithAdditionalPropertiesIgnored(final String api) {
        return new SchemaValidator(
                new SwaggerParser().read(api),
                new MessageResolver(
                        LevelResolver
                                .create()
                                .withLevel(ADDITIONAL_PROPERTIES_KEY, ValidationReport.Level.IGNORE)
                                .build()
                )
        );
    }

    private SchemaValidator validator(final String api) {
        return new SchemaValidator(new SwaggerParser().read(api), new MessageResolver());
    }
}
