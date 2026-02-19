package com.atlassian.oai.validator.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static com.atlassian.oai.validator.util.ContentTypeUtils.findMostSpecificMatch;
import static com.atlassian.oai.validator.util.ContentTypeUtils.isJsonContentType;
import static com.google.common.collect.ImmutableSet.of;
import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ContentTypeUtilsTest {

    @Nested
    class FindMostSpecificMatchTests {

        static Stream<FindMostSpecificMatchTestCase> params() {
            return Stream.of(
                    new FindMostSpecificMatchTestCase("returns empty when no API list", "text/plain", emptySet(), null),
                    new FindMostSpecificMatchTestCase("returns empty when no matches", "text/plain", of("application/json", "application/*"), null),
                    new FindMostSpecificMatchTestCase("returns match when direct match", "text/plain", of("application/json", "text/plain"), "text/plain"),
                    new FindMostSpecificMatchTestCase("returns match when range match", "text/plain", of("application/json", "text/*"), "text/*"),
                    new FindMostSpecificMatchTestCase("returns most specific match when multiple matches", "text/plain", of("application/json", "*/*", "text/*"), "text/*"),
                    new FindMostSpecificMatchTestCase("returns global wildcard when supplied", "*/*", of("application/json", "*/*", "text/*"), "*/*"),
                    new FindMostSpecificMatchTestCase("returns global wildcard when no more specific match", "application/xml", of("application/json", "*/*", "text/*"), "*/*"),
                    new FindMostSpecificMatchTestCase("returns empty when invalid media type", "foop", of("application/json", "*/*", "text/*"), null),
                    new FindMostSpecificMatchTestCase("handles case differences in params", "application/json;charset=utf-8",
                            of("application/json;charset=UTF-8", "application/json;charset=UTF-16"), "application/json;charset=UTF-8"),
                    new FindMostSpecificMatchTestCase("handles whitespace differences in params", "application/json; charset=utf-8",
                            of("application/json;charset=UTF-8", "application/json;charset=UTF-16"), "application/json;charset=UTF-8")
            );
        }

        @ParameterizedTest(name = "findMostSpecificMatch {0}")
        @MethodSource("params")
        void test(final FindMostSpecificMatchTestCase testCase) {
            assertThat(
                    findMostSpecificMatch(testCase.candidate(), testCase.apiContentTypes()),
                    testCase.expected() == null ? emptyOptional() : optionalWithValue(is(testCase.expected()))
            );
        }
    }

    @Nested
    class MatchesAnyTests {

        static Stream<MatchesAnyTestCase> params() {
            return Stream.of(
                    new MatchesAnyTestCase("returns false when null candidate", null, of("application/json", "text/xml"), false),
                    new MatchesAnyTestCase("returns false when empty API list", "application/json", emptySet(), false),
                    new MatchesAnyTestCase("returns true when direct match", "application/json", of("application/json", "text/xml"), true),
                    new MatchesAnyTestCase("returns false when no direct match", "application/json", of("text/json", "text/xml"), false),
                    new MatchesAnyTestCase("returns true when subtype range match", "application/json", of("application/*", "text/xml"), true),
                    new MatchesAnyTestCase("returns false when no subtype range match", "application/json", of("text/*", "text/xml"), false),
                    new MatchesAnyTestCase("returns true when global match", "application/hal+json", of("*/*", "text/xml"), true),
                    new MatchesAnyTestCase("returns true when charsets defined and direct match", "application/json;charset=utf-8", of("application/json", "text/xml"), true),
                    new MatchesAnyTestCase("returns true when suffix match", "application/hal+json", of("application/json", "application/hal+json", "text/xml"), true)
            );
        }

        @ParameterizedTest(name = "matchesAny {0}")
        @MethodSource("params")
        void test(final MatchesAnyTestCase testCase) {
            assertThat(ContentTypeUtils.matchesAny(testCase.candidate(), testCase.apiContentTypes()), is(testCase.expected()));
        }

    }

    @Nested
    class IsJsonContentTypeTests {

        static Stream<IsJsonContentTypeTestCase> params() {
            return Stream.of(
                    new IsJsonContentTypeTestCase("application/json", true),
                    new IsJsonContentTypeTestCase("application/hal+json", true),
                    new IsJsonContentTypeTestCase("application/custom+json", true),
                    new IsJsonContentTypeTestCase("application/*+json", true),
                    new IsJsonContentTypeTestCase("application/xml", false),
                    new IsJsonContentTypeTestCase("invalid-media-type", false),
                    new IsJsonContentTypeTestCase("application/*", false),
                    new IsJsonContentTypeTestCase(null, false),
                    new IsJsonContentTypeTestCase("application/json;charset=utf-8", true)
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("params")
        void test(final IsJsonContentTypeTestCase testCase) {
            assertThat(isJsonContentType(testCase.contentType()), is(testCase.expectation()));
        }
    }

    @Nested
    class DefaultTests {
        @Test
        public void getCharsetFromContentType_doesNotFailIfContentTypeIsNull() {
            assertThat(ContentTypeUtils.getCharsetFromContentType((String) null).isPresent(), is(false));
        }

        @Test
        public void getCharsetFromContentType_charsetNotResolvable() {
            assertThat(ContentTypeUtils.getCharsetFromContentType("text/plain").isPresent(), is(false));
        }

        @Test
        public void getCharsetFromContentType_determinesTheCharset() {
            assertThat(ContentTypeUtils.getCharsetFromContentType("application/xml; charset=ISO-8859-1").get(), is(StandardCharsets.ISO_8859_1));
        }

        @Test
        public void getCharsetFromContentType_doesNotFailIfMultimapIsNull() {
            assertThat(ContentTypeUtils.getCharsetFromContentType((Multimap<String, String>) null).isPresent(), is(false));
        }

        @Test
        public void getCharsetFromContentType_doesNotFailIfMultimapDoesNotContainContentType() {
            final Multimap<String, String> headers = Multimaps.forMap(Collections.emptyMap());
            assertThat(ContentTypeUtils.getCharsetFromContentType(headers).isPresent(), is(false));
        }

        @Test
        public void getCharsetFromContentType_charsetNotResolvableForContentTypeHeader() {
            final Multimap<String, String> headers = Multimaps.forMap(Collections.singletonMap("Content-Type", "text/plain"));
            assertThat(ContentTypeUtils.getCharsetFromContentType(headers).isPresent(), is(false));
        }

        @Test
        public void getCharsetFromContentType_determinesTheCharsetForContentTypeHeader() {
            final Multimap<String, String> headers = Multimaps.forMap(Collections.singletonMap("Content-Type", "application/xml; charset=ISO-8859-1"));
            assertThat(ContentTypeUtils.getCharsetFromContentType(headers).get(), is(StandardCharsets.ISO_8859_1));
        }
    }

    record FindMostSpecificMatchTestCase(String description, String candidate, Set<String> apiContentTypes, String expected) {
        @Override
        public String toString() {
            return description;
        }
    }

    record MatchesAnyTestCase(String description, String candidate, Collection<String> apiContentTypes, boolean expected) {
        @Override
        public String toString() {
            return description;
        }
    }

    record IsJsonContentTypeTestCase(String contentType, boolean expectation) {
        @Override
        public String toString() {
            return contentType + " -> " + expectation;
        }
    }
}
