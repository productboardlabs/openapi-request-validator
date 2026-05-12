package com.atlassian.oai.validator.report;

import static com.atlassian.oai.validator.report.ValidationReport.Level.IGNORE;

public final class LevelResolverFactory {

    private LevelResolverFactory() {

    }

    /**
     * Construct a new {@link LevelResolver} that disables the additional properties validation.
     * <p>
     * This is needed if your spec uses composition via {@code allOf}, {@code anyOf} or {@code oneOf}.
     *
     * @return a new {@link LevelResolver} that disables the additional properties validation.
     */
    public static LevelResolver withAdditionalPropertiesIgnored() {
        return LevelResolver.create().withLevel("validation.schema.additionalProperties", IGNORE).build();
    }

}
