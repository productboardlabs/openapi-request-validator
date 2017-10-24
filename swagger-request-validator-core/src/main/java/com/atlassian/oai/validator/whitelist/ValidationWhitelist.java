package com.atlassian.oai.validator.whitelist;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.whitelist.rule.WhitelistRule;
import com.google.common.collect.ImmutableList;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class ValidationWhitelist {

    private final List<WhitelistRule> rules;

    public static ValidationWhitelist empty() {
        return new ValidationWhitelist(Collections.emptyList());
    }

    public ValidationWhitelist withRule(WhitelistRule rule) {
        return new ValidationWhitelist(
                ImmutableList.<WhitelistRule>builder().addAll(rules).add(rule).build());
    }

    public boolean isWhitelisted(ValidationReport.Message message, ApiOperation operation, Request request, Response response) {
        return whitelistedBy(message, operation, request, response).isPresent();
    }

    public Optional<WhitelistRule> whitelistedBy(ValidationReport.Message message, ApiOperation operation, Request request, Response response) {
        return rules.stream()
                .filter(rule -> rule.matches(message, operation, request, response))
                .findFirst();
    }

    public ValidationWhitelist(Iterable<WhitelistRule> rules) {
        this.rules = ImmutableList.copyOf(rules);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        ValidationWhitelist that = (ValidationWhitelist) o;

        return Objects.equals(this.rules, that.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rules);
    }

    @Override
    public String toString() {
        return rules.toString();
    }
}
