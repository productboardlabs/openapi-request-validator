package com.atlassian.oai.validator.util;

import org.junit.Test;

import static com.atlassian.oai.validator.util.ContentTypeUtils.findMostSpecificMatch;
import static com.atlassian.oai.validator.util.ContentTypeUtils.isJsonContentType;
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

    @Test
    public void findMostSpecificMatch_returnsMostSpecificMatch_whenUppercaseParameter() {
        assertThat(
            findMostSpecificMatch("application/json", of("application/json;charset=UTF-8")),
            optionalWithValue(is("application/json;charset=UTF-8"))
        );
    }

    @Test
    public void isJsonContentType_returnsTrue_whenJson() {
        assertThat(isJsonContentType("application/json"), is(true));
    }

    @Test
    public void isJsonContentType_returnsTrue_whenHalJson() {
        assertThat(isJsonContentType("application/hal+json"), is(true));
    }

    @Test
    public void isJsonContentType_returnsTrue_whenJsonSuffix() {
        assertThat(isJsonContentType("application/custom+json"), is(true));
    }

    @Test
    public void isJsonContentType_returnsTrue_whenJsonWithWildcard() {
        assertThat(isJsonContentType("application/*+json"), is(true));
    }

    @Test
    public void isJsonContentType_returnsFalse_whenNotJson() {
        assertThat(isJsonContentType("application/xml"), is(false));
    }

    @Test
    public void isJsonContentType_returnsFalse_whenNotValid() {
        assertThat(isJsonContentType("invalid-media-type"), is(false));
    }

    @Test
    public void isJsonContentType_returnsFalse_whenNotJsonWithWildcard() {
        assertThat(isJsonContentType("application/*"), is(false));
    }

    @Test
    public void isJsonContentType_returnsFalse_whenNull() {
        assertThat(isJsonContentType((String) null), is(false));
    }

    @Test
    public void isJsonContentType_returnsTrue_whenJsonIncludeParameters() {
        assertThat(isJsonContentType("application/json;charset=utf-8"), is(true));
    }

}
