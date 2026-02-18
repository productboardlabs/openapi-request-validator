package com.atlassian.oai.validator.interaction.request;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;
import java.util.function.Consumer;

import static com.atlassian.oai.validator.util.ParameterGenerator.boolParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.doubleParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.emptyAllowedHeaderParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.emptyAllowedNonConformHeaderParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.emptyAllowedNonConformQueryParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.emptyAllowedQueryParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.floatParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.intParam;
import static com.atlassian.oai.validator.util.ParameterGenerator.stringParam;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

public class BasicParameterValidationTest {

    private final ParameterValidator parameterValidator = new ParameterValidator(
            new SchemaValidator(new OpenAPI(), new MessageResolver()), new MessageResolver());

    static Stream<TestCase> params() {
        return Stream.of(
                new TestCase("invalid integer should fail", "1.0", intParam(), assertFail("validation.request.parameter.schema.type")),
                new TestCase("empty integer should fail", "", intParam(), assertFail("validation.request.parameter.missing")),
                new TestCase("valid integer should pass", "333", intParam(), assertPass()),
                new TestCase("valid float param should pass", "1.0", floatParam(), assertPass()),
                new TestCase("empty float param should fail", "", floatParam(), assertFail("validation.request.parameter.missing")),
                new TestCase("valid double param should pass", "1.0", doubleParam(), assertPass()),
                new TestCase("empty required double param should fail", "", doubleParam(), assertFail("validation.request.parameter.missing")),
                new TestCase("invalid number param should fail", "1.0a", floatParam(), assertFail("validation.request.parameter.schema.type")),
                new TestCase("valid boolean param should pass", "true", boolParam(), assertPass()),
                new TestCase("empty required boolean param should fail", "", boolParam(), assertFail("validation.request.parameter.missing")),
                new TestCase("valid string param should pass", "aaa", stringParam(), assertPass()),
                new TestCase("null required string param should fail", null, stringParam(), assertFail("validation.request.parameter.missing")),
                new TestCase("empty required string param should fail when empty not allowed", "", stringParam(), assertFail("validation.request.parameter.missing")),
                new TestCase("empty required param should pass when empty allowed", "", emptyAllowedQueryParam(), assertPass()),
                new TestCase("empty required param should fail when empty allowed but not query param", "",
                        emptyAllowedHeaderParam(), assertFail("validation.request.parameter.missing")),
                new TestCase("null required param should fail", null, emptyAllowedQueryParam(), assertFail("validation.request.parameter.missing")),
                new TestCase("empty optional param should pass when empty allowed", "", emptyAllowedQueryParam(false), assertPass()),
                new TestCase("empty optional param should pass when not conform schema but empty allowed", "", emptyAllowedNonConformQueryParam(false), assertPass()),
                new TestCase("empty optional param should pass when conform schema and empty allowed but not query param", "", emptyAllowedHeaderParam(false), assertPass()),
                new TestCase("empty optional param should fail when not conform schema and empty allowed but not query param", "", emptyAllowedNonConformHeaderParam(false),
                        assertFail("validation.request.parameter.schema.pattern")),
                new TestCase("the null string param should pass when null not allowed", "null", stringParam(), assertPass())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("params")
    public void test(final TestCase testCase) {
        testCase.assertion().accept(parameterValidator.validate(testCase.value(), testCase.param()));
    }

    record TestCase(String description, String value, Parameter param, Consumer<ValidationReport> assertion) {}

}
