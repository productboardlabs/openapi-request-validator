package com.atlassian.oai.validator.whitelist.rule;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport.Message;

import javax.annotation.Nullable;

interface RequestOrResponseWhitelistRule extends WhitelistRule {
    default boolean matches(Message message, ApiOperation operation, @Nullable Request request, @Nullable Response response) {
        return request != null && matches(message, operation, request) ||
                response != null && matches(message, operation, response);
    }

    boolean matches(Message message, ApiOperation operation, Request request);

    boolean matches(Message message, ApiOperation operation, Response response);
}
