package com.atlassian.oai.validator.pact;

import au.com.dius.pact.consumer.ConsumerPactBuilder;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.core.model.RequestResponsePact;
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.Message;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that verify {@link ValidatedPactConsumerTestExtension} rejects pact interactions that
 * do not conform to the API specification.
 * <p>
 * These are unit-level validation tests — they build pacts directly and validate them through the
 * {@link OpenApiInteractionValidator} without starting a Pact mock server.
 */
public class PactRequestResponseValidationTest {

    private static final OpenApiInteractionValidator VALIDATOR =
            OpenApiInteractionValidator
                    .createFor("oai/api-test.json")
                    .withLevelResolver(PactLevelResolverFactory.create())
                    .build();

    @Test
    public void passes_withAValidSimpleObjectResponse() {
        final DslPart responseBody = new PactDslJsonBody()
                .numberValue("id", 123)
                .stringValue("name", "the thing")
                .array("tags").string("tag1").closeArray();

        final RequestResponsePact pact = ConsumerPactBuilder
                .consumer("Test").hasPactWith("Test")
                .uponReceiving("getValidObjectResponse")
                .method("GET").path("/test/object")
                .willRespondWith().status(200).body(responseBody)
                .toPact();

        final ValidationReport report = validateAllInteractions(pact);
        assertTrue(!report.hasErrors(), "Expected no errors but got: " + report.getMessages());
    }

    @Test
    public void passes_withAValidObjectsInArrayResponse() {
        final DslPart responseBody = new PactDslJsonBody()
                .array("children")
                .object()
                .numberValue("id", 123)
                .stringValue("name", "the thing")
                .array("tags").string("tag1").closeArray()
                .closeObject()
                .closeArray();

        final RequestResponsePact pact = ConsumerPactBuilder
                .consumer("Test").hasPactWith("Test")
                .uponReceiving("getValidObjectsInArrayResponse")
                .method("GET").path("/test/objectsInArray")
                .willRespondWith().status(200).body(responseBody)
                .toPact();

        final ValidationReport report = validateAllInteractions(pact);
        assertTrue(!report.hasErrors(), "Expected no errors but got: " + report.getMessages());
    }

    @Test
    public void passes_withAValidInlineObjectsInArrayResponse() {
        final DslPart responseBody = new PactDslJsonBody()
                .array("children")
                .object()
                .numberValue("id", 123)
                .stringValue("name", "the thing")
                .array("tags").string("tag1").closeArray()
                .closeObject()
                .closeArray();

        final RequestResponsePact pact = ConsumerPactBuilder
                .consumer("Test").hasPactWith("Test")
                .uponReceiving("getValidInlineObjectsInArrayResponse")
                .method("GET").path("/test/inlineObjectsInArray")
                .willRespondWith().status(200).body(responseBody)
                .toPact();

        final ValidationReport report = validateAllInteractions(pact);
        assertTrue(!report.hasErrors(), "Expected no errors but got: " + report.getMessages());
    }

    @Test
    public void passes_whenRequiredFieldsMissingFromResponse() {
        // Consumer expectations are a subset of what the provider returns.
        // Missing required fields are downgraded to INFO by PactLevelResolverFactory.
        final DslPart responseBody = new PactDslJsonBody()
                .numberValue("id", 123);

        final RequestResponsePact pact = ConsumerPactBuilder
                .consumer("Test").hasPactWith("Test")
                .uponReceiving("getPartialObjectResponse")
                .method("GET").path("/test/object")
                .willRespondWith().status(200).body(responseBody)
                .toPact();

        final ValidationReport report = validateAllInteractions(pact);
        assertTrue(!report.hasErrors(), "Expected no errors but got: " + report.getMessages());
    }

    @Test
    public void fails_whenAdditionalFieldsInResponse_withSimpleObject() {
        final DslPart responseBody = new PactDslJsonBody()
                .numberValue("id", 123)
                .stringValue("name", "the thing")
                .stringValue("notAField", "something")
                .array("tags").string("tag1").closeArray();

        final RequestResponsePact pact = ConsumerPactBuilder
                .consumer("Test").hasPactWith("Test")
                .uponReceiving("getExtraFieldsInObjectResponse")
                .method("GET").path("/test/object")
                .willRespondWith().status(200).body(responseBody)
                .toPact();

        final ValidationReport report = validateAllInteractions(pact);
        assertTrue(report.hasErrors(), "Expected validation errors");
        assertHasKey(report, "validation.response.body.schema.additionalProperties");
    }

    @Test
    public void fails_whenAdditionalFieldsInResponse_withArrayOfObjects() {
        final DslPart responseBody = new PactDslJsonBody()
                .array("children")
                .object()
                .numberValue("id", 123)
                .stringValue("name", "the thing")
                .array("notAField").string("foo").closeArray()
                .array("tags").string("tag1").closeArray()
                .closeObject()
                .closeArray();

        final RequestResponsePact pact = ConsumerPactBuilder
                .consumer("Test").hasPactWith("Test")
                .uponReceiving("getExtraFieldsInObjectArrayResponse")
                .method("GET").path("/test/objectsInArray")
                .willRespondWith().status(200).body(responseBody)
                .toPact();

        final ValidationReport report = validateAllInteractions(pact);
        assertTrue(report.hasErrors(), "Expected validation errors");
        assertHasKey(report, "validation.response.body.schema.additionalProperties");
    }

    @Test
    public void fails_whenAdditionalFieldsInResponse_withArrayOfInlineObjects() {
        final DslPart responseBody = new PactDslJsonBody()
                .array("children")
                .object()
                .numberValue("id", 123)
                .stringValue("name", "the thing")
                .array("notAField").string("foo").closeArray()
                .array("tags").string("tag1").closeArray()
                .closeObject()
                .closeArray();

        final RequestResponsePact pact = ConsumerPactBuilder
                .consumer("Test").hasPactWith("Test")
                .uponReceiving("getExtraFieldsInInlineObjectArrayResponse")
                .method("GET").path("/test/inlineObjectsInArray")
                .willRespondWith().status(200).body(responseBody)
                .toPact();

        final ValidationReport report = validateAllInteractions(pact);
        assertTrue(report.hasErrors(), "Expected validation errors");
        assertHasKey(report, "validation.response.body.schema.additionalProperties");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static ValidationReport validateAllInteractions(final RequestResponsePact pact) {
        return pact.getInteractions()
                .stream()
                .filter(i -> i.asSynchronousRequestResponse() != null)
                .map(i -> i.asSynchronousRequestResponse())
                .map(i -> VALIDATOR.validate(PactRequest.of(i.getRequest()), PactResponse.of(i.getResponse())))
                .reduce(ValidationReport.empty(), ValidationReport::merge);
    }

    private static void assertHasKey(final ValidationReport report, final String key) {
        final Set<String> keys = report.getMessages().stream().map(Message::getKey).collect(toSet());
        assertTrue(keys.contains(key),
                "Expected error key '" + key + "' but got: " + keys);
    }
}
