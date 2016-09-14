package com.atlassian.oai.validator.examples.pact;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactProviderRule;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactFragment;
import com.atlassian.oai.validator.pact.IgnoreApiValidation;
import com.atlassian.oai.validator.pact.ValidatedPactProviderRule;
import org.junit.Rule;
import org.junit.Test;

import static io.restassured.RestAssured.get;

/**
 * An example Pact Consumer test that shows use of the {@link ValidatedPactProviderRule} to apply Swagger/OAI
 * validation to Pact interactions.
 * <p>
 * This gives very fast feedback if a consumer's expectations do not match the API specification, without the
 * need to execute the Pacts against the Provider.
 * <p>
 * <b>Note:</b> Its still a good idea to run the Provider side of the Pact interaction; This validation helps
 * catch a class of problems sooner.
 *
 * @see <a href="https://github.com/realestate-com-au/pact">Pact</a>
 * @see <a href="https://github.com/DiUS/pact-jvm">Pact-jvm</a>
 */
public class SwaggerValidatorPactConsumerTestExample {

    public static final String PROVIDER_ID = "Petstore";
    public static final String CONSUMER_ID = "ExampleConsumer";
    public static final String SWAGGER_JSON_URL = "http://petstore.swagger.io/v2/swagger.json";

    /**
     * Validated Pact provider rule - adds OAI validation to the standard {@link PactProviderRule}
     */
    @Rule
    public ValidatedPactProviderRule provider =
            new ValidatedPactProviderRule(SWAGGER_JSON_URL, null, PROVIDER_ID, this);

    @Pact(provider = PROVIDER_ID, consumer = CONSUMER_ID)
    public PactFragment getValidPet(PactDslWithProvider builder) {
        return builder
                .uponReceiving("GET valid pet")
                .method("GET")
                .path("/pet/1")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody().stringValue("name", "fido").array("photoUrls").closeArray().asBody())
                .toFragment();
    }

    @Pact(provider = PROVIDER_ID, consumer = CONSUMER_ID)
    public PactFragment getPetWithIncompleteResponse(PactDslWithProvider builder) {
        return builder
                .uponReceiving("GET invalid pet")
                .method("GET")
                .path("/pet/2")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        // Response missing required field "photoUrls"
                        // API validation is lenient to missing fields and will succeed
                        .stringValue("name", "fido")
                )
                .toFragment();
    }

    @Pact(provider = PROVIDER_ID, consumer = CONSUMER_ID)
    public PactFragment getPetWithInvalidResponse(PactDslWithProvider builder) {
        return builder
                .uponReceiving("GET invalid pet")
                .method("GET")
                .path("/pet/3")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .stringValue("name", "fido")
                        // Response has the incorrect type for a field
                        // API validation will fail
                        .stringType("id", "fido01")
                )
                .toFragment();
    }

    @Pact(provider = PROVIDER_ID, consumer = CONSUMER_ID)
    public PactFragment getPetWithInvalidId(PactDslWithProvider builder) {
        return builder
                .uponReceiving("GET pet with invalid ID")
                .method("GET")
                .path("/pet/a")
                .willRespondWith()
                .status(400)
                .toFragment();
    }

    @Pact(provider = PROVIDER_ID, consumer = CONSUMER_ID)
    public PactFragment getPetWithAdditionalProperties(PactDslWithProvider builder) {
        return builder
                .uponReceiving("GET pet with additional properties")
                .method("GET")
                .path("/pet/4")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .stringValue("name", "fido")
                        .numberValue("extra", 33)
                        .array("photoUrls").closeArray()
                        .asBody())
                .toFragment();
    }

    /**
     * Test a GET with a valid expectation about the response payload.
     * <p>
     * This is expected to pass both API validation and Pact execution.
     */
    @Test
    @PactVerification(value = PROVIDER_ID, fragment = "getValidPet")
    public void testGetValidPet() {
        get(provider.getConfig().url() + "/pet/1");
    }

    /**
     * Test a GET with an expectation that does not specify all the required fields in the response payload.
     * <p>
     * Usually this validation would fail as a required field is missing. However, in keeping with the Pact
     * philosophy of only specifying things the client cares about, the ValidatedPactProviderRule is lenient
     * regarding missing fields in the response. This behavior can be overridden using system properties or a
     * <code>swagger-validator.properties</code> file.
     * See {@link com.atlassian.oai.validator.report.LevelLoader} for more details.
     */
    @Test
    @PactVerification(value = PROVIDER_ID, fragment = "getPetWithIncompleteResponse")
    public void testGetPetWithIncompleteResponse() {
        get(provider.getConfig().url() + "/pet/2");
    }

    /**
     * Test a GET with an expectation that specifies an incorrect field type in the response.
     * <p>
     * Without API validation this test would pass and the mistake would only be detected during Provider test execution.
     * However, with the API validation we get feedback immediately that the Consumer expectation is invalid.
     */
    @Test
    @PactVerification(value = PROVIDER_ID, fragment = "getPetWithInvalidResponse")
    public void testGetPetWithInvalidResponse() {
        get(provider.getConfig().url() + "/pet/3");
    }

    /**
     * Test a GET with an expectation that specifies an additional field in the response.
     * <p>
     * Without API validation this test would pass and the mistake would only be detected during Provider test execution.
     * However, with the API validation we get feedback immediately that the Consumer expectation is invalid.
     * <p>
     * If this is in fact desired behavior (e.g. the Consumer knows that the field exists but is just not in the
     * Provider spec) the validation failure can be changed to a warning by setting the message level
     * <code>validation.schema.additionalProperties=WARN</code>
     */
    @Test
    @PactVerification(value = PROVIDER_ID, fragment = "getPetWithAdditionalProperties")
    public void testGetPetWithAdditionalPropertiesInResponse() {
        get(provider.getConfig().url() + "/pet/4");
    }

    /**
     * Test known bad request.
     * <p>
     * This would normally fail API validation because the request path "pet/a" is not a valid. However,
     * it may be a useful test to run against the Provider, and so we use the <code>IgnoreApiValidation</code>
     * annotation to skip validation against the specification for this specific test.
     */
    @Test
    @PactVerification(value = PROVIDER_ID, fragment = "getPetWithInvalidId")
    @IgnoreApiValidation
    public void testGetWithInvalidId() {
        get(provider.getConfig().url() + "/pet/a");
    }

}
