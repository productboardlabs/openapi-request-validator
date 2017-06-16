package com.atlassian.oai.validator.mockmvc;

import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Static factory methods for {@link ResultMatcher}-based result actions.
 */
public abstract class SwaggerValidatorMatchers {
    public static SwaggerMatchers swagger() {
        return new SwaggerMatchers();
    }
}
