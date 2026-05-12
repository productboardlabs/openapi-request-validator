package com.atlassian.oai.validator.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MediaTypeTest {

    @Test
    void parse_simpleType() {
        final MediaType mt = MediaType.parse("application/json");
        assertThat(mt.type(), equalTo("application"));
        assertThat(mt.subtype(), equalTo("json"));
    }

    @Test
    void parse_withCharsetParameter() {
        final MediaType mt = MediaType.parse("application/json; charset=utf-8");
        assertThat(mt.type(), equalTo("application"));
        assertThat(mt.subtype(), equalTo("json"));
        assertThat(mt.charset().isPresent(), is(true));
        assertThat(mt.charset().get(), equalTo(StandardCharsets.UTF_8));
    }

    @Test
    void parse_withQuotedParameter() {
        final MediaType mt = MediaType.parse("application/json; charset=\"utf-8\"");
        assertThat(mt.charset().isPresent(), is(true));
        assertThat(mt.charset().get(), equalTo(StandardCharsets.UTF_8));
    }

    @Test
    void parse_caseInsensitive() {
        final MediaType mt = MediaType.parse("Application/JSON");
        assertThat(mt.type(), equalTo("application"));
        assertThat(mt.subtype(), equalTo("json"));
    }

    @Test
    void parse_withoutSlash_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MediaType.parse("invalidtype"));
    }

    @Test
    void parse_null_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MediaType.parse(null));
    }

    @Test
    void anyType_isCompatibleWithAnything() {
        assertTrue(MediaType.parse("application/json").is(MediaType.ANY_TYPE));
        assertTrue(MediaType.parse("text/plain").is(MediaType.ANY_TYPE));
        assertTrue(MediaType.parse("image/png").is(MediaType.ANY_TYPE));
    }

    @Test
    void applicationWildcard_matchesApplicationSubtypes() {
        assertTrue(MediaType.parse("application/json").is(MediaType.ANY_APPLICATION_TYPE));
        assertTrue(MediaType.parse("application/xml").is(MediaType.ANY_APPLICATION_TYPE));
    }

    @Test
    void applicationWildcard_doesNotMatchOtherTypes() {
        assertFalse(MediaType.parse("text/plain").is(MediaType.ANY_APPLICATION_TYPE));
        assertFalse(MediaType.parse("image/png").is(MediaType.ANY_APPLICATION_TYPE));
    }

    @Test
    void concreteType_doesNotMatchWildcardInThis() {
        // Direction matters: this.is(other) — only wildcards in 'other' match
        assertFalse(MediaType.parse("application/*").is(MediaType.parse("application/json")));
    }

    @Test
    void withoutParameters_stripsParams() {
        final MediaType mt = MediaType.parse("application/json; charset=utf-8");
        final MediaType stripped = mt.withoutParameters();
        assertThat(stripped.type(), equalTo("application"));
        assertThat(stripped.subtype(), equalTo("json"));
        assertFalse(stripped.charset().isPresent());
        assertThat(stripped.toString(), equalTo("application/json"));
    }

    @Test
    void charset_absent_returnsEmpty() {
        final MediaType mt = MediaType.parse("application/json");
        assertFalse(mt.charset().isPresent());
    }

    @Test
    void is_sameType_returnsTrue() {
        assertTrue(MediaType.parse("application/json").is(MediaType.parse("application/json")));
    }

    @Test
    void is_differentType_returnsFalse() {
        assertFalse(MediaType.parse("application/json").is(MediaType.parse("text/plain")));
    }

    @Test
    void is_caseInsensitiveMatch() {
        assertTrue(MediaType.parse("Application/JSON").is(MediaType.parse("application/json")));
    }

    @Test
    void jsonUtf8Constant_matchesApplicationJson() {
        assertTrue(MediaType.parse("application/json").is(MediaType.JSON_UTF_8.withoutParameters()));
    }

    @Test
    void toString_noParams() {
        assertThat(MediaType.parse("application/json").toString(), equalTo("application/json"));
    }

    @Test
    void toString_withParams() {
        final String result = MediaType.parse("application/json; charset=utf-8").toString();
        assertTrue(result.startsWith("application/json"));
        assertTrue(result.contains("charset=utf-8"));
    }
}
