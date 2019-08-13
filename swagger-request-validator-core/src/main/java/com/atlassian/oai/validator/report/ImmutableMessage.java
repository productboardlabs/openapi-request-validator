package com.atlassian.oai.validator.report;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

class ImmutableMessage implements ValidationReport.Message {

    private final String key;
    private final ValidationReport.Level level;
    private final String message;
    private final List<String> additionalInfo;

    @Nullable
    private final List<ValidationReport.Message> nestedMessages;

    @Nullable
    private final ValidationReport.MessageContext context;

    ImmutableMessage(@Nonnull final String key,
                     @Nonnull final ValidationReport.Level level,
                     @Nonnull final String message,
                     @Nonnull final String... additionalInfo) {
        this(key, level, message, asList(additionalInfo), null);
    }

    ImmutableMessage(@Nonnull final String key,
                     @Nonnull final ValidationReport.Level level,
                     @Nonnull final String message,
                     @Nonnull final List<String> additionalInfo,
                     @Nullable final ValidationReport.MessageContext context) {
        this(key, level, message, additionalInfo, null, context);
    }

    ImmutableMessage(@Nonnull final String key,
                     @Nonnull final ValidationReport.Level level,
                     @Nonnull final String message,
                     @Nonnull final List<String> additionalInfo,
                     @Nullable final List<ValidationReport.Message> nestedMessages,
                     @Nullable final ValidationReport.MessageContext context) {

        this.key = requireNonNull(key, "A key is required");
        this.level = requireNonNull(level, "A level is required");
        this.message = requireNonNull(message, "A message is required");
        this.additionalInfo = unmodifiableList(requireNonNull(additionalInfo));
        this.nestedMessages = nestedMessages;
        this.context = context;
    }

    @Override
    public ValidationReport.Message withLevel(final ValidationReport.Level level) {
        return new ImmutableMessage(key, level, message, additionalInfo.toArray(new String[additionalInfo.size()]));
    }

    @Override
    public ValidationReport.Message withAdditionalInfo(final String info) {
        return new ImmutableMessage(
                key, level, message,
                ImmutableList.<String>builder().addAll(additionalInfo).add(info).build(),
                nestedMessages,
                context
        );
    }

    @Override
    public ValidationReport.Message withNestedMessages(final Collection<ValidationReport.Message> messages) {
        List<ValidationReport.Message> newMessages = nestedMessages;
        if (messages != null) {
            newMessages = ImmutableList.<ValidationReport.Message>builder()
                    .addAll(this.nestedMessages != null ? this.nestedMessages : new ArrayList<>())
                    .addAll(messages)
                    .build();
        }
        return new ImmutableMessage(
                key, level, message,
                additionalInfo,
                newMessages,
                context
        );
    }

    @Override
    public ValidationReport.Message withAdditionalContext(final ValidationReport.MessageContext context) {
        final ValidationReport.MessageContext newContext = this.context == null ? context : this.context.enhanceWith(context);
        return new ImmutableMessage(key, level, message, additionalInfo, nestedMessages, newContext);
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public ValidationReport.Level getLevel() {
        return level;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return level + " - "
                + message.replace("\n", "\n\t")
                + ": [" + additionalInfo.stream().collect(joining(", ")) + "]"
                + (nestedMessages == null
                        ? ""
                        : "\n" + nestedMessages.stream().map(message->this.indent(message, "\t")).collect(joining("\n")));
    }

    private String indent(final ValidationReport.Message msg, final String indentStr) {
        return indentStr + msg.toString().replace("\t", "\t" + indentStr);
    }

    @Override
    public List<String> getAdditionalInfo() {
        return additionalInfo;
    }

    @Override
    public Optional<List<ValidationReport.Message>> getNestedMessages() {
        return Optional.ofNullable(nestedMessages);
    }

    @Override
    public Optional<ValidationReport.MessageContext> getContext() {
        return Optional.ofNullable(context);
    }

}
