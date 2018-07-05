package com.atlassian.oai.validator.pact;

import au.com.dius.pact.consumer.Pact;
import au.com.dius.pact.consumer.PactVerification;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.model.PactFragment;
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

import static io.restassured.RestAssured.get;
import static java.lang.String.format;

/**
 * Simulates usage of the {@link ValidatedPactProviderRule} on the consumer side.
 */
public class PactConsumerValidationTest {

    public ValidatedPactProviderRule provider = new ValidatedPactProviderRule("oai/api-test.json", null, "Test", this);

    @Rule
    public CheckForExceptions rule = new CheckForExceptions(provider);

    @Pact(provider = "Test", consumer = "Test")
    public PactFragment getObjectResponse(final PactDslWithProvider builder) {
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
                .toFragment();
    }

    @Pact(provider = "Test", consumer = "Test")
    public PactFragment getIncompleteObjectResponse(final PactDslWithProvider builder) {
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
                .toFragment();
    }

    @Pact(provider = "Test", consumer = "Test")
    public PactFragment getExtraFieldsInObjectResponse(final PactDslWithProvider builder) {
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
                .toFragment();
    }

    @Pact(provider = "Test", consumer = "Test")
    public PactFragment getExtraFieldsInObjectArrayResponse(final PactDslWithProvider builder) {
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
                .toFragment();
    }

    @Test
    @PactVerification(value = "Test", fragment = "getObjectResponse")
    public void passes_withAValidResponse() {
        get(provider.getConfig().url() + "/test/object");
    }

    @Test
    @PactVerification(value = "Test", fragment = "getIncompleteObjectResponse")
    public void passes_whenExpectingAnIncompleteResponse() {
        get(provider.getConfig().url() + "/test/object");
    }

    @Test
    @PactVerification(value = "Test", fragment = "getExtraFieldsInObjectResponse")
    @ExpectExceptionFromRule(expected = ValidatedPactProviderRule.PactValidationError.class)
    public void fails_whenAdditionalFieldsInResponse_withSimpleObject() {
        get(provider.getConfig().url() + "/test/object");
    }

    @Test
    @PactVerification(value = "Test", fragment = "getExtraFieldsInObjectArrayResponse")
    @ExpectExceptionFromRule(expected = ValidatedPactProviderRule.PactValidationError.class)
    public void fails_whenAdditionalFieldsInResponse_withArrayOfObjects() {
        get(provider.getConfig().url() + "/test/objectsInArray");
    }

    /**
     * Expect an exception to be emitted from a test rule
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ExpectExceptionFromRule {
        Class<?> expected() default Throwable.class;
    }

    /**
     * Simple wrapper that allows us to assert the presence of exceptions being emitted by test rules
     */
    public static class CheckForExceptions implements TestRule {

        private final TestRule inner;

        public CheckForExceptions(final TestRule inner) {
            this.inner = inner;
        }

        @Override
        public Statement apply(final Statement base, final Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    final ExpectExceptionFromRule annotation = description.getAnnotation(ExpectExceptionFromRule.class);
                    final Class<?> expected = annotation == null ? null : annotation.expected();
                    try {
                        inner.apply(base, description).evaluate();
                    } catch (final Throwable e) {
                        if (expected == null) {
                            throw e;
                        }
                        if (!e.getClass().isAssignableFrom(expected)) {
                            throw new AssertionFailedError(
                                    format("Expected an exception of type %s but got %s instead",
                                            expected.getName(), e.getClass().getName()
                                    )
                            );
                        }
                        return;
                    }
                    if (expected != null) {
                        throw new AssertionFailedError(
                                format("Expected an exception of type %s but did not get one", expected.getName())
                        );
                    }
                }
            };
        }
    }

}
