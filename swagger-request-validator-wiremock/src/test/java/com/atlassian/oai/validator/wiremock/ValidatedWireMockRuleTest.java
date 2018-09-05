package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.runners.model.FrameworkMethod;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.get;

public class ValidatedWireMockRuleTest {

    private static final String API_PATH = "/hello/bob";
    private static final String VALID_RESPONSE_BODY = "{\"message\":\"Hello bob!\"}";
    private static final String INVALID_RESPONSE_BODY = "{\"msg\":\"Hello bob!\"}";

    private ValidatedWireMockRule classUnderTest;

    @Before
    public void setup() {
        classUnderTest = new ValidatedWireMockRule("api.json",
                WireMockConfiguration.options().dynamicPort());
    }

    @Test
    public void canBeCreatedWithAnExistingValidator() {
        new ValidatedWireMockRule(
                OpenApiInteractionValidator.createFor("api.json").build(),
                WireMockConfiguration.options().dynamicPort()
        );
    }

    @Test
    public void testWithValidInteraction() throws Throwable {
        classUnderTest.apply(getValidInteractionTestMethod(), null, null).evaluate();
    }

    @Test(expected = OpenApiValidationListener.OpenApiValidationException.class)
    public void testWithInvalidInteraction() throws Throwable {
        classUnderTest.apply(getInvalidInteractionTestMethod(), null, null).evaluate();
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
