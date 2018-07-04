package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.LevelResolver;
import org.junit.Test;

import static com.atlassian.oai.validator.model.Request.Method.GET;
import static com.atlassian.oai.validator.report.ValidationReport.Level.IGNORE;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertFail;
import static com.atlassian.oai.validator.util.ValidatorTestUtil.assertPass;

public class OpenAPIV3DiscriminatorMappingValidationTest {

    private final SwaggerRequestResponseValidator classUnderTest =
            SwaggerRequestResponseValidator
                    .createFor("/oai/v3/pet.yaml")
                    .withLevelResolver(
                            LevelResolver
                                    .create()
                                    .withLevel("validation.schema.additionalProperties", IGNORE)
                                    .build()
                    )
                    .build();

    @Test
    public void validate_withValidMappedType_shouldSucceed() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .withBody("{\"pet_type\": \"cachorro\", \"bark\": \"soft\"}")
                .build();
        assertPass(classUnderTest.validateResponse("/pets/1", GET, response));
    }

    @Test
    public void validate_withValidType_shouldSucceed() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .withBody("{\"pet_type\": \"Dog\", \"bark\": \"soft\"}")
                .build();
        assertPass(classUnderTest.validateResponse("/pets/1", GET, response));
    }

    @Test
    public void validate_withUnexpectedType_shouldFail() {
        final Response response = SimpleResponse.Builder
                .ok()
                .withContentType("application/json")
                .withBody("{\"pet_type\": \"Giraffe\", \"height\": 5}")
                .build();
        assertFail(classUnderTest.validateResponse("/pets/1", GET, response));
    }
}
