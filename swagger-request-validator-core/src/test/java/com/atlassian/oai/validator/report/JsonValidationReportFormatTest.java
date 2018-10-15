package com.atlassian.oai.validator.report;

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

public class JsonValidationReportFormatTest {

    private final JsonValidationReportFormat classUnderTest = JsonValidationReportFormat.getInstance();

    @Test
    public void format_withErrors_returnsFormattedMessages() {
        final ValidationReport report = Stream.of(
                new ImmutableMessage("key1", ERROR, "message 1")
                        .withAdditionalContext(
                                ValidationReport.MessageContext.create()
                                        .in(RESPONSE)
                                        .withRequestMethod(POST)
                                        .withRequestPath("/some/path")
                                        .withParameter(new Parameter().in("header").name("param"))
                                        .build()
                        ),
                new ImmutableMessage("key2", WARN, "message 2")
                        .withAdditionalContext(
                                ValidationReport.MessageContext.create()
                                        .in(REQUEST)
                                        .withRequestMethod(GET)
                                        .withRequestPath("/some/path")
                                        .build()
                        ),
                new ImmutableMessage("key3", INFO, "message 3"))
                .map(ValidationReport::singleton)
                .reduce(ValidationReport.empty(), ValidationReport::merge);

        final String expected =
                "{" + System.lineSeparator() +
                        "  \"messages\" : [ {" + System.lineSeparator() +
                        "    \"key\" : \"key1\"," + System.lineSeparator() +
                        "    \"level\" : \"ERROR\"," + System.lineSeparator() +
                        "    \"message\" : \"message 1\"," + System.lineSeparator() +
                        "    \"context\" : {" + System.lineSeparator() +
                        "      \"requestPath\" : \"/some/path\"," + System.lineSeparator() +
                        "      \"parameter\" : {" + System.lineSeparator() +
                        "        \"name\" : \"param\"," + System.lineSeparator() +
                        "        \"in\" : \"header\"" + System.lineSeparator() +
                        "      }," + System.lineSeparator() +
                        "      \"location\" : \"RESPONSE\"," + System.lineSeparator() +
                        "      \"requestMethod\" : \"POST\"" + System.lineSeparator() +
                        "    }" + System.lineSeparator() +
                        "  }, {" + System.lineSeparator() +
                        "    \"key\" : \"key2\"," + System.lineSeparator() +
                        "    \"level\" : \"WARN\"," + System.lineSeparator() +
                        "    \"message\" : \"message 2\"," + System.lineSeparator() +
                        "    \"context\" : {" + System.lineSeparator() +
                        "      \"requestPath\" : \"/some/path\"," + System.lineSeparator() +
                        "      \"location\" : \"REQUEST\"," + System.lineSeparator() +
                        "      \"requestMethod\" : \"GET\"" + System.lineSeparator() +
                        "    }" + System.lineSeparator() +
                        "  }, {" + System.lineSeparator() +
                        "    \"key\" : \"key3\"," + System.lineSeparator() +
                        "    \"level\" : \"INFO\"," + System.lineSeparator() +
                        "    \"message\" : \"message 3\"" + System.lineSeparator() +
                        "  } ]" + System.lineSeparator() +
                        "}";

        assertThat(classUnderTest.apply(report), is(expected));
    }

}
