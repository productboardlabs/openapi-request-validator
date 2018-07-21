package com.atlassian.oai.validator.examples.wiremock;

import com.atlassian.oai.validator.wiremock.ValidatedWireMockRule;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.restassured.response.Response;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * An example test that uses the {@link ValidatedWireMockRule} to validate WireMock interactions
 * against a Swagger API specification.
 * <p>
 * The {@link ValidatedWireMockRule} is a near drop-in replacement for the existing
 * {@link com.github.tomakehurst.wiremock.junit.WireMockRule} that adds validation of the request/response
 * interactions against the provided Swagger API specification.
 * <p>
 * This allows developers to have confidence that the mocks you are setting up in your tests reflect reality. It also
 * gives early (unit-test level) feedback if a breaking change is made to a provider's API, allowing you to
 * respond accordingly.
 *
 * @see ValidatedWireMockListenerTestExample
 * @see <a href="http://wiremock.org/">WireMock</a>
 */
public class ValidatedWireMockRuleTestExample {

    private static final String SWAGGER_JSON_URL = "http://petstore.swagger.io/v2/swagger.json";
    private static final int PORT = 9999;
    private static final String WIREMOCK_URL = "http://localhost:" + PORT;

    @Rule
    public ValidatedWireMockRule wireMockRule = new ValidatedWireMockRule(SWAGGER_JSON_URL, PORT);

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

        final Response response =
                given()
                    .header("API_Key", "foobar")
                    .get(WIREMOCK_URL + "/pet/1");
        assertThat(response.getStatusCode(), is(200));
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

        final Response response =
                given()
                    .header("API_Key", "foobar")
                    .get(WIREMOCK_URL + "/pet/1");
        assertThat(response.getStatusCode(), is(200));
    }

    /**
     * Test a POST to the endpoint that consumes multipart/form-data
     */
    @Test
    public void testMultipartFormdata() {
        wireMockRule.stubFor(
                WireMock.post(urlEqualTo("/pet/1/uploadImage"))
                        .willReturn(aResponse()
                                .withHeader("content-type", "application/json")
                                .withStatus(200)
                                .withBody("{}")));

        final Response response =
                given()
                        .multiPart("additionalMetadata", "foobar")
                        .multiPart("file", "somefile")
                        .post(WIREMOCK_URL + "/pet/1/uploadImage");
        assertThat(response.getStatusCode(), is(200));
    }

}
