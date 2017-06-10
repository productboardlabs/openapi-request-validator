package com.atlassian.oai.validator.mockmvc;

/**
 *
 */
public abstract class SwaggerValidatorMatchers {
    public static SwaggerMatchers swagger() {
        return new SwaggerMatchers();
    }
}
