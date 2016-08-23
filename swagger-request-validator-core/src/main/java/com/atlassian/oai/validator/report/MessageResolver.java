package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ResourceBundle;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * Resolves a message key to a {@link ValidationReport.Message} object.
 * <p>
 * Message Strings are resolved from the <code>messages</code> resource bundle.
 * <p>
 * Message levels are resolved using a configured {@link LevelResolver}.
 *
 * @see LevelResolver
 */
public class MessageResolver {

    private final ResourceBundle messages = ResourceBundle.getBundle("messages");
    private final LevelResolver levelResolver;

    /**
     * Create a new instance with the default {@link LevelResolver} (all messages will be emitted at the ERROR level).
     *
     * @see LevelResolver#LevelResolver()
     */
    public MessageResolver() {
        this(new LevelResolver());
    }

    /**
     * Create a new instance with the provided {{@link LevelResolver}}.
     */
    public MessageResolver(final LevelResolver levelResolver) {
        this.levelResolver = levelResolver == null ? new LevelResolver() : levelResolver;
    }

    /**
     * Get the message with the given key.
     * <p>
     * If no message is found for the key, or if the configured {@link LevelResolver} returns a level of
     * {@link ValidationReport.Level#IGNORE} for the key, will return <code>null</code>.
     *
     * @param key The key of the message to retrieve.
     * @param args Arguments to use when resolving the message String.
     *
     * @return The message for the given key, or <code>null</code> if no message is found
     */
    @Nullable
    public ValidationReport.Message get(@Nonnull final String key, final Object... args) {
        requireNonNull(key, "A message key is required.");
        final ValidationReport.Level level = levelResolver.getLevel(key);
        if (!messages.containsKey(key)) {
            return null;
        }
        return new ImmutableMessage(key, level, format(messages.getString(key), args));
    }

    /**
     * Create a message with the given key and message.
     * <p>
     * Used when translating validation messages from other sources (e.g. JSON schema validation)
     * where a message has already been generated.
     * <p>
     * Uses the configured {@link LevelResolver} to resolve the message level.
     *
     * @param key The key to include in the message.
     * @param message The message to include
     *
     * @return A message that contains the given key and message string.
     * The level will be set by the configured {@link LevelResolver}.
     */
    public ValidationReport.Message create(@Nonnull final String key, final String message) {
        requireNonNull(key, "A message key is required.");
        final ValidationReport.Level level = levelResolver.getLevel(key);
        return new ImmutableMessage(key, level, message);
    }

}
