package com.atlassian.oai.validator.pact;

//CHECKSTYLE:OFF IllegalImport

import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit.PactVerification;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import com.atlassian.oai.validator.pact.ValidatedPactProviderRule.PactValidationError;
import com.atlassian.oai.validator.report.ValidationReport.Message;
import junit.framework.AssertionFailedError;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import static io.restassured.RestAssured.get;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
//CHECKSTYLE:ON IllegalImport

/**
 * Simulates usage of the {@link ValidatedPactProviderRule} on the consumer side.
 */
public class PactConsumerValidationTest {

    public ValidatedPactProviderRule provider = new ValidatedPactProviderRule("oai/api-test.json", null, "Test", this);

    @Rule
    public CheckForValidationFailures rule = new CheckForValidationFailures(provider);

    @Pact(provider = "Test", consumer = "Test")
    public RequestResponsePact getObjectResponse(final PactDslWithProvider builder) {
        final DslPart responseBody = new PactDslJsonBody()
                .numberValue("id", 123)
                .stringValue("name", "the thing")
                .array("tags")
                .string("tag1")
                .closeArray();

        return builder
                .uponReceiving("getObjectResponse")
                .method("GET")
                .path("/test/object")
                .willRespondWith()
                .status(200)
                .body(responseBody)
                .toPact();
    }

    @Pact(provider = "Test", consumer = "Test")
    public RequestResponsePact getIncompleteObjectResponse(final PactDslWithProvider builder) {
        final DslPart responseBody = new PactDslJsonBody()
                .numberValue("id", 123)
                .array("tags")
                .string("tag1")
                .closeArray();

        return builder
                .uponReceiving("getIncompleteObjectResponse")
                .method("GET")
                .path("/test/object")
                .willRespondWith()
                .status(200)
                .body(responseBody)
                .toPact();
    }

    @Pact(provider = "Test", consumer = "Test")
    public RequestResponsePact getExtraFieldsInObjectResponse(final PactDslWithProvider builder) {
        final DslPart responseBody = new PactDslJsonBody()
                .numberValue("id", 123)
                .stringValue("name", "the thing")
                .stringValue("notAField", "something")
                .array("tags")
                .string("tag1")
                .closeArray();

        return builder
                .uponReceiving("getExtraFieldsInObjectResponse")
                .method("GET")
                .path("/test/object")
                .willRespondWith()
                .status(200)
                .body(responseBody)
                .toPact();
    }

    @Pact(provider = "Test", consumer = "Test")
    public RequestResponsePact getExtraFieldsInObjectArrayResponse(final PactDslWithProvider builder) {
        final DslPart responseBody = new PactDslJsonBody()
                .array("children")
                .object()
                .numberValue("id", 123)
                .stringValue("name", "the thing")
                .array("notAField").string("foo").closeArray()
                .array("tags").string("tag1").closeArray()
                .closeObject()
                .closeArray();

        return builder
                .uponReceiving("getExtraFieldsInObjectArrayResponse")
                .method("GET")
                .path("/test/objectsInArray")
                .willRespondWith()
                .status(200)
                .body(responseBody)
                .toPact();
    }

    @Pact(provider = "Test", consumer = "Test")
    public RequestResponsePact getExtraFieldsInInlineObjectArrayResponse(final PactDslWithProvider builder) {
        final DslPart responseBody = new PactDslJsonBody()
                .array("children")
                .object()
                .numberValue("id", 123)
                .stringValue("name", "the thing")
                .array("notAField").string("foo").closeArray()
                .array("tags").string("tag1").closeArray()
                .closeObject()
                .closeArray();

        return builder
                .uponReceiving("getExtraFieldsInInlineObjectArrayResponse")
                .method("GET")
                .path("/test/inlineObjectsInArray")
                .willRespondWith()
                .status(200)
                .body(responseBody)
                .toPact();
    }

    @Test
    @PactVerification(value = "Test", fragment = "getObjectResponse")
    public void passes_withAValidResponse() {
        get(provider.getUrl() + "/test/object");
    }

    @Test
    @PactVerification(value = "Test", fragment = "getIncompleteObjectResponse")
    public void passes_whenExpectingAnIncompleteResponse() {
        get(provider.getUrl() + "/test/object");
    }

    @Test
    @PactVerification(value = "Test", fragment = "getExtraFieldsInObjectResponse")
    @ExpectValidationErrors("validation.response.body.schema.additionalProperties")
    public void fails_whenAdditionalFieldsInResponse_withSimpleObject() {
        get(provider.getUrl() + "/test/object");
    }

    @Test
    @PactVerification(value = "Test", fragment = "getExtraFieldsInObjectArrayResponse")
    @ExpectValidationErrors("validation.response.body.schema.additionalProperties")
    public void fails_whenAdditionalFieldsInResponse_withArrayOfObjects() {
        get(provider.getUrl() + "/test/objectsInArray");
    }

    @Test
    @PactVerification(value = "Test", fragment = "getExtraFieldsInInlineObjectArrayResponse")
    @ExpectValidationErrors("validation.response.body.schema.additionalProperties")
    public void fails_whenAdditionalFieldsInResponse_withArrayOfInlineObjects() {
        get(provider.getUrl() + "/test/inlineObjectsInArray");
    }

    /**
     * Expect an exception to be emitted from a test rule
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ExpectValidationErrors {
        String[] value() default "";
    }

    /**
     * Simple wrapper that allows us to assert the presence of exceptions being emitted by test rules
     */
    public static class CheckForValidationFailures implements TestRule {

        private final TestRule inner;

        CheckForValidationFailures(final TestRule inner) {
            this.inner = inner;
        }

        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    final ExpectValidationErrors annotation = description.getAnnotation(ExpectValidationErrors.class);
                    final String[] expected = annotation == null ? null : annotation.value();
                    try {
                        inner.apply(base, description).evaluate();
                    } catch (final Throwable e) {
                        checkException(expected, e);
                        return;
                    }
                    if (expected != null) {
                        throw new AssertionFailedError("Expected validation errors but did not get any");
                    }
                }
            };
        }

        private void checkException(final String[] expected, final Throwable e) throws Throwable {
            if (expected == null) {
                throw e;
            }
            if (!(e instanceof PactValidationError)) {
                throw new AssertionFailedError(
                        format("Expected a validation error but got got %s instead", e.getClass().getName())
                );
            }
            final PactValidationError error = (PactValidationError) e;
            final Set<String> keys = error.getValidationReport().getMessages().stream().map(Message::getKey).collect(toSet());
            final Set<String> missingKeys = stream(expected).filter(key -> !key.trim().isEmpty() && !keys.contains(key)).collect(toSet());
            if (!missingKeys.isEmpty()) {
                throw new AssertionFailedError(format("Report is missing expected keys: %s. Found %s.", missingKeys, keys));
            }
        }
    }

}
