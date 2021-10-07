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
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OpenApiLoaderTest {

    private final OpenApiLoader classUnderTest = new OpenApiLoader();

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
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), new ParseOptions());

        // then:
        assertThat(result, notNullValue());
    }

    @Test
    public void loadApiBySpecUrl() {
        // given:
        final SpecSource specSource = mockSpecSource("/oai/v2/api-ref-params.json", true, false);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), new ParseOptions());

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
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), new ParseOptions());

        // then:
        assertThat(result, notNullValue());
    }

    @Test
    public void loadApiByUnknownSource_specUrl() {
        // given:
        final SpecSource specSource = mockSpecSource("/oai/v3/api-formdata.yaml", false, false);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), new ParseOptions());

        // then:
        assertThat(result, notNullValue());
    }

    @Test(expected = OpenApiInteractionValidator.ApiLoadException.class)
    public void errorOnLoadingApi_missingSpecUrl() {
        // given:
        final SpecSource specSource = mockSpecSource("missing.yaml", true, false);

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
    public void removesBase64RegexPatternFromLoadedApi_Swagger() throws JsonProcessingException {
        // given:
        final SpecSource specSource = mockSpecSource("/oai/v2/api-string-byte-pattern.json", true, false);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), new ParseOptions());

        // then:
        final String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result);
        assertThat(json, not(containsString("\"^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$\"")));
    }

    @Test
    public void removesBase64RegexPatternFromLoadedApi_OpenApi3() throws JsonProcessingException {
        // given:
        final SpecSource specSource = mockSpecSource("/oai/v3/api-string-byte-pattern.yaml", true, false);

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
        final SpecSource specSource = mockSpecSource("/oai/v3/api-oneof.yaml", true, false);
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
        Assert.assertNull(primitive.getType());

        final java.util.List<Schema> primitiveOneOfList = primitive.getOneOf();
        Assert.assertEquals(primitiveOneOfList.size(), 2);
        Assert.assertEquals(primitiveOneOfList.get(0).getType(), "string");
        Assert.assertEquals(primitiveOneOfList.get(1).getType(), "integer");

        final ComposedSchema objectModel = (ComposedSchema) oneOfObjectProperties.get("objectModel");
        Assert.assertNull(objectModel.getType());

        final java.util.List<Schema> objectModelOneOfList = objectModel.getOneOf();
        Assert.assertEquals(objectModelOneOfList.size(), 2);
        Assert.assertEquals(objectModelOneOfList.get(0).getType(), "object");
        Assert.assertEquals(objectModelOneOfList.get(1).getType(), "object");

        final ArraySchema oneOfArrayProperty = (ArraySchema) oneOfResponse.getProperties().get("oneOfArrayProperty");
        final ComposedSchema arrayItemOneOf = (ComposedSchema) oneOfArrayProperty.getItems();
        Assert.assertEquals(arrayItemOneOf.getOneOf().size(), 4);
        Assert.assertEquals(arrayItemOneOf.getOneOf().get(0).getType(), "object");
        Assert.assertEquals(arrayItemOneOf.getOneOf().get(1).getType(), "object");
        Assert.assertEquals(arrayItemOneOf.getOneOf().get(2).getType(), "string");
        Assert.assertEquals(arrayItemOneOf.getOneOf().get(3).getType(), "integer");
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void removeTypeObjectAssociationForAnyOfModel() {
        // given:
        final SpecSource specSource = mockSpecSource("/oai/v3/api-anyof.yaml", true, false);
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
        Assert.assertNull(primitive.getType());

        final java.util.List<Schema> primitiveAnyOfList = primitive.getAnyOf();
        Assert.assertEquals(primitiveAnyOfList.size(), 2);
        Assert.assertEquals(primitiveAnyOfList.get(0).getType(), "string");
        Assert.assertEquals(primitiveAnyOfList.get(1).getType(), "integer");

        final ComposedSchema objectModel = (ComposedSchema) anyOfObjectProperties.get("objectModel");
        Assert.assertNull(objectModel.getType());

        final java.util.List<Schema> objectModelAnyOfList = objectModel.getAnyOf();
        Assert.assertEquals(objectModelAnyOfList.size(), 2);
        Assert.assertEquals(objectModelAnyOfList.get(0).getType(), "object");
        Assert.assertEquals(objectModelAnyOfList.get(1).getType(), "object");

        final ArraySchema oneOfArrayProperty = (ArraySchema) anyOfResponse.getProperties().get("anyOfArrayProperty");
        final ComposedSchema arrayItemOneOf = (ComposedSchema) oneOfArrayProperty.getItems();
        Assert.assertEquals(arrayItemOneOf.getAnyOf().size(), 4);
        Assert.assertEquals(arrayItemOneOf.getAnyOf().get(0).getType(), "object");
        Assert.assertEquals(arrayItemOneOf.getAnyOf().get(1).getType(), "object");
        Assert.assertEquals(arrayItemOneOf.getAnyOf().get(2).getType(), "string");
        Assert.assertEquals(arrayItemOneOf.getAnyOf().get(3).getType(), "integer");
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void typeObjectAssociationForAllOfModelIsNull() {
        // given:
        final SpecSource specSource = mockSpecSource("/oai/v3/api-composition.yaml", true, false);
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(true);

        // when:
        final OpenAPI result = classUnderTest.loadApi(specSource, emptyList(), parseOptions);

        // then:
        final Map<String, Schema> schemas = result.getComponents().getSchemas();
        final Schema userResponse = schemas.get("User");
        Assert.assertNull(userResponse.getType());
    }
}
