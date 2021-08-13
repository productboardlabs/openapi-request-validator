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
public class OpenAPIV3RequestContentTypeValidationTest {

    final OpenApiInteractionValidator classUnderTest =
            createForSpecificationUrl("/oai/v3/api-with-complex-contenttypes.yaml").build();

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] data() {
        return new Object[][]{
                {"matches_globalWildcards", "/request/wildcard/global", "image/jpeg", passes()},
                {"matches_subtypeWildcards", "/request/wildcard/subtype", "image/jpeg", passes()},
        };
    }

    @Parameterized.Parameter
    public String testName;

    @Parameterized.Parameter(1)
    public String requestPath;

    @Parameterized.Parameter(2)
    public String requestContentType;

    @Parameterized.Parameter(3)
    public Consumer<ValidationReport> assertion;

    @Test
    public void test() {
        final Request request = SimpleRequest.Builder
                .post(requestPath)
                .withContentType(requestContentType)
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
