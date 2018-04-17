package com.atlassian.oai.validator.report;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * A {@link ValidationReport} containing a single {@link ValidationReport.Message}.
 * <p>
 * This {@link ImmutableValidationReport} is immutable.
 */
public class ImmutableValidationReport implements ValidationReport {

    private final List<ValidationReport.Message> messages;

    ImmutableValidationReport(final ValidationReport.Message message) {
        if (message == null) {
            messages = Collections.emptyList();
            return;
        }
        messages = ImmutableList.of(message);
    }

    ImmutableValidationReport(final ValidationReport.Message... messages) {
        if (messages == null || messages.length == 0) {
            this.messages = Collections.emptyList();
            return;
        }
        this.messages = ImmutableList.copyOf(stream(messages).filter(Objects::nonNull).collect(toList()));
    }

    ImmutableValidationReport(final List<ValidationReport.Message> messages) {
        if (messages == null || messages.size() == 0) {
            this.messages = Collections.emptyList();
            return;
        }
        this.messages = ImmutableList.copyOf(messages.stream().filter(Objects::nonNull).collect(toList()));
    }

    @Nonnull
    @Override
    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return getMessages().toString();
    }

    @Override
    public ValidationReport withAdditionalContext(final MessageContext context) {
        return new ImmutableValidationReport(
                messages.stream().map(m -> m.withAdditionalContext(context))
                        .collect(toList())
        );
    }
}
