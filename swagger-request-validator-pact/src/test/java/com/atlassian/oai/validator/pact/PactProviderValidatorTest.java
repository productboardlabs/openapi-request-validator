package com.atlassian.oai.validator.pact;

import au.com.dius.pact.core.model.BrokerUrlSource;
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import java.net.URL;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

public class PactProviderValidatorTest {

    @Rule
    public final WireMockRule wireMock = new WireMockRule(options()
            .usingFilesUnderClasspath("wiremock")
            .dynamicPort()
            .extensions(new ResponseTemplateTransformer(false)));

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
        assertThat(results.getConsumerResult("ExampleConsumer").isPresent(), is(true));
        assertThat(results.getConsumerResult("ExampleConsumer").get().hasErrors(), is(false));
    }

    @Test
    public void validate_withValidConsumer_returnsMapWithNoValidationErrors_v4() {

        final PactProviderValidationResults results =
                PactProviderValidator
                        .createFor("/oai/api-users.json")
                        .withConsumer("ExampleConsumer", pactUrl("valid-v4.json"))
                        .build()
                        .validate();

        assertThat(results.hasErrors(), is(false));
        assertThat(results.getConsumerResults().size(), is(1));
        assertThat(results.getConsumerResult("ExampleConsumer").isPresent(), is(true));
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
        assertThat(results.getConsumerResult("ExampleConsumer").isPresent(), is(true));
        assertThat(results.getConsumerResult("ExampleConsumer").get().hasErrors(), is(true));
    }

    @Test
    public void validate_withInvalidConsumer_returnsMapWithValidationErrors_v4() {

        final PactProviderValidationResults results =
                PactProviderValidator
                        .createFor("/oai/api-users.json")
                        .withConsumer("ExampleConsumer", pactUrl("invalid-v4.json"))
                        .build()
                        .validate();

        assertThat(results.hasErrors(), is(true));
        assertThat(results.getConsumerResults().size(), is(1));
        assertThat(results.getConsumerResult("ExampleConsumer").isPresent(), is(true));
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
                        .withPactsFrom("http://localhost:" + wireMock.port(), "Provider")
                        .build();

        assertThat(validator.getConsumers().size(), is(2));
        assertThat(validator.getConsumers().stream()
                .map(consumerInfo -> ((BrokerUrlSource) (consumerInfo.getPactSource())).getUrl())
                .collect(Collectors.toList()), everyItem(startsWith("http://localhost:" + wireMock.port())));
    }

    @Test
    public void build_withInvalidProviderId_hasNoConsumers() {
        setupBrokerRootResponse();
        setupBrokerLatestPactsResponse(404, "empty.json");

        final PactProviderValidator validator =
                PactProviderValidator
                        .createFor("/oai/api-users.json")
                        .withPactsFrom("http://localhost:" + wireMock.port(), "Provider")
                        .build();

        assertThat(validator.getConsumers().size(), is(0));
    }

    @Test
    public void validator_usesConfiguredInteractionValidatorIfSupplied() {

        final PactProviderValidationResults results =
                PactProviderValidator
                        .createFor(OpenApiInteractionValidator
                                .createForSpecificationUrl("/oai/api-users.json")
                                .withLevelResolver(PactLevelResolverFactory.create())
                                .build()
                        )
                        .withConsumer("ExampleConsumer", pactUrl("valid.json"))
                        .build()
                        .validate();

        assertThat(results.hasErrors(), is(false));
        assertThat(results.getConsumerResults().size(), is(1));
        assertThat(results.getConsumerResult("ExampleConsumer").isPresent(), is(true));
        assertThat(results.getConsumerResult("ExampleConsumer").get().hasErrors(), is(false));
    }

    @Test
    public void validator_usesConfiguredInteractionValidatorIfSupplied_V4() {

        final PactProviderValidationResults results =
                PactProviderValidator
                        .createFor(OpenApiInteractionValidator
                                .createForSpecificationUrl("/oai/api-users.json")
                                .withLevelResolver(PactLevelResolverFactory.create())
                                .build()
                        )
                        .withConsumer("ExampleConsumer", pactUrl("valid-v4.json"))
                        .build()
                        .validate();

        assertThat(results.hasErrors(), is(false));
        assertThat(results.getConsumerResults().size(), is(1));
        assertThat(results.getConsumerResult("ExampleConsumer").isPresent(), is(true));
        assertThat(results.getConsumerResult("ExampleConsumer").get().hasErrors(), is(false));
    }

    private void setupBrokerLatestPactsResponse(final int status, final String responseName) {
        wireMock.stubFor(get(urlPathEqualTo("/pacts/provider/Provider/latest"))
                .willReturn(aResponse()
                        .withStatus(status)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile(responseName)
                        .withTransformers("response-template")));
    }

    private void setupBrokerRootResponse() {
        wireMock.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBodyFile("broker-root-response.json")
                        .withTransformers("response-template")));
    }

    private URL pactUrl(final String name) {
        return getClass().getResource("/pacts/" + name);
    }

}
