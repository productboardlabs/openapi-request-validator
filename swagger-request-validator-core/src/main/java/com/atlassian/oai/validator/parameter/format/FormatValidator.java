package com.atlassian.oai.validator.parameter.format;

import com.atlassian.oai.validator.report.MutableValidationReport;

public interface FormatValidator<T> {

    boolean supports(String format);

    void validate(MutableValidationReport report, T value);
}
