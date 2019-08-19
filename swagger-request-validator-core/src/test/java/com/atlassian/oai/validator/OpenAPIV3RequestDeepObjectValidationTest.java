package com.atlassian.oai.validator;

import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport.Level;

import org.junit.Test;

public class OpenAPIV3RequestDeepObjectValidationTest {

    @Test
    public void validate_withDeepObjectParameters_shouldPass() {

        LevelResolver.Builder resolver = new LevelResolver.Builder();
        resolver.withLevel("validation.request.parameter.query.unexpected", Level.ERROR);

        OpenApiInteractionValidator classUnderTest =
            OpenApiInteractionValidator
                .createFor("/oai/v3/api-with-deepobject-param.yaml")
                .withLevelResolver(resolver.build())
                .build();

        final Request request = SimpleRequest.Builder
            .get("/users")
            .withQueryParam("filter[name_eq]", "alex")
            .withQueryParam("filter[email_eq]", "alex@mycompany.com")
            .build();

        assertPass(classUnderTest.validateRequest(request));
    }
}