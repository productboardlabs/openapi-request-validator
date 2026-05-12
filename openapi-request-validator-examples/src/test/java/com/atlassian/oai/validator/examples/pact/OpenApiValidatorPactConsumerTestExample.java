package com.atlassian.oai.validator.examples.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.atlassian.oai.validator.pact.IgnoreApiValidation;
import com.atlassian.oai.validator.pact.ValidatedPactConsumerTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.restassured.RestAssured.given;

/**
 * An example Pact Consumer test that shows use of the {@link ValidatedPactConsumerTestExtension} to apply
 * OpenAPI / Swagger validation to Pact interactions.
 * <p>
 * This gives very fast feedback if a consumer's expectations do not match the API specification, without the
 * need to execute the Pacts against the Provider.
 * <p>
 * <b>Note:</b> It's still a good idea to run the Provider side of the Pact interaction; this validation helps
 * catch a class of problems sooner.
 *
 * @see <a href="https://docs.pact.io/">Pact</a>
 * @see <a href="https://github.com/pact-foundation/pact-jvm">Pact-jvm</a>
 */
@PactTestFor(providerName = OpenApiValidatorPactConsumerTestExample.PROVIDER_ID)
public class OpenApiValidatorPactConsumerTestExample {

    public static final String PROVIDER_ID = "Petstore";
    public static final String CONSUMER_ID = "ExampleConsumer";
    public static final String SWAGGER_JSON_URL = "https://petstore.swagger.io/v2/swagger.json";

    /**
     * Validated Pact consumer extension — wraps the standard Pact mock server with OpenAPI validation.
     * Each pact interaction is validated against the spec before the test body executes.
     */
    @RegisterExtension
    static final ValidatedPactConsumerTestExtension PROVIDER =
            new ValidatedPactConsumerTestExtension(SWAGGER_JSON_URL, null);

    @Pact(provider = PROVIDER_ID, consumer = CONSUMER_ID)
    public V4Pact getValidPet(final PactBuilder builder) {
        return builder
                .usingLegacyDsl()
                .uponReceiving("GET valid pet")
                .method("GET")
                .path("/pet/1")
                .matchHeader("api_key", ".*")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody().stringValue("name", "fido").array("photoUrls").closeArray().asBody())
                .toPact(V4Pact.class);
    }

    @Pact(provider = PROVIDER_ID, consumer = CONSUMER_ID)
    public V4Pact getPetWithIncompleteResponse(final PactBuilder builder) {
        return builder
                .usingLegacyDsl()
                .uponReceiving("GET pet with incomplete response")
                .method("GET")
                .path("/pet/2")
                .matchHeader("api_key", ".*")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        // Response missing required field "photoUrls".
                        // API validation is lenient about missing fields in the Pact context and will succeed.
                        .stringValue("name", "fido")
                )
                .toPact(V4Pact.class);
    }

    @Pact(provider = PROVIDER_ID, consumer = CONSUMER_ID)
    public V4Pact getPetWithInvalidResponse(final PactBuilder builder) {
        return builder
                .usingLegacyDsl()
                .uponReceiving("GET pet with invalid response")
                .method("GET")
                .path("/pet/3")
                .matchHeader("api_key", ".*")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .stringValue("name", "fido")
                        // Response has the incorrect type for the 'id' field (string instead of integer).
                        // API validation will fail.
                        .stringType("id", "fido01")
                )
                .toPact(V4Pact.class);
    }

    @Pact(provider = PROVIDER_ID, consumer = CONSUMER_ID)
    public V4Pact getPetWithInvalidId(final PactBuilder builder) {
        return builder
                .usingLegacyDsl()
                .uponReceiving("GET pet with invalid ID")
                .method("GET")
                .path("/pet/a")
                .matchHeader("api_key", ".*")
                .willRespondWith()
                .status(400)
                .toPact(V4Pact.class);
    }

    @Pact(provider = PROVIDER_ID, consumer = CONSUMER_ID)
    public V4Pact getPetWithAdditionalProperties(final PactBuilder builder) {
        return builder
                .usingLegacyDsl()
                .uponReceiving("GET pet with additional properties")
                .method("GET")
                .path("/pet/4")
                .matchHeader("api_key", ".*")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .stringValue("name", "fido")
                        .numberValue("extra", 33)
                        .array("photoUrls").closeArray()
                        .asBody())
                .toPact(V4Pact.class);
    }

    /**
     * Test a GET with a valid expectation about the response payload.
     * <p>
     * This is expected to PASS both API validation and Pact execution.
     */
    @Test
    @PactTestFor(pactMethod = "getValidPet")
    public void testGetValidPet(final MockServer mockServer) {
        given()
                .header("api_key", "some-api-key")
                .get(mockServer.getUrl() + "/pet/1");
    }

    /**
     * Test a GET with an expectation that does not specify all the required fields in the response payload.
     * <p>
     * Usually this would fail as a required field is missing. However, in keeping with the Pact philosophy of
     * only specifying fields the client cares about, the extension is lenient regarding missing response fields.
     * This behaviour can be overridden — see {@link com.atlassian.oai.validator.report.LevelLoader} for details.
     * <p>
     * This test is expected to PASS.
     */
    @Test
    @PactTestFor(pactMethod = "getPetWithIncompleteResponse")
    public void testGetPetWithIncompleteResponse(final MockServer mockServer) {
        given()
                .header("api_key", "some-api-key")
                .get(mockServer.getUrl() + "/pet/2");
    }

    /**
     * Test a GET with an expectation that specifies an incorrect field type in the response.
     * <p>
     * Without API validation this test would pass and the mistake would only be detected during Provider test
     * execution. With API validation we get immediate feedback that the consumer expectation is invalid.
     * <p>
     * This test is expected to FAIL.
     */
    @Test
    @PactTestFor(pactMethod = "getPetWithInvalidResponse")
    public void testGetPetWithInvalidResponse(final MockServer mockServer) {
        given()
                .header("api_key", "some-api-key")
                .get(mockServer.getUrl() + "/pet/3");
    }

    /**
     * Test a GET with an expectation that specifies an additional field in the response.
     * <p>
     * Without API validation this test would pass and the mistake would only be detected during Provider test
     * execution. With API validation we get immediate feedback that the consumer expectation is invalid.
     * <p>
     * If this is desired behaviour (e.g. the consumer knows the field exists but it's not in the spec), the
     * validation failure can be suppressed by setting:
     * <code>validation.response.body.schema.additionalProperties=WARN</code>
     * <p>
     * This test is expected to FAIL.
     */
    @Test
    @PactTestFor(pactMethod = "getPetWithAdditionalProperties")
    public void testGetPetWithAdditionalPropertiesInResponse(final MockServer mockServer) {
        given()
                .header("api_key", "some-api-key")
                .get(mockServer.getUrl() + "/pet/4");
    }

    /**
     * Test a known bad request.
     * <p>
     * This would normally fail API validation because the request path "/pet/a" is not valid. However, it may
     * be useful to run against the Provider, so we use {@link IgnoreApiValidation} to skip spec validation for
     * this specific test.
     * <p>
     * This test is expected to PASS.
     */
    @Test
    @PactTestFor(pactMethod = "getPetWithInvalidId")
    @IgnoreApiValidation
    public void testGetWithInvalidId(final MockServer mockServer) {
        given()
                .header("api_key", "some-api-key")
                .get(mockServer.getUrl() + "/pet/a");
    }
}
