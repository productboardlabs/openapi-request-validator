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
public class OpenAPIV3RequestMultiplePathParamValidationTest {

    final OpenApiInteractionValidator classUnderTest =
            createForSpecificationUrl("/oai/v3/api-with-multiple-path-params.yaml").build();

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][]{
                {"hyphenSeparator_shouldPass_whenValid", "/hyphenseparator/aaa-bbb/foo", passes()},
                {"hyphenSeparator_shouldFail_whenInvalid", "/hyphenseparator/aaa-/foo", fails("validation.request.parameter.missing")},
                {"doublehyphenSeparator_shouldPass_whenValid", "/doublehyphenseparator/aaa--bbb/foo", passes()},
                {"doublehyphenSeparator_shouldFail_whenInvalid", "/doublehyphenseparator/--bbb/foo", fails("validation.request.parameter.missing")},
                {"periodSeparator_shouldPass_whenValid", "/periodseparator/aaa.bbb/foo", passes()},
                {"periodSeparator_shouldFail_whenInvalid", "/periodseparator/aaa./foo", fails("validation.request.parameter.missing")},

                // https://github.com/swagger-api/swagger-parser/issues/1169
                // As at v2.0.20 the parser mis-handles separators other than '-' and '.'
                // {"underscoreSeparator_shouldPass_whenValid", "/underscoreseparator/aaa_bbb/foo", passes()},
                // {"colonSeparator_shouldPass_whenValid", "/colonseparator/aaa:bbb/foo", passes()},
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
