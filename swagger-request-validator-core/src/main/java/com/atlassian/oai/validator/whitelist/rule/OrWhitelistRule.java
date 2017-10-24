package com.atlassian.oai.validator.whitelist.rule;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;

import java.util.List;

import static java.util.stream.Collectors.joining;

class OrWhitelistRule implements WhitelistRule {
    private final List<WhitelistRule> rules;

    @Override
    public boolean matches(ValidationReport.Message message, ApiOperation operation, Request request, Response response) {
        return rules.stream().anyMatch(r -> r.matches(message, operation, request, response));
    }

    public OrWhitelistRule(List<WhitelistRule> rules) {
        this.rules = rules;
    }

    @Override
    public String toString() {
        return rules.stream().map(Object::toString).collect(joining(" OR ", "(", ")"));
    }
}

