package com.atlassian.oai.validator.springmvc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.TreeSet;

import static com.atlassian.oai.validator.report.ValidationReport.Level.ERROR;
import static com.atlassian.oai.validator.report.ValidationReport.Level.INFO;
import static com.atlassian.oai.validator.report.ValidationReport.Level.WARN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultValidationReportHandlerTest {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(DefaultValidationReportHandler.class);
    private ListAppender<ILoggingEvent> listAppender;
    private DefaultValidationReportHandler validationHandler;

    @BeforeEach
    public void setUp() {
        listAppender = new ListAppender<>();
        listAppender.start();

        logger.addAppender(listAppender);
        logger.setLevel(Level.DEBUG);

        validationHandler = new DefaultValidationReportHandler();
    }

    @AfterEach
    public void tearDown() {
        logger.detachAppender(listAppender);
    }

    @Test
    public void requestWithErrorLogsErrorAndThrowsException() {
        final ValidationReport validationReport = mockValidationReport(singletonList(ERROR), "log message");

        try {
            validationHandler.handleRequestReport("GET#/api", validationReport);
        } catch (final InvalidRequestException e) {
            //Expected
        }

        assertThat(listAppender.list.size(), is(1));

        final ILoggingEvent loggingEvent = listAppender.list.get(0);
        assertThat(loggingEvent.getLevel(), is(Level.ERROR));
        assertThat(loggingEvent.getMessage(), is("OpenAPI location={} key={} levels={} messages={}"));
        assertThat(loggingEvent.getArgumentArray(), is(new String[] {"REQUEST", "GET#/api", "ERROR", "Validation failed.\n[ERROR] log message"}));
    }

    @Test
    public void responseWithErrorLogsErrorAndThrowsException() {
        final ValidationReport validationReport = mockValidationReport(singletonList(ERROR), "log message");

        try {
            validationHandler.handleResponseReport("GET#/api", validationReport);
        } catch (final InvalidResponseException e) {
            //Expected
        }

        assertThat(listAppender.list.size(), is(1));

        final ILoggingEvent loggingEvent = listAppender.list.get(0);
        assertThat(loggingEvent.getLevel(), is(Level.ERROR));
        assertThat(loggingEvent.getMessage(), is("OpenAPI location={} key={} levels={} messages={}"));
        assertThat(loggingEvent.getArgumentArray(), is(new String[] {"RESPONSE", "GET#/api", "ERROR", "Validation failed.\n[ERROR] log message"}));
    }

    @Test
    public void requestWithWarnAndInfoLogsInfoMessage() {
        final ValidationReport validationReport = mockValidationReport(asList(WARN, INFO), "log message");
        validationHandler.handleRequestReport("GET#/api", validationReport);

        assertThat(listAppender.list.size(), is(1));

        final ILoggingEvent loggingEvent = listAppender.list.get(0);
        assertThat(loggingEvent.getLevel(), is(Level.INFO));
        assertThat(loggingEvent.getMessage(), is("OpenAPI location={} key={} levels={} messages={}"));
        assertThat(loggingEvent.getArgumentArray(), is(new String[] {"REQUEST", "GET#/api", "WARN,INFO", "No validation errors.\n[WARN] log message"}));
    }

    @Test
    public void responseWithWarnAndInfoLogsInfoMessage() {
        final ValidationReport validationReport = mockValidationReport(asList(WARN, INFO), "log message");
        validationHandler.handleResponseReport("GET#/api", validationReport);

        assertThat(listAppender.list.size(), is(1));

        final ILoggingEvent loggingEvent = listAppender.list.get(0);
        assertThat(loggingEvent.getLevel(), is(Level.INFO));
        assertThat(loggingEvent.getMessage(), is("OpenAPI location={} key={} levels={} messages={}"));
        assertThat(loggingEvent.getArgumentArray(), is(new String[] {"RESPONSE", "GET#/api", "WARN,INFO", "No validation errors.\n[WARN] log message"}));
    }

    @Test
    public void requestWithoutFindingsLogsValidDebugMessage() {
        final ValidationReport validationReport = mockValidationReport(emptyList(), null);
        validationHandler.handleRequestReport("GET#/api", validationReport);

        assertThat(listAppender.list.size(), is(1));
        final ILoggingEvent loggingEvent = listAppender.list.get(0);
        assertThat(loggingEvent.getLevel(), is(Level.DEBUG));
        assertThat(loggingEvent.getMessage(), is("OpenAPI validation: {} - The {} is valid."));
        assertThat(loggingEvent.getArgumentArray(), is(new String[] {"GET#/api", "REQUEST"}));
    }

    @Test
    public void responseWithoutFindingsLogsValidDebugMessage() {
        final ValidationReport validationReport = mockValidationReport(emptyList(), null);
        validationHandler.handleResponseReport("GET#/api", validationReport);

        assertThat(listAppender.list.size(), is(1));
        final ILoggingEvent loggingEvent = listAppender.list.get(0);
        assertThat(loggingEvent.getLevel(), is(Level.DEBUG));
        assertThat(loggingEvent.getMessage(), is("OpenAPI validation: {} - The {} is valid."));
        assertThat(loggingEvent.getArgumentArray(), is(new String[] {"GET#/api", "RESPONSE"}));
    }

    private ValidationReport mockValidationReport(final List<ValidationReport.Level> levels, final String message) {
        final ValidationReport.Message mockedMessage = mock(ValidationReport.Message.class);
        if (levels.size() > 0) {
            when(mockedMessage.getMessage()).thenReturn(message);
            when(mockedMessage.getLevel()).thenReturn(levels.get(0));
        }

        final ValidationReport mockedValidationReport = mock(ValidationReport.class);
        when(mockedValidationReport.sortedValidationLevels()).thenReturn(new TreeSet<>(levels));
        when(mockedValidationReport.getMessages()).thenReturn(singletonList(mockedMessage));
        when(mockedValidationReport.hasErrors()).thenReturn(levels.contains(ERROR));
        return mockedValidationReport;
    }
}
