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
                {"singleContentType_validRequest", "/request/nonwildcard/single", "application/json", passes()},
                {"singleContentType_invalidRequest", "/request/nonwildcard/single", "text/plain", fails()},
                {"multipleContentType_validRequest", "/request/nonwildcard/multiple", "text/plain", passes()},
                {"multipleContentType_invalidRequest", "/request/nonwildcard/multiple", "image/png", fails()},
                {"globalWildcards_validRequest", "/request/wildcard/global", "image/jpeg", passes()},
                {"subtypeWildcards_validRequest", "/request/wildcard/subtype", "image/jpeg", passes()},
                {"subtypeWildcards_invalidRequest", "/request/wildcard/subtype", "text/xml", fails()},
                {"mixedWildcards_validRequest", "/request/wildcard/subtype", "image/jpeg", passes()},
                {"mixedWildcards_invalidRequest", "/request/wildcard/subtype", "text/xml", fails()},
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

    private static Consumer<ValidationReport> fails() {
        return r -> assertFail(r, "validation.request.contentType.notAllowed");
    }

}
