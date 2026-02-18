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

public class OpenAPIV3RequestMultiplePathParamValidationTest {

    final OpenApiInteractionValidator classUnderTest =
            createForSpecificationUrl("/oai/v3/api-with-multiple-path-params.yaml").build();

    static Stream<TestCase> params() {
        return Stream.of(
                new TestCase("hyphenSeparator_shouldPass_whenValid", "/hyphenseparator/aaa-bbb/foo", passes()),
                new TestCase("hyphenSeparator_shouldFail_whenInvalid", "/hyphenseparator/aaa-/foo",
                        fails("validation.request.parameter.missing")),
                new TestCase("doublehyphenSeparator_shouldPass_whenValid", "/doublehyphenseparator/aaa--bbb/foo", passes()),
                new TestCase("doublehyphenSeparator_shouldFail_whenInvalid", "/doublehyphenseparator/--bbb/foo",
                        fails("validation.request.parameter.missing")),
                new TestCase("periodSeparator_shouldPass_whenValid", "/periodseparator/aaa.bbb/foo", passes()),
                new TestCase("periodSeparator_shouldFail_whenInvalid", "/periodseparator/aaa./foo",
                        fails("validation.request.parameter.missing"))
                // https://github.com/swagger-api/swagger-parser/issues/1169
                // As at v2.0.20 the parser mis-handles separators other than '-' and '.'
                // new TestCase("underscoreSeparator_shouldPass_whenValid", "/underscoreseparator/aaa_bbb/foo", passes()),
                // new TestCase("colonSeparator_shouldPass_whenValid", "/colonseparator/aaa:bbb/foo", passes()),
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
