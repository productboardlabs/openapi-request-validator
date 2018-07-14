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
                "{\n" +
                        "  \"messages\" : [ {\n" +
                        "    \"key\" : \"key1\",\n" +
                        "    \"level\" : \"ERROR\",\n" +
                        "    \"message\" : \"message 1\",\n" +
                        "    \"context\" : {\n" +
                        "      \"requestPath\" : \"/some/path\",\n" +
                        "      \"parameter\" : {\n" +
                        "        \"name\" : \"param\",\n" +
                        "        \"in\" : \"header\"\n" +
                        "      },\n" +
                        "      \"location\" : \"RESPONSE\",\n" +
                        "      \"requestMethod\" : \"POST\"\n" +
                        "    }\n" +
                        "  }, {\n" +
                        "    \"key\" : \"key2\",\n" +
                        "    \"level\" : \"WARN\",\n" +
                        "    \"message\" : \"message 2\",\n" +
                        "    \"context\" : {\n" +
                        "      \"requestPath\" : \"/some/path\",\n" +
                        "      \"location\" : \"REQUEST\",\n" +
                        "      \"requestMethod\" : \"GET\"\n" +
                        "    }\n" +
                        "  }, {\n" +
                        "    \"key\" : \"key3\",\n" +
                        "    \"level\" : \"INFO\",\n" +
                        "    \"message\" : \"message 3\"\n" +
                        "  } ]\n" +
                        "}";

        assertThat(classUnderTest.apply(report), is(expected));
    }

}
