package com.atlassian.oai.validator.pact;

import au.com.dius.pact.core.model.BrokerUrlSource;
import au.com.dius.pact.core.model.DefaultPactReader;
import au.com.dius.pact.core.model.FileSource;
import au.com.dius.pact.core.model.Pact;
import au.com.dius.pact.core.model.RequestResponseInteraction;
import au.com.dius.pact.core.model.UrlSource;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.pactbroker.PactBrokerClient;
import au.com.dius.pact.core.pactbroker.PactBrokerClientConfig;
import au.com.dius.pact.core.pactbroker.PactBrokerResult;
import au.com.dius.pact.provider.ConsumerInfo;
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.report.ValidationReport;
import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A validator that can be used on the Provider side to validate Consumer Pacts against the
 * Provider's OpenAPI / Swagger specification.
 * <p>
 * The validator can be used to validate against all Consumers with Pacts registered in a broker, and/or
 * against individual Consumer Pact files sourced from other locations (the file system etc.)
 * <p>
 * To validate against all Consumers in a broker:
 * <pre>
 *     final PactProviderValidator validator = PactProviderValidator
 *                                                  .createFor(SPEC_URL)
 *                                                  .withPactsFrom(BROKER_URL, PROVIDER_ID)
 *                                                  .build();
 * </pre>
 * <p>
 * To validate against specific Consumer Pact files:
 * <pre>
 *     final PactProviderValidator validator = PactProviderValidator
 *                                                  .createFor(SPEC_URL)
 *                                                  .withConsumer(CONSUMER_ID, CONSUMER_PACT_URL)
 *                                                  .build();
 * </pre>
 *
 * @see <a href="https://docs.pact.io/documentation/sharings_pacts.html">Pact broker</a>
 * @see OpenApiInteractionValidator
 * @see ValidatedPactProviderRule
 */
public class PactProviderValidator {

    private static final Logger log = LoggerFactory.getLogger(PactProviderValidator.class);

    private final OpenApiInteractionValidator validator;
    private final Collection<ConsumerInfo> consumers = new ArrayList<>();

    private PactProviderValidator(@Nonnull final OpenApiInteractionValidator validator,
                                  final Collection<ConsumerInfo> consumers) {
        this.validator = requireNonNull(validator, "A validator is required");
        if (consumers != null) {
            this.consumers.addAll(consumers);
        }
    }

    /**
     * Create a new {@link PactProviderValidator} that validates Consumers against the given OpenAPI / Swagger specification.
     * <p>
     * The provided param can be either a URL to the API specification, or an inline specification.
     * Supports both JSON and YAML formats.
     *
     * @param specUrlOrPayload The URL of the OpenAPI / Swagger specification to use, or an inline specification
     *
     * @return A builder that can create configured {@link PactProviderValidator} instances.
     */
    public static Builder createFor(@Nonnull final String specUrlOrPayload) {
        return new Builder().withApiSpecification(specUrlOrPayload);
    }

    /**
     * Create a new {@link PactProviderValidator} that validates Consumers against the given OpenAPI / Swagger specification.
     *
     * @param validator The pre-configured validator instance to use
     *
     * @return A builder that can create configured {@link PactProviderValidator} instances.
     */
    public static Builder createFor(@Nonnull final OpenApiInteractionValidator validator) {
        return new Builder().withValidator(validator);
    }

    /**
     * Perform the validation of Consumer Pacts against the configured OpenAPI / Swagger specification.
     *
     * @return The results of validation for each Consumer.
     */
    public PactProviderValidationResults validate() {
        log.debug("Validating {} consumers against API spec", consumers.size());

        final PactProviderValidationResults result = new PactProviderValidationResults();

        if (consumers.isEmpty()) {
            log.warn("No consumers supplied. No validation will be performed.");
            return result;
        }
        result.addConsumerResults(
                consumers.stream()
                        .filter(Objects::nonNull)
                        .map(this::doValidate)
                        .collect(toList()));
        return result;
    }

    private PactProviderValidationResults.ConsumerResult doValidate(@Nonnull final ConsumerInfo consumer) {
        log.debug("Validating consumer '{}' against API spec", consumer.getName());

        final PactProviderValidationResults.ConsumerResult result =
                new PactProviderValidationResults.ConsumerResult(consumer.getName(), getPactSourceLocation(consumer));

        final Map<String, Object> options = new HashMap<>();
        final List<Object> authOptions = consumer.getPactFileAuthentication();
        if (authOptions != null && !authOptions.isEmpty()) {
            options.put("authentication", authOptions);
        }

        final Pact pact = DefaultPactReader.INSTANCE.loadPact(consumer.getPactSource(), options);
        ((pact instanceof V4Pact) ? ((V4Pact) pact).asRequestResponsePact().get() : pact).getInteractions().forEach(i -> {
            final RequestResponseInteraction interaction = (RequestResponseInteraction) i;
            final ValidationReport report = validator.validate(
                    PactRequest.of(interaction.getRequest()),
                    PactResponse.of(interaction.getResponse()));
            result.addInteractionResult(interaction.getDescription(), report);
        });

        return result;
    }

    private String getPactSourceLocation(@Nonnull final ConsumerInfo consumer) {
        final Object pactSource = consumer.getPactSource();
        if (pactSource instanceof BrokerUrlSource) {
            return ((BrokerUrlSource) pactSource).getUrl();
        } else if (pactSource instanceof UrlSource) {
            return ((UrlSource) pactSource).getUrl();
        } else if (pactSource instanceof FileSource) {
            return ((FileSource) pactSource).getFile().getAbsolutePath();
        }
        throw new IllegalStateException("Pact Source Not Valid.");
    }

    @VisibleForTesting
    Collection<ConsumerInfo> getConsumers() {
        return consumers;
    }

    /**
     * A builder that can be used to create configured {@link PactProviderValidator} instances.
     * <p>
     * Instances should normally be obtained via {@link PactProviderValidator#createFor(String)}.
     *
     * @see PactProviderValidator#createFor(String)
     */
    public static class Builder {
        private String specUrlOrPayload;
        private OpenApiInteractionValidator validator;
        private final List<ConsumerInfo> consumers = new ArrayList<>();

        private String brokerUrl;
        private String providerName;
        private final Map<String, Object> brokerOptions = new HashMap<>();

        /**
         * @deprecated Replaced with {@link #withApiSpecification(String)}
         */
        @Deprecated
        public Builder withSwaggerJsonUrl(final String specUrlOrPayload) {
            this.specUrlOrPayload = specUrlOrPayload;
            return this;
        }

        /**
         * The OpenAPI v3 or Swagger v2 specification to use in the validator.
         * <p>
         * Can be either a URL to a specification, or an inline specification payload, in either JSON or YAML formats.
         * <p>
         * A URL can be an absolute HTTP/HTTPS URL, a File URL or a classpath location (without the classpath: scheme).
         * <p>
         * For example:
         * <pre>
         *     // Create from a publicly hosted HTTP location
         *     .withApiSpecification("http://api.myservice.com/swagger.json")
         *
         *     // Create from a file on the local filesystem
         *     .withApiSpecification("file://Users/myuser/tmp/api.json");
         *
         *     // Create from a classpath resource in the /api package
         *     .withApiSpecification("/api/api.yaml");
         * </pre>
         *
         * @param specUrlOrPayload The location of the Swagger JSON specification to use in the validator.
         *
         * @return this builder instance.
         */
        public Builder withApiSpecification(final String specUrlOrPayload) {
            this.specUrlOrPayload = specUrlOrPayload;
            return this;
        }

        /**
         * The pre-configured interaction validator to use.
         * <p>
         * If provided, will ignore any provided spec URL / payloads.
         *
         * @param validator The underlying validator to use
         *
         * @return this builder instance
         */
        public Builder withValidator(final OpenApiInteractionValidator validator) {
            this.validator = validator;
            return this;
        }

        /**
         * Add one or more Consumers that will be included in the validation.
         * <p>
         * Note that each supplied consumer must have a <code>name</code> and <code>pactFile</code> configured.
         *
         * @param consumers The consumers to include
         *
         * @return this builder instance.
         */
        public Builder withConsumers(final ConsumerInfo... consumers) {
            this.consumers.addAll(asList(consumers));
            return this;
        }

        /**
         * Add a Consumer that will be included in the validation.
         *
         * @param consumerName The name of the Consumer
         * @param pactFileLocation The location of the Consumer Pact file to validate against
         *
         * @return this builder instance.
         */
        @SuppressWarnings("rawtypes")
        public Builder withConsumer(final String consumerName, final String pactFileLocation) {
            final ConsumerInfo consumerInfo = new ConsumerInfo(consumerName);
            consumerInfo.setPactSource(new FileSource(new File(pactFileLocation)));
            consumers.add(consumerInfo);
            return this;
        }

        /**
         * Add a Consumer that will be included in the validation.
         *
         * @param consumerName The name of the Consumer
         * @param pactFileUrl The location of the Consumer Pact file to validate against
         *
         * @return this builder instance.
         */
        @SuppressWarnings("rawtypes")
        public Builder withConsumer(final String consumerName, final URL pactFileUrl) {
            final ConsumerInfo consumerInfo = new ConsumerInfo(consumerName);
            consumerInfo.setPactSource(new UrlSource(pactFileUrl.toString()));
            consumers.add(consumerInfo);
            return this;
        }

        /**
         * Configure the validator to validate against all Consumer Pacts retrieved from the given
         * broker for the given Provider.
         *
         * @param brokerUrl The URL of the Pact Broker to retrieve Consumer Pacts from
         * @param providerName The ID of the Provider to retrieve Pacts for
         *
         * @return this builder instance.
         */
        public Builder withPactsFrom(final String brokerUrl,
                                     final String providerName) {
            this.brokerUrl = brokerUrl;
            this.providerName = providerName;
            return this;
        }

        /**
         * Configure the validator to validate against all Consumer Pacts retrieved from the given
         * secure broker for the given Provider.
         *
         * @param brokerUrl The URL of the Pact Broker to retrieve Consumer Pacts from
         * @param username The username for the broker
         * @param password The password for the broker
         * @param providerName The ID of the Provider to retrieve Pacts for
         *
         * @return this builder instance.
         */
        public Builder withPactsFrom(final String brokerUrl,
                                     final String username,
                                     final String password,
                                     final String providerName) {
            withPactsFrom(brokerUrl, providerName);
            brokerOptions.clear();
            brokerOptions.put("authentication", Arrays.asList("basic", username, password));
            return this;
        }

        /**
         * Build a configured {@link PactProviderValidator} instance with the values collected in this builder.
         *
         * @return The configured {@link PactProviderValidator} instance.
         */
        public PactProviderValidator build() {
            if (brokerUrl != null && providerName != null) {
                consumers.addAll(retrieveConsumers());
            }
            if (validator != null) {
                return new PactProviderValidator(validator, consumers);
            }

            final OpenApiInteractionValidator validator = OpenApiInteractionValidator
                    .createFor(specUrlOrPayload)
                    .withLevelResolver(PactLevelResolverFactory.create())
                    .build();
            return new PactProviderValidator(validator, consumers);
        }

        @Nonnull
        private Collection<ConsumerInfo> retrieveConsumers() {
            log.debug("Retrieving consumers from broker '{}' for provider '{}'", brokerUrl, providerName);
            try {
                final Collection<ConsumerInfo> consumersInfo = retrievePactBrokerConsumers().stream()
                        .map(ConsumerInfo.Companion::from)
                        .collect(toList());
                if (consumersInfo.isEmpty()) {
                    log.info("No consumers found for provider '{}' on broker '{}'", providerName, brokerUrl);
                }
                return consumersInfo;
            } catch (final Exception e) {
                log.error(format("Exception occurred while retrieving consumers for provider '%s' from broker '%s'",
                        providerName, brokerUrl), e);
                return emptyList();
            }
        }

        private Collection<PactBrokerResult> retrievePactBrokerConsumers() {
            return new PactBrokerClient(brokerUrl, brokerOptions, new PactBrokerClientConfig()).fetchConsumers(providerName);
        }
    }
}
