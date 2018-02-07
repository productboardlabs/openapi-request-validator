package com.atlassian.oai.validator.report;

import com.atlassian.oai.validator.model.ApiOperation;
import io.swagger.models.parameters.Parameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A report of validation errors that occurred during validation.
 * <p>
 * A report consists of a collection of messages with a given level.
 * Any message with a level of {@link Level#ERROR} indicates a validation failure.
 */
public interface ValidationReport {

    /**
     * The validation level
     */
    enum Level {
        ERROR,
        WARN,
        INFO,
        IGNORE
    }

    /**
     * A single message in the validation report
     */
    interface Message {

        static Builder create(final String key, final String message) {
            return new Builder(key, Level.ERROR, message);
        }

        String getKey();

        String getMessage();

        Level getLevel();

        List<String> getAdditionalInfo();

        /**
         * Returns contextual information about this message, if it is available.
         */
        Optional<MessageContext> getContext();

        /**
         * Returns a new instance, the same as this message, but, with level changed.
         */
        Message withLevel(Level level);

        /**
         * Returns a new instance, the same as this message, but with additional info attached.
         */
        Message withAdditionalInfo(String info);

        /**
         * Returns a new instance, the same as this message, but with the provided context attached.
         */
        Message withContext(MessageContext context);

        /**
         * Returns a new instance, the same as this message, but additional context attached.
         */
        Message withAdditionalContext(MessageContext context);

        class Builder {
            private final String key;
            private final ValidationReport.Level level;
            private final String message;
            private final List<String> additionalInfo = new ArrayList<>();
            private ValidationReport.MessageContext context;

            private Builder(@Nonnull final String key,
                            @Nonnull final ValidationReport.Level level,
                            @Nonnull final String message) {

                this.key = requireNonNull(key, "A key is required");
                this.level = requireNonNull(level, "A level is required");
                this.message = requireNonNull(message, "A message is required");
            }

            public Builder withAdditionalInfo(final List<String> additionalInfo) {
                if (additionalInfo != null) {
                    this.additionalInfo.addAll(additionalInfo);
                }
                return this;
            }

            public Builder withAdditionalInfo(final String... additionalInfo) {
                this.additionalInfo.addAll(asList(additionalInfo));
                return this;
            }

            public Builder withContext(final ValidationReport.MessageContext context) {
                this.context = context;
                return this;
            }

            public Message build() {
                return new ImmutableMessage(key, level, message, additionalInfo, context);
            }
        }

    }

    /**
     * Contextual information about a validation message.
     */
    interface MessageContext {

        static MessageContext empty() {
            return create().build();
        }

        static Builder create() {
            return new Builder();
        }

        static Builder from(final MessageContext other) {
            return new Builder(other);
        }

        Optional<String> getRequestPath();

        Optional<ApiOperation> getApiOperation();

        Optional<Parameter> getParameter();

        /**
         * Return a new MessageContext instance that contains all of the data in this context,
         * plus data from the incoming context where that data does not already exist on this context.
         * <p>
         * This is used to build a context up as more information becomes available.
         */
        MessageContext enhanceWith(MessageContext other);

        class Builder {
            ApiOperation apiOperation;
            Parameter parameter;
            String requestPath;

            private Builder() {
            }

            private Builder(final MessageContext init) {
                apiOperation = init.getApiOperation().orElse(null);
                parameter = init.getParameter().orElse(null);
                requestPath = init.getRequestPath().orElse(null);
            }

            public Builder withApiOperation(final ApiOperation apiOperation) {
                this.apiOperation = apiOperation;
                return this;
            }

            public Builder withParameter(final Parameter parameter) {
                this.parameter = parameter;
                return this;
            }

            public Builder withRequestPath(final String path) {
                requestPath = path;
                return this;
            }

            public Builder withAdditionalDataFrom(final MessageContext other) {
                if (apiOperation == null) {
                    apiOperation = other.getApiOperation().orElse(null);
                }
                if (parameter == null) {
                    parameter = other.getParameter().orElse(null);
                }
                if (requestPath == null) {
                    requestPath = other.getRequestPath().orElse(null);
                }
                return this;
            }

            public MessageContext build() {
                return new ImmutableMessageContext(this);
            }

        }
    }

    /**
     * Return an empty report.
     *
     * @return an immutable empty report
     */
    static ValidationReport empty() {
        return new EmptyValidationReport();
    }

    /**
     * Return an unmodifiable report that contains a single message.
     *
     * @param message The message to add to the report
     *
     * @return An unmodifiable validation report with a single message
     */
    static ValidationReport singleton(@Nullable final Message message) {
        if (message == null) {
            return empty();
        }
        return new ImmutableValidationReport(message);
    }

    /**
     * Return an unmodifiable report containing all the provided messages
     *
     * @param messages The messages to add to the report
     *
     * @return an unmodifiable report containing all the provided messages
     */
    static ValidationReport from(final Collection<Message> messages) {
        return from(messages.toArray(new Message[messages.size()]));
    }

    /**
     * Return an unmodifiable report containing all the provided messages
     *
     * @param messages The messages to add to the report
     *
     * @return an unmodifiable report containing all the provided messages
     */
    static ValidationReport from(final Message... messages) {
        if (messages == null || messages.length == 0) {
            return empty();
        }
        return new ImmutableValidationReport(messages);
    }

    /**
     * Return if this validation report contains errors.
     *
     * @return <code>true</code> if a validation error exists; <code>false</code> otherwise.
     */
    default boolean hasErrors() {
        return getMessages().stream().anyMatch(m -> m.getLevel() == Level.ERROR);
    }

    /**
     * Get the validation messages on this report.
     *
     * @return The messages recorded on this report
     */
    @Nonnull
    List<Message> getMessages();

    /**
     * Merges the given validation report with this one, and return a new, unmodifiable report
     * containing the messages from both reports.
     *
     * @param other The validation report to merge with this one
     *
     * @return A new, unmodifiable validation report containing all the messages from this report
     * and the other report
     */
    default ValidationReport merge(@Nonnull final ValidationReport other) {
        requireNonNull(other, "A validation report is required");
        return new MergedValidationReport(this, other);
    }
}
