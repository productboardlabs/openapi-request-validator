package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.util.ValidatorTestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Consumer;

import static com.atlassian.oai.validator.OpenApiInteractionValidator.createForSpecificationUrl;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;

@RunWith(Parameterized.class)
public class OpenAPIV3RequestPathParamReservedCharactersValidationTest {

    final OpenApiInteractionValidator classUnderTest =
            createForSpecificationUrl("/oai/v3/api-with-path-param-reserved-characters.yaml").build();

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][]{
                {"shouldPass_whenValid", "/test/abc%2F%7C/foo", passes()},
                {"shouldFail_whenInvalid", "/test/abc%2F/foo", fails("validation.request.parameter.schema.pattern")}
        };
    }

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String requestPath;

    @Parameterized.Parameter(2)
    public Consumer<ValidationReport> assertion;

    @Test
    public void test() {
        final Request request = SimpleRequest.Builder
                .get(requestPath)
                .build();
        assertion.accept(classUnderTest.validateRequest(request));
    }

    private static Consumer<ValidationReport> passes() {
        return ValidatorTestUtil::assertPass;
    }

    private static Consumer<ValidationReport> fails(final String expectedKey) {
        return r -> assertFail(r, expectedKey);
    }
}
