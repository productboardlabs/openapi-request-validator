package com.atlassian.oai.validator.pact;

import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;

public class PactLevelResolverFactory {

    public static LevelResolver create() {
        return LevelResolver.create()
                .withLevel("validation.schema.required", ValidationReport.Level.INFO)
                .withLevel("validation.response.body.missing", ValidationReport.Level.INFO)
                .build();
    }

}
