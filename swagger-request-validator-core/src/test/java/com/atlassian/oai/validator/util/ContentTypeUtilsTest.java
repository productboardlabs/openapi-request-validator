package com.atlassian.oai.validator.util;

import org.junit.Test;

import static com.atlassian.oai.validator.util.ContentTypeUtils.findMostSpecificMatch;
import static com.google.common.collect.ImmutableSet.of;
import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ContentTypeUtilsTest {

    @Test
    public void findMostSpecificMatch_returnsEmpty_whenEmptyApiList() {
        assertThat(
                findMostSpecificMatch("text/plain", emptySet()),
                emptyOptional()
        );
    }

    @Test
    public void findMostSpecificMatch_returnsEmpty_whenNoMatches() {
        assertThat(
                findMostSpecificMatch("text/plain", of("application/json", "application/*")),
                emptyOptional()
        );
    }

    @Test
    public void findMostSpecificMatch_returnsMatch_whenDirectMatch() {
        assertThat(
                findMostSpecificMatch("text/plain", of("application/json", "text/plain")),
                optionalWithValue(is("text/plain"))
        );
    }

    @Test
    public void findMostSpecificMatch_returnsMatch_whenRangeMatch() {
        assertThat(
                findMostSpecificMatch("text/plain", of("application/json", "text/*")),
                optionalWithValue(is("text/*"))
        );
    }

    @Test
    public void findMostSpecificMatch_returnsMostSpecificMatch_whenMultipleMatches() {
        assertThat(
                findMostSpecificMatch("text/plain", of("application/json", "*/*", "text/*")),
                optionalWithValue(is("text/*"))
        );
    }

    @Test
    public void findMostSpecificMatch_returnsGlobalWildcard_whenGlobalWildcardSupplied() {
        assertThat(
                findMostSpecificMatch("*/*", of("application/json", "*/*", "text/*")),
                optionalWithValue(is("*/*"))
        );
    }

    @Test
    public void findMostSpecificMatch_returnsGlobalWildcard_whenNoMoreSpecificMatches() {
        assertThat(
                findMostSpecificMatch("application/xml", of("application/json", "*/*", "text/*")),
                optionalWithValue(is("*/*"))
        );
    }

    @Test
    public void findMostSpecificMatch_returnsEmpty_whenInvalidMediaType() {
        assertThat(
                findMostSpecificMatch("foop", of("application/json", "*/*", "text/*")),
                emptyOptional()
        );
    }

}