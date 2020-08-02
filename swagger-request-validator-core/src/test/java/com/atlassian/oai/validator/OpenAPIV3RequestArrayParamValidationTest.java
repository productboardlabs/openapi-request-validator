package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import org.junit.Test;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

/**
 * See https://swagger.io/specification/#parameter-object
 */
public class OpenAPIV3RequestArrayParamValidationTest {

    // Note: Arrays used in this test deliberately have numeric items so that they will
    // fail if parsing is done incorrectly (as they will become String valued)

    private final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-with-array-params.yaml").build();

    @Test
    public void simpleParam_inPath_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder
                .get("/style/simple/1,2,3")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void simpleParam_inPath_shouldFail_whenTooManyItems() {
        final Request request = SimpleRequest.Builder
                .get("/style/simple/1,2,3,4")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.collection.tooManyItems");
    }

    @Test
    public void simpleParam_inHeader_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder
                .get("/style/simple")
                .withHeader("param", "1,2,3")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void simpleParam_inHeader_shouldFail_whenTooManyItems() {
        final Request request = SimpleRequest.Builder
                .get("/style/simple")
                .withHeader("param", "1,2,3,4")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.collection.tooManyItems");
    }

    @Test
    public void explodedFormParam_inQuery_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder
                .get("/style/form")
                .withQueryParam("exploded", "1", "2", "3")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void explodedFormParam_inQuery_shouldFail_whenTooManyItems() {
        final Request request = SimpleRequest.Builder
                .get("/style/form")
                .withQueryParam("exploded", "1", "2", "3", "4")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.collection.tooManyItems");
    }

    @Test
    public void explodedFormParam_inHeader_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder
                .get("/style/form")
                .withHeader("exploded", "1", "2", "3")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void explodedFormParam_inHeader_shouldFail_whenTooManyItems() {
        final Request request = SimpleRequest.Builder
                .get("/style/form")
                .withHeader("exploded", "1", "2", "3", "4")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.collection.tooManyItems");
    }

    @Test
    public void nonExplodedFormParam_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder
                .get("/style/form")
                .withQueryParam("nonexploded", "1,2,3")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void nonExplodedFormParam_shouldFail_whenTooManyItems() {
        final Request request = SimpleRequest.Builder
                .get("/style/form")
                .withQueryParam("nonexploded", "1,2,3,3")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.collection.tooManyItems");
    }

    @Test
    public void nonExplodedFormParam_shouldFail_whenIncorrectFormat() {
        final Request request = SimpleRequest.Builder
                .get("/style/form")
                .withQueryParam("nonexploded", "1", "2", "3")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.collection.invalidFormat");
    }

    @Test
    public void formParam_defaultsToExploded() {
        final Request request = SimpleRequest.Builder
                .get("/style/form")
                .withQueryParam("explodednotdefined", "1", "2", "3")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void spaceDelimitedParam_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder
                .get("/style/spaceDelimited")
                .withQueryParam("query", "1 2 3")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void spaceDelimitedParam_shouldFail_whenTooManyItems() {
        final Request request = SimpleRequest.Builder
                .get("/style/spaceDelimited")
                .withQueryParam("query", "1 2 3 4")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.collection.tooManyItems");
    }

    @Test
    public void pipeDelimitedParam_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder
                .get("/style/pipeDelimited")
                .withQueryParam("query", "1|2|3")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void pipeDelimitedParam_shouldFail_whenTooManyItems() {
        final Request request = SimpleRequest.Builder
                .get("/style/pipeDelimited")
                .withQueryParam("query", "1|2|3|4")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.collection.tooManyItems");
    }

    @Test
    public void labelParam_inPath_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder
                .get("/style/label/.1.2.3")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void labelParam_inPath_shouldFail_whenTooManyItems() {
        final Request request = SimpleRequest.Builder
                .get("/style/label/.1.2.3.4")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.collection.tooManyItems");
    }

    @Test
    public void explodedMatrixParam_inPath_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder
                .get("/style/matrix/exploded/;param=1;param=2;param=3")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void explodedMatrixParam_inPath_shouldFail_whenTooManyItems() {
        final Request request = SimpleRequest.Builder
                .get("/style/matrix/exploded/;param=1;param=2;param=3;param=4")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.collection.tooManyItems");
    }

    @Test
    public void nonExplodedMatrixParam_inPath_shouldPass_whenValid() {
        final Request request = SimpleRequest.Builder
                .get("/style/matrix/nonexploded/;param=1,2,3")
                .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void nonExplodedMatrixParam_inPath_shouldFail_whenTooManyItems() {
        final Request request = SimpleRequest.Builder
                .get("/style/matrix/nonexploded/;param=1,2,3,4")
                .build();

        assertFail(classUnderTest.validateRequest(request),
                "validation.request.parameter.collection.tooManyItems");
    }
}
