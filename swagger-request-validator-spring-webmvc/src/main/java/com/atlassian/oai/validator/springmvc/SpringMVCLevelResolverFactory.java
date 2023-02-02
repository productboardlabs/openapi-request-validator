package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;

/**
 * A factory for creating {@link LevelResolver} instances that are suitable for use in Spring MVC
 * validation scenarios.
 *
 * @see #create()
 */
public class SpringMVCLevelResolverFactory {

    private SpringMVCLevelResolverFactory() {
    }

    /**
     * Creates a default {@link LevelResolver} instance that is suitable for use in Spring MVC
     * validation scenarios.
     */
    public static LevelResolver create() {
        return LevelResolver.create()
                .withLevel("validation.request.path.missing", ValidationReport.Level.INFO)
                .build();
    }
}
