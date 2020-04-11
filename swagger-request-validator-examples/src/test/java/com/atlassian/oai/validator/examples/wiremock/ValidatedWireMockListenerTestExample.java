package com.atlassian.oai.validator.examples.wiremock;

import com.atlassian.oai.validator.wiremock.OpenApiValidationListener;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * An example test that uses the {@link OpenApiValidationListener} to validate WireMock interactions
 * against a Swagger API specification.
 * <p>
 * This allows developers to have confidence that the mocks you are setting up in your tests reflect reality. It also
 * gives early (unit-test level) feedback if a breaking change is made to a provider's API, allowing you to
 * respond accordingly.
 *
 * @see ValidatedWireMockRuleTestExample
 * @see <a href="http://wiremock.org/">WireMock</a>
 */
public class ValidatedWireMockListenerTestExample {

    private static final String SWAGGER_JSON_URL = "http://petstore.swagger.io/v2/swagger.json";
    private static final int PORT = 9999;
    private static final String WIREMOCK_URL = "http://localhost:" + PORT;

    @Rule
    public WireMockRule wireMockRule;
    private final OpenApiValidationListener validationListener;

    public ValidatedWireMockListenerTestExample() {
        validationListener = new OpenApiValidationListener(SWAGGER_JSON_URL);

        wireMockRule = new WireMockRule(PORT);
        wireMockRule.addMockServiceRequestListener(validationListener);
    }

    @After
    public void teardown() {
        validationListener.reset();
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

        final Response response = given().header("api_key", "foo").get(WIREMOCK_URL + "/pet/1");

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

        final Response response = given().header("api_key", "foo").get(WIREMOCK_URL + "/pet/1");

        assertThat(response.getStatusCode(), is(200));
        validationListener.assertValidationPassed();
    }

}
