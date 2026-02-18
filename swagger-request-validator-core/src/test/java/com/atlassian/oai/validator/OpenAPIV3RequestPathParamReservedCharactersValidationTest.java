package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.util.ValidatorTestUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.atlassian.oai.validator.OpenApiInteractionValidator.createForSpecificationUrl;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;

public class OpenAPIV3RequestPathParamReservedCharactersValidationTest {

    final OpenApiInteractionValidator classUnderTest =
            createForSpecificationUrl("/oai/v3/api-with-path-param-reserved-characters.yaml").build();

    static Stream<TestCase> params() {
        return Stream.of(
                new TestCase("shouldPass_whenValid", "/test/abc%2F%7C/foo", passes()),
                new TestCase("shouldFail_whenInvalid", "/test/abc%2F/foo", fails("validation.request.parameter.schema.pattern"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("params")
    public void test(final TestCase testCase) {
        final Request request = SimpleRequest.Builder
                .get(testCase.requestPath())
                .build();
        testCase.assertion().accept(classUnderTest.validateRequest(request));
    }

    record TestCase(String testName, String requestPath, Consumer<ValidationReport> assertion) {}

    private static Consumer<ValidationReport> passes() {
        return ValidatorTestUtil::assertPass;
    }

    private static Consumer<ValidationReport> fails(final String expectedKey) {
        return r -> assertFail(r, expectedKey);
    }
}
