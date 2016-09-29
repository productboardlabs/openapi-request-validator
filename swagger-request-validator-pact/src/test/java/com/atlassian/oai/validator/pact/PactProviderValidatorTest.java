package com.atlassian.oai.validator.pact;

import au.com.dius.pact.provider.ConsumerInfo;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Test;

import java.net.URL;
import java.util.Map;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PactProviderValidatorTest {

    @Test
    public void validate_withNoConsumers_returnsEmptyMap() {

        final Map<ConsumerInfo, ValidationReport> results =
                PactProviderValidator
                        .createFor("/oai/api-users.json")
                        .build()
                        .validate();

        assertThat(results.size(), is(0));
    }

    @Test
    public void validate_withValidConsumer_returnsMapWithNoValidationErrors() {

        final Map<ConsumerInfo, ValidationReport> results =
                PactProviderValidator
                        .createFor("/oai/api-users.json")
                        .withConsumer("ExampleConsumer", pactUrl("valid.json"))
                        .build()
                        .validate();

        assertThat(results.size(), is(1));
        results.forEach((consumer, report) -> {
            assertThat(report.hasErrors(), is(false));
        });
    }

    @Test
    public void validate_withInvalidConsumer_returnsMapWithValidationErrors() {

        final Map<ConsumerInfo, ValidationReport> results =
                PactProviderValidator
                        .createFor("/oai/api-users.json")
                        .withConsumer("ExampleConsumer", pactUrl("invalid.json"))
                        .build()
                        .validate();

        assertThat(results.size(), is(1));
        results.forEach((consumer, report) -> {
            assertThat(report.hasErrors(), is(true));
        });
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