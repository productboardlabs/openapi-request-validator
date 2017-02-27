package com.atlassian.oai.validator.parameter.format;

import com.atlassian.oai.validator.report.MutableValidationReport;

public class NoOpStringFormatValidator implements FormatValidator<String> {

    @Override
    public boolean supports(final String format) {
        return false;
    }

    @Override
    public void validate(final MutableValidationReport report, final String value) { }
}
