package com.atlassian.oai.validator.examples.springweb.client;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.springweb.client.OpenApiValidationClientHttpRequestInterceptor;
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.Collections;
import java.util.Map;

import static com.atlassian.oai.validator.whitelist.rule.WhitelistRules.messageHasKey;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * An example that uses the {@link OpenApiValidationClientHttpRequestInterceptor} to validate request/response interactions
 * mediated by the Spring RestTemplate against a Swagger API specification.
 * <p>
 * The interceptor allows developers to test that their REST service implementation matches their API specification.
 * This is particularly useful when using a design-first approach where the implementation is separate from the
 * specification. However, even in cases where the specification is generated from the implementation this can
 * yield benefits, as a lot of the information in the specification comes from metadata applied to the implementation
 * (e.g. via annotations on the resource methods) which are not checked at compile time.
 *
 * @since 2.1
 */
public class OpenApiValidationClientHttpRequestInterceptorTestExample {

    private static final String SWAGGER_JSON_URL = "http://petstore.swagger.io/v2/swagger.json";
    private static final int PORT = 9999;

    private final OpenApiValidationClientHttpRequestInterceptor validationInterceptor = new OpenApiValidationClientHttpRequestInterceptor(
            OpenApiInteractionValidator.createForSpecificationUrl(SWAGGER_JSON_URL)
                    // Currently, this test doesn't verify the security definition of the petstore reference
                    .withWhitelist(ValidationErrorsWhitelist.create().withRule(
                            "Ignore missing security when getting store inventory",
                            messageHasKey("validation.request.security.missing")))
                    .build());
    private RestTemplate restTemplate;

    // Using wiremock to simulate a production service.
    // In a real-world use case you would call out to your service (e.g. in a Spring WebMVC test,
    // or to a service running in your TEST environment etc.)
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT);

    @Before
    public void setupWireMock() {
        wireMockRule.stubFor(
                WireMock.get(urlEqualTo("/pet/1"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("content-type", "application/json")
                                .withBody("{\"name\":\"fido\", \"photoUrls\":[]}")));

        wireMockRule.stubFor(
                WireMock.get(urlEqualTo("/pet/2"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("content-type", "application/json")
                                .withBody("{\"name\":\"fido\"}"))); // Missing required 'photoUrls' field
    }

    @Before
    public void setupRestTemplate() {
        restTemplate = new RestTemplate();
        restTemplate.setInterceptors(Collections.singletonList(validationInterceptor));
        final DefaultUriBuilderFactory handler = new DefaultUriBuilderFactory("http://localhost:" + PORT);
        restTemplate.setUriTemplateHandler(handler);
    }

    /**
     * Test a GET with a valid request/response
     * <p>
     * This test is expected to PASS
     */
    @Test
    public void testGetValidPet() {
        final ResponseEntity<Map> response = restTemplate.exchange("/pet/{0}", HttpMethod.GET, null, Map.class, 1);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

    /**
     * Test a GET with an invalid request/response expectation.
     * <p>
     * This test will pass the business logic tests, but the validation interceptor will fail the test because the
     * response received from the server doesn't match the schema defined in the API specification.
     * <p>
     * This could be due to a bug in the implementation, or a problem in the API specification.
     * Regardless - something is wrong and should be addressed.
     * <p>
     * This test is expected to FAIL
     */
    @Test
    public void testGetInvalidPet() {
        final ResponseEntity<Map> response = restTemplate.exchange("/pet/{0}", HttpMethod.GET, null, Map.class, 2);
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
    }

}
