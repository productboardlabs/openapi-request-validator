package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.report.ValidationReport;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * In case the request is invalid.
 * <p>
 * The requests response will be mapped to an appropriate {@link HttpStatus}.
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidRequestException extends RuntimeException {

    private static final String MESSAGE_DELIMETER = ", ";

    private final ValidationReport validationReport;
    private String message;

    public InvalidRequestException(final ValidationReport validationReport) {
        this.validationReport = validationReport;
    }

    @Override
    public String getMessage() {
        if (message == null) {
            final StringBuilder sb = new StringBuilder();
            for (final ValidationReport.Message message : validationReport.getMessages()) {
                sb.append(MESSAGE_DELIMETER).append(message.getMessage());
            }
            message = sb.substring(Math.min(sb.length(), MESSAGE_DELIMETER.length()));
        }
        return message;
    }
}