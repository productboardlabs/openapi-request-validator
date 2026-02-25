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

public class OpenAPIV3RequestContentTypeValidationTest {

    final OpenApiInteractionValidator classUnderTest =
            createForSpecificationUrl("/oai/v3/api-with-complex-contenttypes.yaml").build();

    static Stream<TestCase> params() {
        return Stream.of(
                new TestCase("singleContentType_validRequest", "/request/nonwildcard/single", "application/json", passes()),
                new TestCase("singleContentType_invalidRequest", "/request/nonwildcard/single", "text/plain", fails()),
                new TestCase("singleContentType_invalidRequest_emptyContentType", "/request/nonwildcard/single", "", fails()),
                new TestCase("multipleContentType_validRequest", "/request/nonwildcard/multiple", "text/plain", passes()),
                new TestCase("multipleContentType_invalidRequest", "/request/nonwildcard/multiple", "image/png", fails()),
                new TestCase("globalWildcards_validRequest", "/request/wildcard/global", "image/jpeg", passes()),
                new TestCase("subtypeWildcards_validRequest", "/request/wildcard/subtype", "image/jpeg", passes()),
                new TestCase("subtypeWildcards_invalidRequest", "/request/wildcard/subtype", "text/xml", fails()),
                new TestCase("mixedWildcards_validRequest", "/request/wildcard/subtype", "image/jpeg", passes()),
                new TestCase("mixedWildcards_invalidRequest", "/request/wildcard/subtype", "text/xml", fails())
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("params")
    public void test(final TestCase testCase) {
        final Request request = SimpleRequest.Builder
                .post(testCase.requestPath())
                .withContentType(testCase.requestContentType())
                .build();
        testCase.assertion().accept(classUnderTest.validateRequest(request));
    }

    record TestCase(String testName, String requestPath, String requestContentType, Consumer<ValidationReport> assertion) {
        @Override
        public String toString() {
            return testName;
        }
    }

    private static Consumer<ValidationReport> passes() {
        return ValidatorTestUtil::assertPass;
    }

    private static Consumer<ValidationReport> fails() {
        return r -> assertFail(r, "validation.request.contentType.notAllowed");
    }

}
