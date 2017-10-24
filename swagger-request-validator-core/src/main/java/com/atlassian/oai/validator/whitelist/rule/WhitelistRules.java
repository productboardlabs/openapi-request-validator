package com.atlassian.oai.validator.whitelist.rule;

import com.atlassian.oai.validator.whitelist.StatusType;
import com.google.common.collect.ImmutableList;
import io.swagger.models.HttpMethod;

/**
 * Static factory methods for creating {@link WhitelistRule} instances.
 */
public final class WhitelistRules {

    private WhitelistRules() {}

    public static WhitelistRule allOf(WhitelistRule... rules) {
        return new AndWhitelistRule(ImmutableList.copyOf(rules));
    }

    public static WhitelistRule anyOf(WhitelistRule... rules) {
        return new OrWhitelistRule(ImmutableList.copyOf(rules));
    }

    public static WhitelistRule isEntity(String entityName) {
        return new IsEntityWhitelistRule(entityName);
    }

    public static WhitelistRule messageHasKey(String key) {
        return new PrintableWhitelistRule(
                "Message with key: " + key,
                (message, operation, request, response) ->
                        message.getKey().toLowerCase().contains(key.toLowerCase()));
    }

    public static WhitelistRule massageContains(String regexp) {
        return new PrintableWhitelistRule(
                "Message contains: " + regexp,
                (message, operation, request, response) -> message.getMessage().toLowerCase()
                        .matches(".*" + regexp.toLowerCase() + ".*"));
    }

    public static WhitelistRule pathContains(String regexp) {
        return new PrintableWhitelistRule(
                "Api path containing: " + regexp,
                (message, operation, request, response) -> operation.getRequestPath().normalised()
                        .matches(".*" + regexp.toLowerCase() + ".*"));
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

    public static WhitelistRule responseStatusIs(int status) {
        return new PrintableWhitelistRule(
                "Response status is " + status,
                (message, operation, request, response) -> response != null && response.getStatus() == status);
    }

    public static WhitelistRule responseStatusTypeIs(StatusType statusType) {
        return new PrintableWhitelistRule(
                "Response status is " + statusType,
                (message, operation, request, response) -> response != null && statusType.matches(response.getStatus()));
    }

    public static WhitelistRule methodIs(HttpMethod method) {
        return new PrintableWhitelistRule(
                "Method is " + method,
                (message, operation, request, response) -> operation.getMethod() == method);
    }

}
