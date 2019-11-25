package com.atlassian.oai.validator.mockmvc;

import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Static factory methods for {@link ResultMatcher}-based result actions.
 *
 * @deprecated Replaced with {@link OpenApiValidationMatchers}
 */
@Deprecated
public abstract class SwaggerValidatorMatchers {
    public static OpenApiMatchers swagger() {
        return new OpenApiMatchers();
    }
}
