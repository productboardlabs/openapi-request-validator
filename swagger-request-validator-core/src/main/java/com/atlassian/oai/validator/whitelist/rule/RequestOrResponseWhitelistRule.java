package com.atlassian.oai.validator.whitelist.rule;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport.Message;

import javax.annotation.Nullable;

interface RequestOrResponseWhitelistRule extends WhitelistRule {
    default boolean matches(Message message, ApiOperation operation, @Nullable Request request, @Nullable Response response) {
        if (request != null) {
            return matches(message, operation, request);
        } else if (response != null) {
            return matches(message, operation, response);
        } else {
            return false;
        }
    }

    boolean matches(Message message, ApiOperation operation, Request request);

    boolean matches(Message message, ApiOperation operation, Response response);
}
