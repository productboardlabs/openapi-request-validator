package com.atlassian.oai.validator.pact;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactReader;
import au.com.dius.pact.model.RequestResponseInteraction;
import au.com.dius.pact.provider.ConsumerInfo;
import au.com.dius.pact.provider.broker.PactBrokerClient;
import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.report.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toMap;

public class PactProviderValidator {

    private static final Logger log = LoggerFactory.getLogger(PactProviderValidator.class);

    private final SwaggerRequestResponseValidator validator;
    private final Collection<ConsumerInfo> consumers = new ArrayList<>();

    private PactProviderValidator(@Nonnull final String swaggerJsonUrl,
                                  @Nullable final Collection<ConsumerInfo> consumers) {
        requireNonNull(swaggerJsonUrl, "A Swagger JSON Url is required");

        this.validator = SwaggerRequestResponseValidator
                .createFor(swaggerJsonUrl)
                .withLevelResolver(PactLevelResolverFactory.create())
                .build();

        if (consumers != null) {
            this.consumers.addAll(consumers);
        }
    }

    public static Builder createFor(@Nonnull final String swaggerJsonUrl) {
        return new Builder().withSwaggerJsonUrl(swaggerJsonUrl);
    }

    public Map<ConsumerInfo, ValidationReport> validate() {
        if (consumers.isEmpty()) {
            log.warn("No consumers supplied. No validation will be performed.");
            return emptyMap();
        }
        return consumers
                .stream()
                .filter(c -> c != null)
                .map(this::doValidate)
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map.Entry<ConsumerInfo, ValidationReport> doValidate(@Nonnull final ConsumerInfo consumer) {

        log.debug("Validating consumer '{}' against API spec", consumer.getName());

        final Pact pact = PactReader.loadPact(consumer.getPactFile());
        final ValidationReport report =
                pact.getInteractions()
                        .stream()
                        .map(i ->
                                validator.validate(
                                        new PactRequest(((RequestResponseInteraction) i).getRequest()),
                                        new PactResponse(((RequestResponseInteraction) i).getResponse()))
                        )
                        .reduce(ValidationReport.empty(), ValidationReport::merge);

        return new AbstractMap.SimpleEntry<>(consumer, report);
    }

    public static class Builder {

        private String swaggerJsonUrl;
        private List<ConsumerInfo> consumers = new ArrayList<>();

        private String brokerUrl;
        private String providerName;

        public Builder withSwaggerJsonUrl(final String swaggerJsonUrl) {
            this.swaggerJsonUrl = swaggerJsonUrl;
            return this;
        }

        public Builder withConsumers(final ConsumerInfo... consumers) {
            this.consumers.addAll(asList(consumers));
            return this;
        }

        public Builder withConsumer(final String consumerName, final String pactFileUrl) {
            this.consumers.add(new ConsumerInfo(consumerName, pactFileUrl));
            return this;
        }

        public Builder withConsumer(final String consumerName, final URL pactFileUrl) {
            this.consumers.add(new ConsumerInfo(consumerName, pactFileUrl));
            return this;
        }

        public Builder withPactsFrom(final String brokerUrl, final String providerName) {
            this.brokerUrl = brokerUrl;
            this.providerName = providerName;
            return this;
        }

        public PactProviderValidator build() {
            if (brokerUrl != null && providerName != null) {
                consumers.addAll(retrieveConsumers(brokerUrl, providerName));
            }
            return new PactProviderValidator(swaggerJsonUrl, consumers);
        }

        @Nonnull
        @SuppressWarnings("unchecked")
        private Collection<ConsumerInfo> retrieveConsumers(@Nonnull final String brokerUrl,
                                                           @Nonnull final String providerName) {

            log.debug("Retrieving consumers from broker '{}' for provider '{}'", brokerUrl, providerName);

            final PactBrokerClient client = new PactBrokerClient(brokerUrl);

            try {
                final Collection<ConsumerInfo> result = client.fetchConsumers(providerName);
                if (result == null || result.isEmpty()) {
                    log.info("No consumers found for provider '{}' on broker '{}'", providerName, brokerUrl);
                    return emptyList();
                }
                return result;
            } catch (final Exception e) {
                log.error(format("Exception occurred while retrieving consumers for provider '%s' from broker '%s'",
                                providerName, brokerUrl), e);
                return emptyList();
            }

        }

    }

}
