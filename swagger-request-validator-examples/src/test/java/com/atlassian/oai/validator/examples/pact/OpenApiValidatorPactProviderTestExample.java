package com.atlassian.oai.validator.examples.pact;

import com.atlassian.oai.validator.pact.PactProviderValidationResults;
import com.atlassian.oai.validator.pact.PactProviderValidator;
import com.atlassian.oai.validator.pact.ValidatedPactProviderRule;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * An example Pact Provider test that uses the {@link PactProviderValidator} to validate consumer Pacts
 * against a service Swagger API specification.
 */
public class OpenApiValidatorPactProviderTestExample {

    public static final String SWAGGER_JSON_URL = "https://petstore.swagger.io/v2/swagger.json";

    /**
     * This test simulates running against a Consumer where all interactions in the Pact spec are valid according
     * to the Swagger API spec.
     * <p>
     * This test is expected to PASS
     */
    @Test
    public void validate_withLocalPact_withValidInteractions() {

        final PactProviderValidator validator = PactProviderValidator
                .createFor(SWAGGER_JSON_URL)
                .withConsumer("ExampleConsumer", getClass().getResource("/pacts/valid-interactions.json"))
                .build();

        assertNoBreakingChanges(validator.validate());

    }

    /**
     * This test simulates running against a Consumer where there are invalid interactions in the Pact file.
     * <p>
     * This may have occurred in one of two ways:
     * <ol>
     * <li>
     * The Consumer has invalid expectations on the Provider (which could perhaps have been mitigated by
     * using the {@link ValidatedPactProviderRule} on the Consumer side); OR
     * </li>
     * <li>
     * The Provider has made a breaking change to their Swagger API specification and will break a Consumer.
     * </li>
     * </ol>
     * <p>
     * This test is expected to FAIL
     */
    @Test
    public void validate_withLocalPact_withInvalidInteractions() {

        final PactProviderValidator validator = PactProviderValidator
                .createFor(SWAGGER_JSON_URL)
                .withConsumer("ExampleConsumer", getClass().getResource("/pacts/invalid-interactions.json"))
                .build();

        assertNoBreakingChanges(validator.validate());

    }

    private void assertNoBreakingChanges(final PactProviderValidationResults results) {
        if (results.hasErrors()) {
            final StringBuilder msg = new StringBuilder("Validation errors found.\n\t");
            msg.append(results.getValidationFailureReport().replace("\n", "\n\t"));
            fail(msg.toString());
        }
    }

}
