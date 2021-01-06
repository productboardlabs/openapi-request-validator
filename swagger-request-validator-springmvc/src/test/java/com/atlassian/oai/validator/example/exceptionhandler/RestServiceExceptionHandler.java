package com.atlassian.oai.validator.example.exceptionhandler;

import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.springmvc.InvalidRequestException;
import com.atlassian.oai.validator.springmvc.InvalidResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

@ControllerAdvice
public class RestServiceExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<RestValidationReport> handle(final InvalidRequestException invalidRequestException) {
        final RestValidationReport report = new RestValidationReport(invalidRequestException
                .getValidationReport().getMessages());
        return new ResponseEntity<>(report, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(InvalidResponseException.class)
    public ResponseEntity<RestValidationReport> handle(final InvalidResponseException invalidResponseException) {
        final RestValidationReport report = new RestValidationReport(invalidResponseException
                .getValidationReport().getMessages());
        return new ResponseEntity<>(report, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static class RestValidationReport {

        private final List<ValidationReport.Message> messages;

        public RestValidationReport(final List<ValidationReport.Message> messages) {
            this.messages = messages;
        }

        public List<ValidationReport.Message> getMessages() {
            return messages;
        }
    }
}
