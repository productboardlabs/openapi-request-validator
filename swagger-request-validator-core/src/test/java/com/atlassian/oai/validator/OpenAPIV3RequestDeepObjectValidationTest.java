package com.atlassian.oai.validator;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.LevelResolver;

import org.junit.Test;

public class OpenAPIV3RequestDeepObjectValidationTest {

    // Use an empty resolver - so we can ensure that any issue is raised 
    // correctly as an error and we don't miss anything.
    private LevelResolver allErrorsResolver = LevelResolver.create()
        .build();

    @Test
    public void validate_withDeepObjectParameters_shouldPass() {

        final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator
                .createFor("/oai/v3/api-with-deepobject-param.yaml")
                .withLevelResolver(allErrorsResolver)
                .build();

        final Request request = SimpleRequest.Builder
            .get("/users")
            .withQueryParam("filter[name_eq]", "Alex")
            .build();

        assertPass(classUnderTest.validateRequest(request));
    }

    @Test
    public void validate_withMissingRequiredValueDeepObjectParameter_shouldFail() {

        final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator
                .createFor("/oai/v3/api-with-deepobject-param.yaml")
                .withLevelResolver(allErrorsResolver)
                .build();

        final Request request = SimpleRequest.Builder
            .get("/users")
            .withQueryParam("filter[email_eq]", "alex.stevens@gmail.com")
            .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.schema.required");
    }

    @Test
    public void validate_withDeepObjectParameterNotRequired_shouldPass() {

        final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator
                .createFor("/oai/v3/api-with-deepobject-param.yaml")
                .withLevelResolver(allErrorsResolver)
                .build();

        final Request request = SimpleRequest.Builder
            .get("/users")
            .build();

        assertPass(classUnderTest.validateRequest(request));
    }
    
    @Test
    public void validate_withAdditionalDeepObjectParameter_shouldFail() {

        final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator
                .createFor("/oai/v3/api-with-deepobject-param.yaml")
                .withLevelResolver(allErrorsResolver)
                .build();

        final Request request = SimpleRequest.Builder
            .get("/users")
            .withQueryParam("filter[name_eq]", "Alex")
            .withQueryParam("filter[notavalid]", "value")
            .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.schema.additionalProperties");
    }

    @Test
    public void validate_withUnexpectedEnumDeepObjectParameter_shouldFail() {

        final OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator
                .createFor("/oai/v3/api-with-deepobject-param.yaml")
                .withLevelResolver(allErrorsResolver)
                .build();

        final Request request = SimpleRequest.Builder
            .get("/users")
            .withQueryParam("filter[name_eq]", "Alex")
            .withQueryParam("filter[status_eq]", "notvalid")
            .build();

        assertFail(classUnderTest.validateRequest(request), "validation.request.parameter.schema.enum");
    }

}