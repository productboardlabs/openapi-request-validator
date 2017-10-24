package com.atlassian.oai.validator.whitelist.rule;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;

import java.util.Objects;

/**
 * A whitelist rule with a human-readable text representation (returned by {@code toString}).
 */
class PrintableWhitelistRule implements WhitelistRule {
    private final String representation;
    private final WhitelistRule function;

    @Override
    public boolean matches(ValidationReport.Message message, ApiOperation operation, Request request, Response response) {
        try {
            return function.matches(message, operation, request, response);
        } catch (RuntimeException ex) {
            ex.printStackTrace(System.out);
            return false;
        }
    }

    PrintableWhitelistRule(String representation, WhitelistRule function) {
        this.representation = Objects.requireNonNull(representation);
        this.function = Objects.requireNonNull(function);
    }

    @Override
    public String toString() {
        return representation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PrintableWhitelistRule that = (PrintableWhitelistRule) o;

        return Objects.equals(this.representation, that.representation);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(representation);
    }
}
