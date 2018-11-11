package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.report.SimpleValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReportFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import static com.atlassian.oai.validator.report.ValidationReport.MessageContext.Location.REQUEST;
import static com.atlassian.oai.validator.report.ValidationReport.MessageContext.Location.RESPONSE;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Default implementation of {@link ValidationReportHandler}. It logs request/response
 * validation findings to project logger. In case of:
 * <ul>
 *     <li>error - throws {@link InvalidRequestException} for request
 *     or {@link InvalidResponseException} for response and write messages
 *     to log.error</li>
 *     <li>info/warn/ignore - write messages to log.info</li>
 *     <li>no issue - log.debug end of validation</li>
 * </ul>
 * When you would like to modify messages format you can implement your
 * own {@link ValidationReportFormat} and inject it to constructor.
 */
public class DefaultValidationReportHandler implements ValidationReportHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultValidationReportHandler.class);
    private static final String DELIMITER = ",";

    private final ValidationReportFormat validationReportFormat;

    public DefaultValidationReportHandler() {
        this(SimpleValidationReportFormat.getInstance());
    }

    public DefaultValidationReportHandler(@Nonnull final ValidationReportFormat validationReportFormat) {
        requireNonNull(validationReportFormat, "validationReportFormat must not be null");
        this.validationReportFormat = validationReportFormat;
    }

    @Override
    public void handleRequestReport(final String loggingKey, final ValidationReport validationReport) {
        processApiValidationReport(REQUEST, loggingKey, validationReport);
    }

    @Override
    public void handleResponseReport(final String loggingKey, final ValidationReport validationReport) {
        processApiValidationReport(RESPONSE, loggingKey, validationReport);
    }

    protected void processApiValidationReport(final ValidationReport.MessageContext.Location location,
                                            final String loggingKey,
                                            final ValidationReport validationReport) {
        final Set<ValidationReport.Level> validationLevels = validationReport.sortedValidationLevels();

        if (validationLevels.contains(ValidationReport.Level.ERROR)) {
            final RuntimeException validationException = createValidationException(validationReport, location);
            logApiValidation(LOG::error, location, loggingKey, validationLevels,
                    validationReportFormat.apply(validationReport));
            throw validationException;
        } else if (validationLevels.contains(ValidationReport.Level.INFO)
                || validationLevels.contains(ValidationReport.Level.WARN)
                || validationLevels.contains(ValidationReport.Level.IGNORE)) {
            logApiValidation(LOG::info, location, loggingKey, validationLevels,
                    validationReportFormat.apply(validationReport));
        } else {
            LOG.debug("OpenAPI validation: {} - The {} is valid.", loggingKey, location.toString());
        }
    }

    protected void logApiValidation(final BiConsumer<String, String[]> logConsumer,
                                    final ValidationReport.MessageContext.Location location,
                                    final String loggingKey,
                                    final Set<ValidationReport.Level> validationLevels,
                                    final String message) {
        final String logTemplate = "OpenAPI location={} key={} levels={} messages={}";
        final String joinedLevels = validationLevels
                .stream()
                .map(Objects::toString)
                .collect(joining(DELIMITER));

        logConsumer.accept(logTemplate, new String[] {
                location.toString(), loggingKey, joinedLevels, message
        });
    }

    protected RuntimeException createValidationException(final ValidationReport validationReport,
                                                         final ValidationReport.MessageContext.Location location) {
        if (location == REQUEST) {
            return new InvalidRequestException(validationReport);
        } else {
            return new InvalidResponseException(validationReport);
        }
    }
}
