package com.atlassian.oai.validator.mockmvc;

import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Static factory methods for {@link ResultMatcher}-based result actions.
 */
public abstract class OpenApiValidationMatchers {
    public static OpenApiMatchers openApi() {
        return new OpenApiMatchers();
    }
}
