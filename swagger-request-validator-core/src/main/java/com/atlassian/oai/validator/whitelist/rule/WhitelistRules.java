package com.atlassian.oai.validator.whitelist.rule;

import com.atlassian.oai.validator.whitelist.StatusType;
import com.google.common.collect.ImmutableList;
import io.swagger.models.HttpMethod;

/**
 * Static factory methods for creating {@link WhitelistRule} instances.
 */
public final class WhitelistRules {

    private WhitelistRules() {}

    public static WhitelistRule allOf(final WhitelistRule... rules) {
        return new AndWhitelistRule(ImmutableList.copyOf(rules));
    }

    public static WhitelistRule anyOf(final WhitelistRule... rules) {
        return new OrWhitelistRule(ImmutableList.copyOf(rules));
    }

    public static WhitelistRule isEntity(final String entityName) {
        return new IsEntityWhitelistRule(entityName);
    }

    public static WhitelistRule messageHasKey(final String key) {
        return new PrintableWhitelistRule(
                "Message with key: '" + key + "'",
                (message, operation, request, response) ->
                        key.equalsIgnoreCase(message.getKey()));
    }

    public static WhitelistRule messageContains(final String regexp) {
        return new PrintableWhitelistRule(
                "Message contains: '" + regexp + "'",
                (message, operation, request, response) -> message.getMessage().toLowerCase()
                        .matches(".*" + regexp.toLowerCase() + ".*"));
    }

    public static WhitelistRule pathContains(final String regexp) {
        return new PrintableWhitelistRule(
                "Api path contains: '" + regexp + "'",
                (message, operation, request, response) -> operation != null &&
                        operation.getRequestPath().normalised().matches(".*" + regexp.toLowerCase() + ".*"));
    }

    public static WhitelistRule isRequest() {
        return new PrintableWhitelistRule(
                "Is request",
                (message, operation, request, response) -> request != null);
    }

    public static WhitelistRule isResponse() {
        return new PrintableWhitelistRule(
                "Is request",
                (message, operation, request, response) -> response != null);
    }

    public static WhitelistRule responseStatusIs(final int status) {
        return new PrintableWhitelistRule(
                "Response status is " + status,
                (message, operation, request, response) -> response != null && response.getStatus() == status);
    }

    public static WhitelistRule responseStatusTypeIs(final StatusType statusType) {
        return new PrintableWhitelistRule(
                "Response status is " + statusType,
                (message, operation, request, response) -> response != null && statusType.matches(response.getStatus()));
    }

    public static WhitelistRule methodIs(final HttpMethod method) {
        return new PrintableWhitelistRule(
                "Method is " + method,
                (message, operation, request, response) -> operation != null && operation.getMethod() == method);
    }

}
