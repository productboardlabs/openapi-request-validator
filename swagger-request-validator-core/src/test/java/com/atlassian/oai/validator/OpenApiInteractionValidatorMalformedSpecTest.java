package com.atlassian.oai.validator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;

public class OpenApiInteractionValidatorMalformedSpecTest {

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void givesReasonableError_whenEmptyString() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("A specification URL or payload is required");

        OpenApiInteractionValidator.createFor("").build();
    }

    @Test
    public void givesReasonableError_whenUnknownFile() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);

        OpenApiInteractionValidator.createFor("file://unknown").build();
    }

    @Test
    public void givesReasonableError_whenEmptyJson() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);

        OpenApiInteractionValidator.createFor("{}").build();
    }

    @Test
    public void givesReasonableError_whenMalformedJson() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);

        OpenApiInteractionValidator.createFor("{foo").build();
    }

    @Test
    public void givesReasonableError_whenInvalidOAI3() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);
        expected.expect(hasProperty("parseMessages", hasSize(1)));

        OpenApiInteractionValidator.createFor("/oai/v3/api-malformed.yaml").build();
    }

    @Test
    public void givesReasonableError_whenInvalidSwagger2() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);
        expected.expect(hasProperty("parseMessages", hasSize(1)));

        OpenApiInteractionValidator.createFor("/oai/v2/api-malformed.json").build();
    }

}
