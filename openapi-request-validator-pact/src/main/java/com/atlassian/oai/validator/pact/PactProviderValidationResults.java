package com.atlassian.oai.validator.pact;

import com.atlassian.oai.validator.report.ValidationReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

/**
 * Results from validating Consumer Pacts against a Provider OpenAPI / Swagger spec.
 */
public class PactProviderValidationResults {

    private final List<ConsumerResult> results = new ArrayList<>();

    /**
     * Get all results for all Consumers that were validated.
     *
     * @return The results for all Consumers that were validated.
     */
    public List<ConsumerResult> getConsumerResults() {
        return Collections.unmodifiableList(results);
    }

    /**
     * Get all results for the named Consumer, if it exists.
     *
     * @return The results for the named Consumer if it was validated; empty otherwise.
     */
    public Optional<ConsumerResult> getConsumerResult(final String name) {
        return results.stream().filter(r -> r.getConsumerName().equals(name)).findFirst();
    }

    /**
     * The results for Consumers that have validation errors.
     *
     * @return The list of results for Consumers that have validation failures
     */
    public List<ConsumerResult> getFailedConsumerResults() {
        return results.stream().filter(ConsumerResult::hasErrors).collect(toList());
    }

    /**
     * Return whether any validation errors exist for any interactions against any Consumer.
     *
     * @return <code>true</code> if validation errors exist; <code>false</code> otherwise.
     */
    public boolean hasErrors() {
        return results.stream().anyMatch(ConsumerResult::hasErrors);
    }

    /**
     * Add a Consumer result to this report
     *
     * @param result the result to add
     */
    public void addConsumerResult(final ConsumerResult result) {
        results.add(result);
    }

    /**
     * Add Consumer results to this report
     *
     * @param results the results to add
     */
    public void addConsumerResults(final Collection<ConsumerResult> results) {
        this.results.addAll(results);
    }

    /**
     * Get a formatted report of validation failures suitable for use in e.g. test output.
     * <p>
     * This is intended as a convenience method. For specific formatting it would be better to construct
     * your own from the data held on this instance.
     *
     * @return A formatted report of validation failures.
     */
    public String getValidationFailureReport() {
        final StringBuilder msg = new StringBuilder();
        getFailedConsumerResults().forEach(r -> {
            msg.append("* ").append(r.getConsumerName()).append('\n');
            r.getFailedInteractions().forEach((i, v) -> {
                msg.append("\t- ").append(i).append("\t");
                v.getMessages()
                        .stream()
                        .filter(m -> m.getLevel() == ValidationReport.Level.ERROR)
                        .forEach(m -> msg.append("\n\t\t[")
                                .append(m.getLevel())
                                .append("] ")
                                .append(m.getMessage().replace("\n", "\n\t\t")));
            });
        });
        return msg.toString();
    }

    /**
     * Provider validation results for a single Consumer.
     */
    public static class ConsumerResult {

        private final String consumerName;
        private final String consumerPact;
        private final Map<String, ValidationReport> interactionResults = new HashMap<>();

        public ConsumerResult(final String consumerName, final String consumerPact) {
            this.consumerName = consumerName;
            this.consumerPact = consumerPact;
        }

        /**
         * @return The name of the Consumer that was validated
         */
        public String getConsumerName() {
            return consumerName;
        }

        /**
         * @return the Pact file that was used during validation
         */
        public String getConsumerPact() {
            return consumerPact;
        }

        /**
         * Get all interaction results, keyed by interaction name.
         *
         * @return The map of interaction name -&gt; validation result
         */
        public Map<String, ValidationReport> getInteractionResults() {
            return Collections.unmodifiableMap(interactionResults);
        }

        /**
         * Get all interactions that have validation errors, keyed by interaction name.
         *
         * @return The map of interaction name -&gt; validation result for failed interactions
         */
        public Map<String, ValidationReport> getFailedInteractions() {
            return interactionResults
                    .entrySet()
                    .stream()
                    .filter(e -> e.getValue().hasErrors())
                    .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        /**
         * Add a result for the named interaction.
         * <p>
         * If a result already exists for the given interaction name (e.g. duplicate interaction names in the Pact file)
         * the provided report will be merged with the existing report stored for that interaction.
         *
         * @param name The name of the interaction to store the result for
         * @param report The validation result for the interaction
         */
        public void addInteractionResult(final String name, final ValidationReport report) {
            interactionResults.merge(name, report, ValidationReport::merge);
        }

        /**
         * Return whether any validation errors exist for any interactions against this Consumer.
         *
         * @return <code>true</code> if validation errors exist; <code>false</code> otherwise.
         */
        boolean hasErrors() {
            return interactionResults.values().stream().anyMatch(ValidationReport::hasErrors);
        }
    }
}
