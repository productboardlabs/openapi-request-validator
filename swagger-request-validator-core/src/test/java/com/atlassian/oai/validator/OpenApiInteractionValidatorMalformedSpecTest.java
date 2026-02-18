package com.atlassian.oai.validator;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OpenApiInteractionValidatorMalformedSpecTest {

    @Test
    public void givesReasonableError_whenEmptyString_whenUnknownSourceType() {
        final var exception = assertThrows(IllegalArgumentException.class, () ->
            OpenApiInteractionValidator.createFor("").build()
        );
        assert exception.getMessage().contains("A specification URL or payload is required");
    }

    @Test
    public void givesReasonableError_whenUnknownFile_whenUnknownSourceType() {
        assertThrows(OpenApiInteractionValidator.ApiLoadException.class, () ->
            OpenApiInteractionValidator.createFor("file://unknown").build()
        );
    }

    @Test
    public void givesReasonableError_whenEmptyJson_whenUnknownSourceType() {
        assertThrows(OpenApiInteractionValidator.ApiLoadException.class, () ->
            OpenApiInteractionValidator.createFor("{}").build()
        );
    }

    @Test
    public void givesReasonableError_whenMalformedJson_whenUnknownSourceType() {
        assertThrows(OpenApiInteractionValidator.ApiLoadException.class, () ->
            OpenApiInteractionValidator.createFor("{foo").build()
        );
    }

    @Test
    public void givesReasonableError_whenInvalidOAI3_whenUnknownSourceType() {
        final var exception = assertThrows(OpenApiInteractionValidator.ApiLoadException.class, () ->
            OpenApiInteractionValidator.createFor("/oai/v3/api-malformed.yaml").build()
        );
        assertThat(exception, hasProperty("parseMessages", hasSize(1)));
    }

    @Test
    public void givesReasonableError_whenInvalidSwagger2_whenUnknownSourceType() {
        final var exception = assertThrows(OpenApiInteractionValidator.ApiLoadException.class, () ->
            OpenApiInteractionValidator.createFor("/oai/v2/api-malformed.json").build()
        );
        assertThat(exception, hasProperty("parseMessages", hasSize(1)));
    }

    @Test
    public void givesReasonableError_whenEmptyString_whenUrlSource() {
        final var exception = assertThrows(IllegalArgumentException.class, () ->
            OpenApiInteractionValidator.createForSpecificationUrl("").build()
        );
        assert exception.getMessage().contains("A specification URL is required");
    }

    @Test
    public void givesReasonableError_whenEmptyString_whenInlineSource() {
        final var exception = assertThrows(IllegalArgumentException.class, () ->
            OpenApiInteractionValidator.createForInlineApiSpecification("").build()
        );
        assert exception.getMessage().contains("A specification payload is required");
    }

    @Test
    public void givesReasonableError_whenUnknownFile() {
        assertThrows(OpenApiInteractionValidator.ApiLoadException.class, () ->
            OpenApiInteractionValidator.createForSpecificationUrl("file:/unknown.txt").build()
        );
    }

    @Test
    public void givesReasonableError_whenEmptyJson() {
        assertThrows(OpenApiInteractionValidator.ApiLoadException.class, () ->
            OpenApiInteractionValidator.createForInlineApiSpecification("{}").build()
        );
    }

    @Test
    public void givesReasonableError_whenMalformedJson() {
        assertThrows(OpenApiInteractionValidator.ApiLoadException.class, () ->
            OpenApiInteractionValidator.createForInlineApiSpecification("{foo").build()
        );
    }

    @Test
    public void givesReasonableError_whenInvalidOAI3() {
        final var exception = assertThrows(OpenApiInteractionValidator.ApiLoadException.class, () ->
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v3/api-malformed.yaml").build()
        );
        assertThat(exception, hasProperty("parseMessages", hasSize(1)));
    }

    @Test
    public void givesReasonableError_whenInvalidSwagger2() {
        final var exception = assertThrows(OpenApiInteractionValidator.ApiLoadException.class, () ->
            OpenApiInteractionValidator.createForSpecificationUrl("/oai/v2/api-malformed.json").build()
        );
        assertThat(exception, hasProperty("parseMessages", hasSize(1)));
    }

}
