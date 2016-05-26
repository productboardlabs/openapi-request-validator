package com.atlassian.oai.validator.pact;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A marker annotation used to disable API validation for an individual test.
 * <p>
 * Useful for ignoring validation on a test that is expected to fail validation
 * (e.g. testing malformed request/response, building expectations for endpoints that don't yet exist etc.)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface IgnoreApiValidation {}
