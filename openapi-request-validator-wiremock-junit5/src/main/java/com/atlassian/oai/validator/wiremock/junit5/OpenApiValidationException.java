package com.atlassian.oai.validator.wiremock.junit5;

import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;

public class OpenApiValidationException extends RuntimeException {

    private final ValidationReport report;

    public OpenApiValidationException(final ValidationReport report) {
        super(JsonValidationReportFormat.getInstance().apply(report));
        this.report = report;
    }

    public ValidationReport getValidationReport() {
        return report;
    }
}