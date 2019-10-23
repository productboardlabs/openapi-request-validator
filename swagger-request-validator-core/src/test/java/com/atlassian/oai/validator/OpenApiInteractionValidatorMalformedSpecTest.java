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
    public void givesReasonableError_whenEmptyString_whenUnknownSourceType() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("A specification URL or payload is required");

        OpenApiInteractionValidator.createFor("").build();
    }

    @Test
    public void givesReasonableError_whenUnknownFile_whenUnknownSourceType() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);

        OpenApiInteractionValidator.createFor("file://unknown").build();
    }

    @Test
    public void givesReasonableError_whenEmptyJson_whenUnknownSourceType() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);

        OpenApiInteractionValidator.createFor("{}").build();
    }

    @Test
    public void givesReasonableError_whenMalformedJson_whenUnknownSourceType() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);

        OpenApiInteractionValidator.createFor("{foo").build();
    }

    @Test
    public void givesReasonableError_whenInvalidOAI3_whenUnknownSourceType() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);
        expected.expect(hasProperty("parseMessages", hasSize(1)));

        OpenApiInteractionValidator.createFor("/oai/v3/api-malformed.yaml").build();
    }

    @Test
    public void givesReasonableError_whenInvalidSwagger2_whenUnknownSourceType() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);
        expected.expect(hasProperty("parseMessages", hasSize(1)));

        OpenApiInteractionValidator.createFor("/oai/v2/api-malformed.json").build();
    }

    @Test
    public void givesReasonableError_whenEmptyString_whenUrlSource() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("A specification URL is required");

        OpenApiInteractionValidator.createForSpecificationUrl("").build();
    }

    @Test
    public void givesReasonableError_whenEmptyString_whenInlineSource() {
        expected.expect(IllegalArgumentException.class);
        expected.expectMessage("A specification payload is required");

        OpenApiInteractionValidator.createForInlineApiSpecification("").build();
    }

    @Test
    public void givesReasonableError_whenUnknownFile() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);

        OpenApiInteractionValidator.createForSpecificationUrl("file:/unknown.txt").build();
    }

    @Test
    public void givesReasonableError_whenEmptyJson() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);

        OpenApiInteractionValidator.createForInlineApiSpecification("{}").build();
    }

    @Test
    public void givesReasonableError_whenMalformedJson() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);

        OpenApiInteractionValidator.createForInlineApiSpecification("{foo").build();
    }

    @Test
    public void givesReasonableError_whenInvalidOAI3() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);
        expected.expect(hasProperty("parseMessages", hasSize(1)));

        OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-malformed.yaml").build();
    }

    @Test
    public void givesReasonableError_whenInvalidSwagger2() {
        expected.expect(OpenApiInteractionValidator.ApiLoadException.class);
        expected.expect(hasProperty("parseMessages", hasSize(1)));

        OpenApiInteractionValidator.createForSpecificationUrl("/oai/v2/api-malformed.json").build();
    }

}
