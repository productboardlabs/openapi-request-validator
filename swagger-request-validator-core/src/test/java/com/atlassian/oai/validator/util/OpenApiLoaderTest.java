package com.atlassian.oai.validator.util;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.OpenApiInteractionValidator.SpecSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenApiLoaderTest {

    private OpenApiLoader classUnderTest = new OpenApiLoader();

    private static SpecSource mockSpecSource(final String value, final boolean isSpecUrl, final boolean isInlineSpec) {
        final SpecSource specSource = mock(SpecSource.class);
        when(specSource.isSpecUrl()).thenReturn(isSpecUrl);
        when(specSource.isInlineSpecification()).thenReturn(isInlineSpec);
        when(specSource.getValue()).thenReturn(value);
        return specSource;
    }

    @Test
    public void loadApiByInlineSpecification() throws IOException {
        // given:
        final String inlineSpec = IOUtils.toString(
                this.getClass().getResourceAsStream("/oai/v3/api-complex-composition.yaml"));
        final SpecSource specSource = mockSpecSource(inlineSpec, false, true);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList());

        // then:
        assertThat(result, notNullValue());
    }

    @Test
    public void loadApiBySpecUrl() {
        // given:
        final SpecSource specSource = mockSpecSource("/oai/v2/api-ref-params.json", true, false);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList());

        // then:
        assertThat(result, notNullValue());
    }

    @Test
    public void loadApiByUnknownSource_inlineSpecification() throws IOException {
        // given:
        final String inlineSpec = IOUtils.toString(
                this.getClass().getResourceAsStream("/oai/v2/api-users.json"));
        final SpecSource specSource = mockSpecSource(inlineSpec, false, false);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList());

        // then:
        assertThat(result, notNullValue());
    }

    @Test
    public void loadApiByUnknownSource_specUrl() {
        // given:
        final SpecSource specSource = mockSpecSource("/oai/v3/api-formdata.yaml", false, false);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList());

        // then:
        assertThat(result, notNullValue());
    }

    @Test(expected = OpenApiInteractionValidator.ApiLoadException.class)
    public void errorOnLoadingApi_missingSpecUrl() {
        // given:
        final SpecSource specSource = mockSpecSource("missing.yaml", true, false);

        // expect:
        classUnderTest.loadApi(specSource, emptyList());
    }

    @Test(expected = OpenApiInteractionValidator.ApiLoadException.class)
    public void errorOnLoadingApi_exception() {
        // given:
        final SpecSource specSource = mock(SpecSource.class);
        when(specSource.isSpecUrl()).thenThrow(new NullPointerException("Unexpected"));
        when(specSource.getValue()).thenReturn("spec.url");

        // expect:
        classUnderTest.loadApi(specSource, emptyList());
    }

    @Test
    public void removesBase64RegexPatternFromLoadedApi_Swagger() throws JsonProcessingException {
        // given:
        final SpecSource specSource = mockSpecSource("/oai/v2/api-string-byte-pattern.json", true, false);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList());

        // then:
        final String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result);
        assertThat(json, not(containsString("\"^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$\"")));
    }

    @Test
    public void removesBase64RegexPatternFromLoadedApi_OpenApi3() throws JsonProcessingException {
        // given:
        final SpecSource specSource = mockSpecSource("/oai/v3/api-string-byte-pattern.yaml", true, false);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList());

        // then:
        final String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result);
        assertThat(json, not(containsString("\"^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$\"")));
    }
}
