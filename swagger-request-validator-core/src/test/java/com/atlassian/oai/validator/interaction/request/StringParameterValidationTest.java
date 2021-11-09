package com.atlassian.oai.validator.interaction.request;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.v3.oas.models.OpenAPI;
import org.junit.Test;

import java.util.UUID;

import static com.atlassian.oai.validator.util.ParameterGenerator.dateParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.dateTimeParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.emailParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.enumeratedStringParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.ipv4Param;
import static com.atlassian.oai.validator.util.ParameterGenerator.ipv6Param;
import static com.atlassian.oai.validator.util.ParameterGenerator.patternStringParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.stringParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.stringParamFormat;
import static com.atlassian.oai.validator.util.ParameterGenerator.uriParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.uuidParam;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;
import static java.lang.Integer.MAX_VALUE;

public class StringParameterValidationTest {

    private final ParameterValidator classUnderTest = new ParameterValidator(
            new SchemaValidator(new OpenAPI(), new MessageResolver()), new MessageResolver());

    @Test
    public void validate_withNullValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate((String) null, stringParam(false)));
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenNotRequired() {
        assertPass(classUnderTest.validate("", stringParam(false)));
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenPatternAllows() {
        assertPass(classUnderTest.validate("", patternStringParam("^.*$", false)));
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenPatternDoesNotAllow() {
        assertFail(classUnderTest.validate("", patternStringParam("^.+$", false)),
                "validation.request.parameter.schema.pattern");
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenMinLengthMet() {
        assertPass(classUnderTest.validate("", stringParam(0, MAX_VALUE, false)));
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenMinLengthNotMet() {
        assertFail(classUnderTest.validate("", stringParam(1, MAX_VALUE, false)),
                "validation.request.parameter.schema.minLength");
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenEnum() {
        assertFail(classUnderTest.validate("", enumeratedStringParam(false, "VALID")),
                "validation.request.parameter.schema.enum");
    }

    @Test
    public void validate_withNullValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate((String) null, stringParam(true)),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        assertFail(classUnderTest.validate("", stringParam(true)),
                "validation.request.parameter.missing");
    }

    @Test
    public void validate_withEnum_shouldPass_whenMatching() {
        assertPass(classUnderTest.validate("Enum-2", enumeratedStringParam("Enum-1", "Enum-2", "Enum-3")));
    }

    @Test
    public void validate_withEnum_shouldFail_whenNotMatching() {
        assertFail(classUnderTest.validate("Unknown", enumeratedStringParam("Enum-1", "Enum-2", "Enum-3")),
                "validation.request.parameter.schema.enum");
        // case sensitive match necessary
        assertFail(classUnderTest.validate("ENUM-1", enumeratedStringParam("Enum-1", "Enum-2", "Enum-3")),
                "validation.request.parameter.schema.enum");
    }

    @Test
    public void validate_withPattern_shouldFail_whenNoMatch() {
        assertFail(classUnderTest.validate("NO_CAPS_ALLOWED", patternStringParam("^[a-z]*$")),
                "validation.request.parameter.schema.pattern");
    }

    @Test
    public void validate_withPattern_shouldPass_whenMatch() {
        assertPass(classUnderTest.validate("allgood", patternStringParam("[a-z]*")));
    }

    @Test
    public void validate_withMinLength_shouldFail_whenTooShort() {
        assertFail(classUnderTest.validate("short", stringParam(6, null)),
                "validation.request.parameter.schema.minLength");
    }

    @Test
    public void validate_withMinLength_shouldPass_whenMissingButNotRequired() {
        assertPass(classUnderTest.validate((String) null, stringParam(6, null, false)));
    }

    @Test
    public void validate_withMinLength_shouldPass_whenLongEnough() {
        assertPass(classUnderTest.validate("longer", stringParam(6, null)));
    }

    @Test
    public void validate_withMaxLength_shouldFail_whenTooLong() {
        assertFail(classUnderTest.validate("far too long for my taste", stringParam(null, 10)),
                "validation.request.parameter.schema.maxLength");
    }

    @Test
    public void validate_withMaxLength_shouldPass_whenShortEnough() {
        assertPass(classUnderTest.validate("easily short enough", stringParam(null, 30)));
    }

    @Test
    public void validate_withMaxLength_shouldPass_whenMissingButNotRequired() {
        assertPass(classUnderTest.validate((String) null, stringParam(null, 6, false)));
    }

    @Test
    public void validate_withDateFormat_shouldFail_whenNotAValidISODate() {
        assertFail(classUnderTest.validate("2016--5dd", dateParam()),
                "validation.request.parameter.schema.format.date");
    }

    @Test
    public void validate_withDateFormat_shouldPass_whenAValidISODate() {
        assertPass(classUnderTest.validate("2016-09-28", dateParam()));
    }

    @Test
    public void validate_withDateTimeFormat_shouldFail_whenNotAValidISODate() {
        assertFail(classUnderTest.validate("2016--5dd-slkdjfl01938", dateTimeParam()),
                "validation.request.parameter.schema.format.date-time");
    }

    @Test
    public void validate_withDateTimeFormat_shouldPass_whenAValidISODate() {
        assertPass(classUnderTest.validate("2016-09-28T11:22:33.111Z", dateTimeParam()));
    }

    @Test
    public void validate_withUUIDFormat_shouldPass_whenAValidUUID() {
        assertPass(classUnderTest.validate(UUID.randomUUID().toString(), uuidParam()));
    }

    @Test
    public void validate_withUUIDFormat_shouldFail_whenInvalidUUID() {
        assertFail(classUnderTest.validate("notauuid", uuidParam()),
                "validation.request.parameter.schema.format.uuid");
    }

    @Test
    public void validate_withEmailFormat_shouldPass_whenAValidEmail() {
        assertPass(classUnderTest.validate("some.body@somewhere.com", emailParam()));
    }

    @Test
    public void validate_withEmailFormat_shouldFail_whenInvalidEmail() {
        assertFail(classUnderTest.validate("notanemail", emailParam()),
                "validation.request.parameter.schema.format.email");
    }

    @Test
    public void validate_withIPv4Format_shouldPass_whenAValidIPAddress() {
        assertPass(classUnderTest.validate("192.168.0.1", ipv4Param()));
    }

    @Test
    public void validate_withIPv4Format_shouldFail_whenInvalidIPAddress() {
        assertFail(classUnderTest.validate("192.0.0", ipv4Param()),
                "validation.request.parameter.schema.format.ipv4");
    }

    @Test
    public void validate_withIPv6Format_shouldPass_whenAValidIPAddress() {
        assertPass(classUnderTest.validate("::1", ipv6Param()));
    }

    @Test
    public void validate_withIPv6Format_shouldFail_whenInvalidIPAddress() {
        assertFail(classUnderTest.validate(":1", ipv6Param()),
                "validation.request.parameter.schema.format.ipv6");
    }

    @Test
    public void validate_withURIFormat_shouldPass_whenAValidURI() {
        assertPass(classUnderTest.validate("http://foo.com", uriParam()));
    }

    @Test
    public void validate_withURIFormat_shouldFail_whenInvalidURI() {
        assertFail(classUnderTest.validate("http://<>.com", uriParam()),
                "validation.request.parameter.schema.format.uri");
    }

    @Test
    public void validate_withUnsupportedFormat_shouldPass() {
        assertPass(classUnderTest.validate("should-pass", stringParamFormat("unsupported")));
    }
}
