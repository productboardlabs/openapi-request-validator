package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReportFormatter;

/**
 *
 */
public class SwaggerValidationException extends RuntimeException {

    public SwaggerValidationException(final ValidationReport report) {
        super(ValidationReportFormatter.format(report));
    }
}
