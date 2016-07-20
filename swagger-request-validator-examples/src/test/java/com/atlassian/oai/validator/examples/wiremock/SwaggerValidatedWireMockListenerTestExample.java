package com.atlassian.oai.validator.examples.wiremock;

import com.atlassian.oai.validator.wiremock.SwaggerValidationListener;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.jayway.restassured.response.Response;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.jayway.restassured.RestAssured.get;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * An example test that uses the {@link SwaggerValidationListener} to validate WireMock interactions
 * against a Swagger API specification.
 * <p>
 * This allows developers to have confidence that the mocks you are setting up in your tests reflect reality. It also
 * gives early (unit-test level) feedback if a breaking change is made to a provider's API, allowing you to
 * respond accordingly.
 *
 * @see SwaggerValidatedWireMockRuleTestExample
 * @see <a href="http://wiremock.org/">WireMock</a>
 */
public class SwaggerValidatedWireMockListenerTestExample {

    private static final String SWAGGER_JSON_URL = "http://petstore.swagger.io/v2/swagger.json";
    private static final int PORT = 9999;
    private static final String WIREMOCK_URL = "http://localhost:" + PORT;

    @Rule
    public WireMockRule wireMockRule;
    private SwaggerValidationListener validationListener;

    public SwaggerValidatedWireMockListenerTestExample() {
        this.validationListener = new SwaggerValidationListener(SWAGGER_JSON_URL);

        this.wireMockRule = new WireMockRule(PORT);
        this.wireMockRule.addMockServiceRequestListener(validationListener);
    }

    @After
    public void teardown() {
        this.validationListener.reset();
    }

    /**
     * Test a GET with a valid request/response expectation.
     * <p>
     * This test will pass both the (contrived) business logic tests and the Swagger validation.
     */
    @Test
    public void testGetValidPet() {
        wireMockRule.stubFor(
                WireMock.get(urlEqualTo("/pet/1"))
                        .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("content-type", "application/json")
                            .withBody("{\"name\":\"fido\", \"photoUrls\":[]}")));

        final Response response = get(WIREMOCK_URL + "/pet/1");
        assertThat(response.getStatusCode(), is(200));
        validationListener.assertValidationPassed();
    }

    /**
     * Test a GET with an invalid request/response expectation.
     * <p>
     * This test will pass the business logic tests, but will fail because the expectations encoded
     * in the WireMock stubs do not match the API specification defined in the Swagger spec.
     */
    @Test
    public void testGetInvalidPet() {
        wireMockRule.stubFor(
                WireMock.get(urlEqualTo("/pet/1"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("content-type", "application/json")
                                .withBody("{\"name\":\"fido\"}"))); // Missing required 'photoUrls' field

        final Response response = get(WIREMOCK_URL + "/pet/1");
        assertThat(response.getStatusCode(), is(200));
        validationListener.assertValidationPassed();
    }

}
