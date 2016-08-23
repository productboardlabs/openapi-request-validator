package com.atlassian.oai.validator.report;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.max;

/**
 * Resolves the {@link ValidationReport.Level} for a given message key.
 * <p>
 * Levels are specified hierarchically - if a level is not found for the given key it
 * will inherit the level of its parent key. If no level is found for any parent key the
 * {@link #defaultLevel} will be returned.
 * <p>
 * For example:
 * <pre>
 *     validation.request=ERROR
 *     validation.request.body=WARN
 *
 *     getLevel("validation.request.body.missing") == WARN
 *     getLevel("validation.request.parameter.query.missing") == ERROR
 * </pre>
 *
 */
public class LevelResolver {

    private final ValidationReport.Level defaultLevel;
    private final Map<String, ValidationReport.Level> levels = new HashMap<>();

    /**
     * Create a new instance with no key mappings and a default level of {@link ValidationReport.Level#ERROR}.
     */
    public LevelResolver() {
        this(ValidationReport.Level.ERROR);
    }

    /**
     * Create a new instance with the given default level applied to all messages.
     *
     * @param defaultLevel The level to apply to all messages.
     */
    public LevelResolver(final ValidationReport.Level defaultLevel) {
        this(null, defaultLevel);
    }

    /**
     * Create a new instance with the given key -> level mappings and default level.
     *
     * @param levels The mapping of message key -> level to apply to messages.
     *               If <code>null</code>, no mappings will be applied.
     * @param defaultLevel The default level to apply to message keys for which no mapping is provided.
     *                     If <code>null</code> will default to {@link ValidationReport.Level#ERROR}.
     */
    public LevelResolver(@Nullable final Map<String, ValidationReport.Level> levels,
                         @Nullable final ValidationReport.Level defaultLevel) {
        if (levels != null) {
            this.levels.putAll(levels);
        }
        this.defaultLevel = defaultLevel == null ? ValidationReport.Level.ERROR : defaultLevel;
    }

    /**
     * Gets the {@link ValidationReport.Level} for the given message key.
     * <p>
     * Levels are specified hierarchically - if a level is not found for the given key it
     * will inherit the level of its parent key. If no level is found for any parent key the
     * {@link #defaultLevel} will be returned.
     * <p>
     * For example:
     * <pre>
     *     validation.request=ERROR
     *     validation.request.body=WARN
     *
     *     getLevel("validation.request.body.missing") == WARN
     *     getLevel("validation.request.parameter.query.missing") == ERROR
     * </pre>
     *
     * @param key the message key to resolve e.g. <code>"validation.request.body.missing"</code>
     *
     * @return The level to use for the given message key
     */
    @Nonnull
    public ValidationReport.Level getLevel(@Nullable final String key) {
        if (key == null || key.isEmpty()) {
            return defaultLevel;
        }

        if (levels.containsKey(key)) {
            return levels.get(key);
        }

        final String parentKey = key.substring(0, max(0, key.lastIndexOf('.')));
        final ValidationReport.Level result = getLevel(parentKey);
        levels.put(key, result);
        return result;
    }

}
