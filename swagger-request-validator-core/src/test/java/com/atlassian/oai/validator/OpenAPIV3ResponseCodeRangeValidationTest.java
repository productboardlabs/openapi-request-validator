package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleResponse;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

/**
 * See https://swagger.io/specification/#responses-object
 */
public class OpenAPIV3ResponseCodeRangeValidationTest {

    private final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-with-response-code-range.yaml").build();

    @Test
    public void response_200_matching2XX_shouldPass() {
        final Response response = SimpleResponse.Builder.ok().withBody("10").build();

        assertPass(classUnderTest.validateResponse("/", Request.Method.GET, response));
    }

    @Test
    public void response_204_matching204_shouldPass() {
        final Response response = SimpleResponse.Builder.noContent().withBody("{}").build();

        assertPass(classUnderTest.validateResponse("/", Request.Method.GET, response));
    }

    @Test
    public void response_401_matching4XX_shouldPass() {
        final Response response = SimpleResponse.Builder.unauthorized().withBody("\"response\"").build();

        assertPass(classUnderTest.validateResponse("/", Request.Method.GET, response));
    }

    @Test
    public void response_499_matching4XX_shouldPass() {
        final Response response = SimpleResponse.Builder.status(499).withBody("\"response\"").build();

        assertPass(classUnderTest.validateResponse("/", Request.Method.GET, response));
    }

    @Test
    public void response_503_matching5XX_shouldPass() {
        final Response response = SimpleResponse.Builder.status(503).withBody("true").build();

        assertPass(classUnderTest.validateResponse("/", Request.Method.GET, response));
    }
}
