package com.atlassian.oai.validator.whitelist.rule;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport.Message;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

public interface WhitelistRule {

    boolean matches(Message message, @Nullable ApiOperation operation, @Nullable Request request, @Nullable Response response);

    default WhitelistRule and(WhitelistRule rule) {
        return new AndWhitelistRule(ImmutableList.of(this, rule));
    }

    default WhitelistRule or(WhitelistRule rule) {
        return new OrWhitelistRule(ImmutableList.of(this, rule));
    }

    default WhitelistRule not() {
        return new PrintableWhitelistRule(
                "Not " + this,
                (message, operation, request, response) -> !this.matches(message, operation, request, response)
        );
    }
}
