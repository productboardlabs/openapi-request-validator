package com.atlassian.oai.validator.util;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.OpenApiInteractionValidator.SpecSource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenApiLoaderTest {

    private final OpenApiLoader classUnderTest = new OpenApiLoader();

    @Test
    public void loadApiByInlineSpecification() throws IOException {
        // given:
        final String inlineSpec = IOUtils.toString(
                this.getClass().getResourceAsStream("/oai/v3/api-complex-composition.yaml"), defaultCharset());
        final SpecSource specSource = SpecSource.inline(inlineSpec);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), new ParseOptions());

        // then:
        assertThat(result, notNullValue());
    }

    @Test
    public void loadApiBySpecUrl() {
        // given:
        final SpecSource specSource = SpecSource.specUrl("/oai/v2/api-ref-params.json");

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), new ParseOptions());

        // then:
        assertThat(result, notNullValue());
    }

    @Test
    public void loadApiByUnknownSource_inlineSpecification() throws IOException {
        // given:
        final String inlineSpec = IOUtils.toString(
                this.getClass().getResourceAsStream("/oai/v2/api-users.json"), defaultCharset());
        final SpecSource specSource = SpecSource.inline(inlineSpec);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), new ParseOptions());

        // then:
        assertThat(result, notNullValue());
    }

    @Test
    public void loadApiByUnknownSource_specUrl() {
        // given:
        final SpecSource specSource = SpecSource.unknown("/oai/v3/api-formdata.yaml");

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), new ParseOptions());

        // then:
        assertThat(result, notNullValue());
    }

    @Test(expected = OpenApiInteractionValidator.ApiLoadException.class)
    public void errorOnLoadingApi_missingSpecUrl() {
        // given:
        final SpecSource specSource = SpecSource.specUrl("missing.yaml");

        // expect:
        classUnderTest.loadApi(specSource, emptyList(), new ParseOptions());
    }

    @Test(expected = OpenApiInteractionValidator.ApiLoadException.class)
    public void errorOnLoadingApi_exception() {
        // given:
        final SpecSource specSource = mock(SpecSource.class);
        when(specSource.isSpecUrl()).thenThrow(new NullPointerException("Unexpected"));
        when(specSource.getValue()).thenReturn("spec.url");

        // expect:
        classUnderTest.loadApi(specSource, emptyList(), new ParseOptions());
    }

    @Test
    @Ignore("This test fails due to overzealous regex check")
    // The Schema#jsonSchema is being set with the original data when loading, which fails the regex check
    // Need to find a way to exclude or filter this too when we eventually start using the node
    public void removesBase64RegexPatternFromLoadedApi_Swagger() throws JsonProcessingException {
        // given:
        final SpecSource specSource = SpecSource.specUrl("/oai/v2/api-string-byte-pattern.json");

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), new ParseOptions());

        // then:
        final String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result);
        assertThat(json, not(containsString("\"^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$\"")));
    }

    @Test
    public void removesBase64RegexPatternFromLoadedApi_OpenApi3() throws JsonProcessingException {
        // given:
        final SpecSource specSource = SpecSource.specUrl("/oai/v3/api-string-byte-pattern.yaml");

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), new ParseOptions());

        // then:
        final String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result);
        assertThat(json, not(containsString("\"^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$\"")));
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void removeTypeObjectAssociationForOneOfModel() throws JsonProcessingException {
        // given:
        final SpecSource specSource = SpecSource.specUrl("/oai/v3/api-oneof.yaml");
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(true);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), parseOptions);

        // then:
        final Map<String, Schema> schemas = result.getComponents().getSchemas();
        final ObjectSchema oneOfResponse = (ObjectSchema) schemas.get("oneOfResponse");
        final ObjectSchema oneOfObjectProperty = (ObjectSchema) oneOfResponse.getProperties().get("oneOfObjectProperty");
        final Map<String, Schema> oneOfObjectProperties = oneOfObjectProperty.getProperties();

        final ComposedSchema primitive = (ComposedSchema) oneOfObjectProperties.get("primitive");
        assertNull(primitive.getType());

        final java.util.List<Schema> primitiveOneOfList = primitive.getOneOf();
        assertEquals(primitiveOneOfList.size(), 2);
        assertEquals(primitiveOneOfList.get(0).getType(), "string");
        assertEquals(primitiveOneOfList.get(1).getType(), "integer");

        final ComposedSchema objectModel = (ComposedSchema) oneOfObjectProperties.get("objectModel");
        assertNull(objectModel.getType());

        final java.util.List<Schema> objectModelOneOfList = objectModel.getOneOf();
        assertEquals(objectModelOneOfList.size(), 2);
        assertEquals(objectModelOneOfList.get(0).getType(), "object");
        assertEquals(objectModelOneOfList.get(1).getType(), "object");

        final ArraySchema oneOfArrayProperty = (ArraySchema) oneOfResponse.getProperties().get("oneOfArrayProperty");
        final ComposedSchema arrayItemOneOf = (ComposedSchema) oneOfArrayProperty.getItems();
        assertEquals(arrayItemOneOf.getOneOf().size(), 4);
        assertEquals(arrayItemOneOf.getOneOf().get(0).getType(), "object");
        assertEquals(arrayItemOneOf.getOneOf().get(1).getType(), "object");
        assertEquals(arrayItemOneOf.getOneOf().get(2).getType(), "string");
        assertEquals(arrayItemOneOf.getOneOf().get(3).getType(), "integer");
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void removeTypeObjectAssociationForAnyOfModel() {
        // given:
        final SpecSource specSource = SpecSource.specUrl("/oai/v3/api-anyof.yaml");
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(true);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), parseOptions);

        // then:
        final Map<String, Schema> schemas = result.getComponents().getSchemas();
        final ObjectSchema anyOfResponse = (ObjectSchema) schemas.get("anyOfResponse");
        final ObjectSchema anyOfObjectProperty = (ObjectSchema) anyOfResponse.getProperties().get("anyOfObjectProperty");
        final Map<String, Schema> anyOfObjectProperties = anyOfObjectProperty.getProperties();

        final ComposedSchema primitive = (ComposedSchema) anyOfObjectProperties.get("primitive");
        assertNull(primitive.getType());

        final java.util.List<Schema> primitiveAnyOfList = primitive.getAnyOf();
        assertEquals(primitiveAnyOfList.size(), 2);
        assertEquals(primitiveAnyOfList.get(0).getType(), "string");
        assertEquals(primitiveAnyOfList.get(1).getType(), "integer");

        final ComposedSchema objectModel = (ComposedSchema) anyOfObjectProperties.get("objectModel");
        assertNull(objectModel.getType());

        final java.util.List<Schema> objectModelAnyOfList = objectModel.getAnyOf();
        assertEquals(objectModelAnyOfList.size(), 2);
        assertEquals(objectModelAnyOfList.get(0).getType(), "object");
        assertEquals(objectModelAnyOfList.get(1).getType(), "object");

        final ArraySchema oneOfArrayProperty = (ArraySchema) anyOfResponse.getProperties().get("anyOfArrayProperty");
        final ComposedSchema arrayItemOneOf = (ComposedSchema) oneOfArrayProperty.getItems();
        assertEquals(arrayItemOneOf.getAnyOf().size(), 4);
        assertEquals(arrayItemOneOf.getAnyOf().get(0).getType(), "object");
        assertEquals(arrayItemOneOf.getAnyOf().get(1).getType(), "object");
        assertEquals(arrayItemOneOf.getAnyOf().get(2).getType(), "string");
        assertEquals(arrayItemOneOf.getAnyOf().get(3).getType(), "integer");
    }

}
