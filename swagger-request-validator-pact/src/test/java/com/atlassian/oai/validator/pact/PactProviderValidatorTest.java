package com.atlassian.oai.validator.pact;

import org.junit.Test;

import java.net.URL;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PactProviderValidatorTest {

    @Test
    public void validate_withNoConsumers_returnsEmptyMap() {

        final PactProviderValidationResults results =
                PactProviderValidator
                        .createFor("/oai/api-users.json")
                        .build()
                        .validate();

        assertThat(results.getConsumerResults(), empty());
        assertThat(results.hasErrors(), is(false));
    }

    @Test
    public void validate_withValidConsumer_returnsMapWithNoValidationErrors() {

        final PactProviderValidationResults results =
                PactProviderValidator
                        .createFor("/oai/api-users.json")
                        .withConsumer("ExampleConsumer", pactUrl("valid.json"))
                        .build()
                        .validate();

        assertThat(results.hasErrors(), is(false));
        assertThat(results.getConsumerResults().size(), is(1));
        assertThat(results.getConsumerResult("ExampleConsumer").get().hasErrors(), is(false));

    }

    @Test
    public void validate_withInvalidConsumer_returnsMapWithValidationErrors() {

        final PactProviderValidationResults results =
                PactProviderValidator
                        .createFor("/oai/api-users.json")
                        .withConsumer("ExampleConsumer", pactUrl("invalid.json"))
                        .build()
                        .validate();

        assertThat(results.hasErrors(), is(true));
        assertThat(results.getConsumerResults().size(), is(1));
        assertThat(results.getConsumerResult("ExampleConsumer").get().hasErrors(), is(true));
    }

    @Test
    public void build_withInvalidPactUrl_hasNoConsumers() {
        final PactProviderValidator validator =
                PactProviderValidator
                        .createFor("/oai/api-users.json")
                        .withPactsFrom("foo", "Provider")
                        .build();

        assertThat(validator.getConsumers(), empty());
    }

    private URL pactUrl(final String name) {
        return getClass().getResource("/pacts/" + name);
    }

}