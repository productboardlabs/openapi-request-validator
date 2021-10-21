package com.atlassian.oai.validator.interaction.request;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ParameterGenerator.enumeratedFloatParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.floatParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.floatParamFormat;
import static com.atlassian.oai.validator.util.ParameterGenerator.floatParamMultipleOf;
import static com.atlassian.oai.validator.util.ParameterGenerator.stringParam;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

public class NumberParameterValidationTest {

    private final ParameterValidator classUnderTest = new ParameterValidator(
            new SchemaValidator(new OpenAPI(), new MessageResolver()), new MessageResolver());

    @Test
    public void validate_withNullValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate((String) null, floatParam(false)));
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenNotRequired() {
        assertFail(classUnderTest.validate("", floatParam(false)),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withNullValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate((String) null, floatParam(true)),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate("", floatParam(true)),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withNonNumericValue_shouldFail() {
        assertFail(classUnderTest.validate("not-a-Number", floatParam()),
                "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_withPositiveNumericValue_shouldPass() {
        assertPass(classUnderTest.validate("123.456", floatParam()));
    }

    @Test
    public void validate_withNegativeNumericValue_shouldPass() {
        assertPass(classUnderTest.validate("-123.456", floatParam()));
    }

    @Test
    public void validate_withValueGreaterThanMax_shouldFail_ifMaxSpecified() {
        assertFail(classUnderTest.validate("1.1", floatParam(null, 1.0)),
                "validation.request.parameter.schema.maximum");
    }

    @Test
    public void validate_withValueEqualToMax_shouldFail_ifExclusiveMaxSpecified() {
        assertFail(classUnderTest.validate("1.0", floatParam(null, 1.0, null, true)),
                "validation.request.parameter.schema.maximum");
    }

    @Test
    public void validate_withValueEqualToMin_shouldFail_ifExclusiveMinSpecified() {
        assertFail(classUnderTest.validate("1.0", floatParam(1.0, null, true, null)),
                "validation.request.parameter.schema.minimum");
    }

    @Test
    public void validate_withValueLessThanMin_shouldFail_ifMinSpecified() {
        assertFail(classUnderTest.validate("0.9", floatParam(1.0, null)),
                "validation.request.parameter.schema.minimum");
    }

    @Test
    public void validate_withValueInRange_shouldPass() {
        assertPass(classUnderTest.validate("1.1", floatParam(1.0, 1.2)));
    }

    @Test
    public void validate_withValueNotMultipleOf_shouldFail() {
        assertFail(classUnderTest.validate("1.6", floatParamMultipleOf(0.5f)),
                "validation.request.parameter.schema.multipleOf");
    }

    @Test
    public void validate_withValueMultipleOf_shouldPass() {
        assertPass(classUnderTest.validate("1.5", floatParamMultipleOf(0.5f)));
    }

    @Test
    public void validate_withFormatNull_shouldPass() {
        assertPass(classUnderTest.validate("25", floatParamFormat(null)));
    }

    @Test
    public void validate_withFormatUnknown_shouldPass() {
        assertPass(classUnderTest.validate("25", floatParamFormat("unknown")));
    }

    @Test
    public void validate_withNonNumericValueFormatUnknown_shouldFail() {
        assertFail(classUnderTest.validate("not-a-Number", floatParamFormat("unknown")),
                "validation.request.parameter.schema.type");
    }

    @Test
    public void validate_whenValidatorDoesNotSupportParameterType_shouldPass() {
        assertPass(classUnderTest.validate("invalid parameter", stringParam(true)));
    }

    @Test
    public void validate_withValidEnumeratedValue_shouldPass() {
        assertPass(classUnderTest.validate("2.0", enumeratedFloatParam(1.0f, 2.0f, 3.0f)));
    }

    @Test
    public void validate_withInvalidEnumeratedValue_shouldFail() {
        assertFail(classUnderTest.validate("1.4", enumeratedFloatParam(1.1f, 1.2f, 1.3f)),
                "validation.request.parameter.schema.enum");
    }

}
