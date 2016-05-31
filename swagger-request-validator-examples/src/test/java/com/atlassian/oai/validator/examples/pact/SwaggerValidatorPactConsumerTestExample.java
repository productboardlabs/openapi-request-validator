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

import static com.jayway.restassured.RestAssured.get;

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

    @Pact(provider = PROVIDER_ID, consumer = "ExampleConsumer")
    public PactFragment getInvalidPet(PactDslWithProvider builder) {
        return builder
                .uponReceiving("GET invalid pet")
                .method("GET")
                .path("/pet/2")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody().stringValue("name", "fido")) // Response missing required field "photoUrls"
                .toFragment();
    }

    @Pact(provider = PROVIDER_ID, consumer = "ExampleConsumer")
    public PactFragment getPetWithInvalidId(PactDslWithProvider builder) {
        return builder
                .uponReceiving("GET pet with invalid ID")
                .method("GET")
                .path("/pet/a")
                .willRespondWith()
                .status(400)
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
     * Test a GET with an invalid expectation about the response payload.
     * <p>
     * Without API validation this test would pass, as it is a valid Pact fragment. However, the expected
     * response payload does not match the schema specified in the API spec, and so the test will fail.
     */
    @Test
    @PactVerification(value = PROVIDER_ID, fragment = "getInvalidPet")
    public void testGetInvalidPet() {
        get(provider.getConfig().url() + "/pet/2");
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
