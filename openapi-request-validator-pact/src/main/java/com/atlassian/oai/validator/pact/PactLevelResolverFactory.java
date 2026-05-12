package com.atlassian.oai.validator.pact;

import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;

/**
 * A factory for creating {@link LevelResolver} instances that are suitable for use in Pact validation scenarios.
 *
 * @see #create()
 */
public class PactLevelResolverFactory {

    private PactLevelResolverFactory() { }

    /**
     * Create a {@link LevelResolver} instance that is suitable for use in Pact validation scenarios.
     */
    public static LevelResolver create() {
        return LevelResolver.create()
                .withLevel("validation.response.body.schema.required", ValidationReport.Level.INFO)
                .withLevel("validation.response.body.missing", ValidationReport.Level.INFO)
                .build();
    }

}
