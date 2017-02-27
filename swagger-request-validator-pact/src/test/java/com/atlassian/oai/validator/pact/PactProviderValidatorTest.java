package com.atlassian.oai.validator.pact;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PactProviderValidatorTest {

    @Rule
    public final WireMockRule wireMock = new WireMockRule(options()
            .usingFilesUnderClasspath("wiremock")
            .port(9999));

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
    public void build_withInvalidBrokerUrl_hasNoConsumers() {
        final PactProviderValidator validator =
                PactProviderValidator
                        .createFor("/oai/api-users.json")
                        .withPactsFrom("foo", "Provider")
                        .build();

        assertThat(validator.getConsumers(), empty());
    }

    @Test
    public void build_withValidBrokerUrl_hasConsumersForProvider() {
        setupBrokerRootResponse();
        setupBrokerLatestPactsResponse(200, "broker-latest-consumers-response.json");

        final PactProviderValidator validator =
                PactProviderValidator
                    .createFor("/oai/api-users.json")
                    .withPactsFrom("http://localhost:9999", "Provider")
                    .build();

        assertThat(validator.getConsumers().size(), is(2));

    }

    @Test
    public void build_withInvalidProviderId_hasNoConsumers() {
        setupBrokerRootResponse();
        setupBrokerLatestPactsResponse(404, "empty.json");

        final PactProviderValidator validator =
                PactProviderValidator
                        .createFor("/oai/api-users.json")
                        .withPactsFrom("http://localhost:9999", "Provider")
                        .build();

        assertThat(validator.getConsumers().size(), is(0));

    }

    private void setupBrokerLatestPactsResponse(final int status, final String responseName) {
        wireMock.stubFor(get(urlPathEqualTo("/pacts/provider/Provider/latest"))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile(responseName)));
    }

    private void setupBrokerRootResponse() {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("broker-root-response.json")));
    }

    private URL pactUrl(final String name) {
        return getClass().getResource("/pacts/" + name);
    }

}