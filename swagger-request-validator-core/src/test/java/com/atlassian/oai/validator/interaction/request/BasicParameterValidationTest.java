package com.atlassian.oai.validator.interaction.request;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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

@RunWith(Parameterized.class)
public class BasicParameterValidationTest {

    private final ParameterValidator parameterValidator = new ParameterValidator(
            new SchemaValidator(new OpenAPI(), new MessageResolver()), new MessageResolver());

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Object[][] params() {
        return new Object[][]{
                {"invalid integer should fail", "1.0", intParam(), assertFail("validation.request.parameter.schema.type")},
                {"empty integer should fail", "", intParam(), assertFail("validation.request.parameter.missing")},
                {"valid integer should pass", "333", intParam(), assertPass()},
                {"valid float param should pass", "1.0", floatParam(), assertPass()},
                {"empty float param should fail", "", floatParam(), assertFail("validation.request.parameter.missing")},
                {"valid double param should pass", "1.0", doubleParam(), assertPass()},
                {"empty required double param should fail", "", doubleParam(), assertFail("validation.request.parameter.missing")},
                {"invalid number param should fail", "1.0a", floatParam(), assertFail("validation.request.parameter.schema.type")},
                {"valid boolean param should pass", "true", boolParam(), assertPass()},
                {"empty required boolean param should fail", "", boolParam(), assertFail("validation.request.parameter.missing")},
                {"valid string param should pass", "aaa", stringParam(), assertPass()},
                {"null required string param should fail", null, stringParam(), assertFail("validation.request.parameter.missing")},
                {"empty required string param should fail when empty not allowed", "", stringParam(), assertFail("validation.request.parameter.missing")},
                {"empty required param should pass when empty allowed", "", emptyAllowedQueryParam(), assertPass()},
                {"empty required param should fail when empty allowed but not query param", "", emptyAllowedHeaderParam(), assertFail("validation.request.parameter.missing")},
                {"null required param should fail", null, emptyAllowedQueryParam(), assertFail("validation.request.parameter.missing")},
                {"empty optional param should pass when empty allowed", "", emptyAllowedQueryParam(false), assertPass()},
                {"empty optional param should pass when not conform schema but empty allowed", "", emptyAllowedNonConformQueryParam(false), assertPass()},
                {"empty optional param should pass when conform schema and empty allowed but not query param", "", emptyAllowedHeaderParam(false), assertPass()},
                {"empty optional param should fail when not conform schema and empty allowed but not query param", "", emptyAllowedNonConformHeaderParam(false),
                        assertFail("validation.request.parameter.schema.pattern")}
        };
    }

    @Parameterized.Parameter(0)
    public String description;

    @Parameterized.Parameter(1)
    public String value;

    @Parameterized.Parameter(2)
    public Parameter param;

    @Parameterized.Parameter(3)
    public Consumer<ValidationReport> assertion;

    @Test
    public void test() {
        assertion.accept(parameterValidator.validate(value, param));
    }

}
