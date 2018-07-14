package com.atlassian.oai.validator.report;

import com.atlassian.oai.validator.report.ValidationReport.MessageContext;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.Test;

import java.util.stream.Stream;

import static com.atlassian.oai.validator.model.Request.Method.GET;
import static com.atlassian.oai.validator.model.Request.Method.POST;
import static com.atlassian.oai.validator.report.ValidationReport.Level.ERROR;
import static com.atlassian.oai.validator.report.ValidationReport.Level.INFO;
import static com.atlassian.oai.validator.report.ValidationReport.Level.WARN;
import static com.atlassian.oai.validator.report.ValidationReport.MessageContext.Location.REQUEST;
import static com.atlassian.oai.validator.report.ValidationReport.MessageContext.Location.RESPONSE;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SimpleValidationReportFormatTest {

    private final SimpleValidationReportFormat classUnderTest = SimpleValidationReportFormat.getInstance();

    @Test
    public void format_withNull_returnsNullReportMessage() {
        final ValidationReport report = null;
        assertThat(classUnderTest.apply(report), is("Validation report is null."));
    }

    @Test
    public void format_noErrors_returnsNoErrorMessage() {
        final ValidationReport report = ValidationReport.empty();
        assertThat(classUnderTest.apply(report), is("No validation errors."));
    }

    @Test
    public void format_withErrors_returnsFormattedMessages() {
        final ValidationReport report = Stream.of(
                new ImmutableMessage("key1", ERROR, "message 1")
                        .withAdditionalContext(
                                MessageContext.create()
                                        .in(RESPONSE)
                                        .withRequestMethod(POST)
                                        .withRequestPath("/some/path")
                                        .withParameter(new Parameter().in("header").name("param"))
                                        .build()
                        ),
                new ImmutableMessage("key2", WARN, "message 2")
                        .withAdditionalContext(
                                MessageContext.create()
                                        .in(REQUEST)
                                        .withRequestMethod(GET)
                                        .withRequestPath("/some/path")
                                        .build()
                        ),
                new ImmutableMessage("key3", INFO, "message 3"))
                .map(ValidationReport::singleton)
                .reduce(ValidationReport.empty(), ValidationReport::merge);

        final String expected =
                "Validation failed.\n" +
                        "[ERROR][RESPONSE][POST /some/path @header.param] message 1\n" +
                        "[WARN][REQUEST][GET /some/path] message 2\n" +
                        "[INFO] message 3";

        assertThat(classUnderTest.apply(report), is(expected));
    }

    @Test
    public void formatMessage_withAdditionalInfo_includesBulletPointInfo() {
        final ValidationReport.Message msg =
                new ImmutableMessage("key1", ERROR, "message 1",
                        "additional info 1", "additional info 2", null, "additional info \nwith a linebreak");

        final String expected =
                "Validation failed.\n" +
                        "[ERROR] message 1\n" +
                        "\t* additional info 1\n" +
                        "\t* additional info 2\n" +
                        "\t* additional info \n" +
                        "\t\twith a linebreak";

        assertThat(classUnderTest.apply(ValidationReport.singleton(msg)), is(expected));
    }
}