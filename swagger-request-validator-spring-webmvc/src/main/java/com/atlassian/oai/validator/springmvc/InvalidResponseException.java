package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * In case the response is invalid.
 * <p>
 * The response will be mapped to an appropriate {@link HttpStatus}.
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class InvalidResponseException extends RuntimeException {

    private final ValidationReport validationReport;
    private String message;

    public InvalidResponseException(final ValidationReport validationReport) {
        this.validationReport = validationReport;
    }

    @Override
    public String getMessage() {
        if (message == null) {
            message = JsonValidationReportFormat.getInstance().apply(validationReport);
        }
        return message;
    }

    public ValidationReport getValidationReport() {
        return validationReport;
    }
}