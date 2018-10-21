package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.report.ValidationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import static com.atlassian.oai.validator.report.ValidationReport.MessageContext.Location.REQUEST;
import static com.atlassian.oai.validator.report.ValidationReport.MessageContext.Location.RESPONSE;
import static java.util.stream.Collectors.joining;

/**
 * Default implementation of {@link ValidationReportHandler}.
 */
public class DefaultValidationReportHandler implements ValidationReportHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultValidationReportHandler.class);
    private static final String DELIMITER = ",";

    @Override
    public void handleRequestReport(final String loggingKey, final ValidationReport validationReport) {
        processApiValidationReport(REQUEST, loggingKey, validationReport);
    }

    @Override
    public void handleResponseReport(final String loggingKey, final ValidationReport validationReport) {
        processApiValidationReport(RESPONSE, loggingKey, validationReport);
    }

    private void processApiValidationReport(final ValidationReport.MessageContext.Location location,
                                            final String loggingKey,
                                            final ValidationReport validationReport) {
        final Set<ValidationReport.Level> validationLevels = validationReport.sortedValidationLevels();

        if (validationLevels.contains(ValidationReport.Level.ERROR)) {
            final RuntimeException validationException = createValidationException(validationReport, location);
            logApiValidation(LOG::error, location, loggingKey, validationLevels, validationMessage(validationReport));
            throw validationException;
        } else if (validationLevels.contains(ValidationReport.Level.INFO)
                || validationLevels.contains(ValidationReport.Level.WARN)
                || validationLevels.contains(ValidationReport.Level.IGNORE)) {
            logApiValidation(LOG::info, location, loggingKey, validationLevels, validationMessage(validationReport));
        } else {
            LOG.debug("OpenAPI validation: {} - The {} is valid.", loggingKey, location.toString());
        }
    }

    private void logApiValidation(final BiConsumer<String, String[]> logConsumer,
                                  final ValidationReport.MessageContext.Location location,
                                  final String loggingKey,
                                  final Set<ValidationReport.Level> validationLevels,
                                  final String message) {
        final String logTemplate = "OpenAPI location={} key={} levels={} message={}";
        final String joinedLevels = validationLevels
                .stream()
                .map(Objects::toString)
                .collect(joining(DELIMITER));

        logConsumer.accept(logTemplate, new String[] {
                location.toString(), loggingKey, joinedLevels, message
        });
    }

    private String validationMessage(final ValidationReport validationReport) {
        return validationReport
                .getMessages()
                .stream()
                .map(ValidationReport.Message::toString)
                .collect(joining(DELIMITER));
    }

    private RuntimeException createValidationException(
            final ValidationReport validationReport,
            final ValidationReport.MessageContext.Location location
    ) {
        if (location == REQUEST) {
            return new InvalidRequestException(validationReport);
        } else {
            return new InvalidResponseException(validationReport);
        }
    }
}
