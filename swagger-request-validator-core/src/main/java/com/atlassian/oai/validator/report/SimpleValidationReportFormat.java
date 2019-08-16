package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.atlassian.oai.validator.util.StringUtils;

/**
 * Format a {@link ValidationReport} instance into human-readable String representation for use in e.g. logs or exceptions.
 */
public class SimpleValidationReportFormat implements ValidationReportFormat {

    private static final SimpleValidationReportFormat INSTANCE = new SimpleValidationReportFormat();

    public static SimpleValidationReportFormat getInstance() {
        return INSTANCE;
    }

    @Override
    @Nonnull
    public String apply(@Nullable final ValidationReport report) {
        if (report == null) {
            return "Validation report is null.";
        }
        final StringBuilder b = new StringBuilder();
        if (!report.hasErrors()) {
            b.append("No validation errors.");
        } else {
            b.append("Validation failed.");
        }
        report.getMessages().forEach(m -> b.append('\n').append(formatMessage(m)));
        return b.toString();
    }

    private static String formatMessage(final ValidationReport.Message msg) {
        final StringBuilder b = new StringBuilder();
        b.append("[").append(msg.getLevel()).append("]")
                .append(formatContext(msg.getContext().orElse(null)))
                .append(' ').append(msg.getMessage().replace("\n", "\n\t"));
        msg.getAdditionalInfo().stream()
                .filter(Objects::nonNull)
                .forEach(info -> b.append("\n\t* ").append(info.replace("\n", "\n\t\t")));
        msg.getNestedMessages().stream()
                .forEach(message -> b.append(StringUtils.indentString("\n- " + formatMessage(message), "\t")));

        return b.toString();
    }

    private static String formatContext(@Nullable final ValidationReport.MessageContext ctx) {
        if (ctx == null) {
            return "";
        }
        final String ctxString = stream(
                new String[]{
                        ctx.getRequestMethod().map(Enum::name).orElse(null),
                        ctx.getRequestPath().orElse(null),
                        ctx.getParameter()
                                .map(p -> "@" + p.getIn() + "." + p.getName())
                                .orElse(ctx.getApiRequestBodyDefinition().map(b -> "@body").orElse(null))
                })
                .filter(Objects::nonNull)
                .collect(joining(" ", "[", "]"));

        return ctx.getLocation().map(l -> "[" + l.name() + "]").orElse("") + ctxString;
    }

    private SimpleValidationReportFormat() {

    }
}
