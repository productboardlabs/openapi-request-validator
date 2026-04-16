package com.atlassian.oai.validator.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static io.restassured.RestAssured.get;

/**
 * Integration tests for {@link ValidatedPactConsumerTestExtension} that use a live Pact mock server.
 * <p>
 * Tests for invalid pact interactions (that should be rejected by the validator) are in
 * {@link PactRequestResponseValidationTest}.
 */
@PactTestFor(providerName = "Test")
public class PactConsumerValidationTest {

    @RegisterExtension
    public static final ValidatedPactConsumerTestExtension PROVIDER =
            new ValidatedPactConsumerTestExtension(
                    OpenApiInteractionValidator
                            .createFor("oai/api-test.json")
                            .withLevelResolver(PactLevelResolverFactory.create())
                            .build()
            );

    // -----------------------------------------------------------------------
    // Pact interaction definitions
    // -----------------------------------------------------------------------

    @Pact(provider = "Test", consumer = "Test")
    public V4Pact getObjectResponse(final PactBuilder builder) {
        final DslPart responseBody = new PactDslJsonBody()
                .numberValue("id", 123)
                .stringValue("name", "the thing")
                .array("tags")
                .string("tag1")
                .closeArray();

        return builder
                .usingLegacyDsl()
                .uponReceiving("getObjectResponse")
                .method("GET")
                .path("/test/object")
                .willRespondWith()
                .status(200)
                .body(responseBody)
                .toPact(V4Pact.class);
    }

    @Pact(provider = "Test", consumer = "Test")
    public V4Pact getIncompleteObjectResponse(final PactBuilder builder) {
        // Consumer expectations don't need all required fields (Pact-style subset matching).
        // Required field violations are downgraded to INFO in Pact context.
        final DslPart responseBody = new PactDslJsonBody()
                .numberValue("id", 123)
                .array("tags")
                .string("tag1")
                .closeArray();

        return builder
                .usingLegacyDsl()
                .uponReceiving("getIncompleteObjectResponse")
                .method("GET")
                .path("/test/object")
                .willRespondWith()
                .status(200)
                .body(responseBody)
                .toPact(V4Pact.class);
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    @PactTestFor(pactMethod = "getObjectResponse")
    public void passes_withAValidResponse(final MockServer mockServer) {
        get(mockServer.getUrl() + "/test/object");
    }

    @Test
    @PactTestFor(pactMethod = "getIncompleteObjectResponse")
    public void passes_whenExpectingAnIncompleteResponse(final MockServer mockServer) {
        get(mockServer.getUrl() + "/test/object");
    }
}
