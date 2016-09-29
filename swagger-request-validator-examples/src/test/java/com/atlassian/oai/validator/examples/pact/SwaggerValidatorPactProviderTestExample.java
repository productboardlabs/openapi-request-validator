package com.atlassian.oai.validator.examples.pact;

import au.com.dius.pact.provider.ConsumerInfo;
import com.atlassian.oai.validator.pact.PactProviderValidator;
import com.atlassian.oai.validator.pact.ValidatedPactProviderRule;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReportFormatter;
import org.junit.Test;

import java.util.Map;

import static java.lang.String.format;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * An example Pact Provider test that uses the {@link PactProviderValidator} to validate consumer Pacts
 * against a service Swagger API specification.
 */
public class SwaggerValidatorPactProviderTestExample {

    public static final String SWAGGER_JSON_URL = "http://petstore.swagger.io/v2/swagger.json";

    /**
     * This test simulates running against a Consumer where all interactions in the Pact spec are valid according
     * to the Swagger API spec.
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
     *     <li>
     *         The Consumer has invalid expectations on the Provider (which could perhaps have been mitigated by
     *         using the {@link ValidatedPactProviderRule} on the Consumer side); OR
     *     </li>
     *     <li>
     *         The Provider has made a breaking change to their Swagger API specification and will break a Consumer.
     *     </li>
     * </ol>
     */
    @Test
    public void validate_withLocalPact_withInvalidInteractions() {

        final PactProviderValidator validator = PactProviderValidator
                .createFor(SWAGGER_JSON_URL)
                .withConsumer("ExampleConsumer", getClass().getResource("/pacts/invalid-interactions.json"))
                .build();

        assertNoBreakingChanges(validator.validate());

    }

    private void assertNoBreakingChanges(final Map<ConsumerInfo, ValidationReport> results) {
        results.forEach((consumer, report) -> {
            assertThat(format("Validation errors found for consumer '%s':\n%s",
                    consumer.getName(), ValidationReportFormatter.format(report)),
                    report.hasErrors(), is(false));
        });
    }

}
