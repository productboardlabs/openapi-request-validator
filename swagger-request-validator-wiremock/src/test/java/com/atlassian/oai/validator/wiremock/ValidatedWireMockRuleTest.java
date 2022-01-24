package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Test;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.runners.model.FrameworkMethod;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.RestAssured.get;

public class ValidatedWireMockRuleTest {

    private static final String API_PATH = "/hello/bob?queryParam=foo";
    private static final String VALID_RESPONSE_BODY = "{\"message\":\"Hello bob!\"}";
    private static final String INVALID_RESPONSE_BODY = "{\"msg\":\"Hello bob!\"}";

    private ValidatedWireMockRule classUnderTest;

    @Test
    public void canBeCreatedWithAnExistingValidator() {
        new ValidatedWireMockRule(
                OpenApiInteractionValidator.createForSpecificationUrl("api-oai3.yaml").build(),
                options().dynamicPort()
        );
    }

    @Test
    public void shouldPass_withValidInteraction_whenSwaggerv2() throws Throwable {
        classUnderTest = new ValidatedWireMockRule("api-swagger2.json", options().dynamicPort());
        classUnderTest.apply(getValidInteractionTestMethod(), null).evaluate();
    }

    @Test(expected = OpenApiValidationListener.OpenApiValidationException.class)
    public void shouldFail_withInvalidInteraction_whenSwaggerv2() throws Throwable {
        classUnderTest = new ValidatedWireMockRule("api-swagger2.json", options().dynamicPort());
        classUnderTest.apply(getInvalidInteractionTestMethod(), null).evaluate();
    }

    @Test
    public void shouldPass_withValidInteraction_whenOpenApi3() throws Throwable {
        classUnderTest = new ValidatedWireMockRule("api-oai3.yaml", options().dynamicPort());
        classUnderTest.apply(getValidInteractionTestMethod(), null).evaluate();
    }

    @Test(expected = OpenApiValidationListener.OpenApiValidationException.class)
    public void shouldFail_withInvalidInteraction_whenOpenApi3() throws Throwable {
        classUnderTest = new ValidatedWireMockRule("api-oai3.yaml", options().dynamicPort());
        classUnderTest.apply(getInvalidInteractionTestMethod(), null).evaluate();
    }

    public void validInteractionTestMethod() {
        setupStubWithBody(VALID_RESPONSE_BODY);
        get(getPath()).then().assertThat().statusCode(200);
    }

    public void invalidInteractionTestMethod() {
        setupStubWithBody(INVALID_RESPONSE_BODY);
        get(getPath()).then().assertThat().statusCode(200);
    }

    private void setupStubWithBody(final String responseBody) {
        classUnderTest.stubFor(
                WireMock.any(urlEqualTo(API_PATH))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("content-type", "application/json")
                                .withBody(responseBody))
        );
    }

    private String getPath() {
        return "http://localhost:" + classUnderTest.port() + API_PATH;
    }

    private InvokeMethod getValidInteractionTestMethod() throws NoSuchMethodException {
        return getTestMethod("validInteractionTestMethod");
    }

    private InvokeMethod getInvalidInteractionTestMethod() throws NoSuchMethodException {
        return getTestMethod("invalidInteractionTestMethod");
    }

    private InvokeMethod getTestMethod(final String methodName) throws NoSuchMethodException {
        final FrameworkMethod testMethod = new FrameworkMethod(getClass().getMethod(methodName));
        return new InvokeMethod(testMethod, this);
    }

}
