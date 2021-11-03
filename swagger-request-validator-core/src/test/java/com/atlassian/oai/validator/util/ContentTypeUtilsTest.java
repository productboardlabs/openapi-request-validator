package com.atlassian.oai.validator.util;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static com.atlassian.oai.validator.util.ContentTypeUtils.findMostSpecificMatch;
import static com.atlassian.oai.validator.util.ContentTypeUtils.isJsonContentType;
import static com.google.common.collect.ImmutableSet.of;
import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Enclosed.class)
public class ContentTypeUtilsTest {

    @RunWith(Parameterized.class)
    public static class FindMostSpecificMatchTests {

        @Parameterized.Parameters(name = "findMostSpecificMatch {0}")
        public static Object[][] params() {
            return new Object[][]{
                    {"returns empty when no API list", "text/plain", emptySet(), null},
                    {"returns empty when no matches", "text/plain", of("application/json", "application/*"), null},
                    {"returns match when direct match", "text/plain", of("application/json", "text/plain"), "text/plain"},
                    {"returns match when range match", "text/plain", of("application/json", "text/*"), "text/*"},
                    {"returns most specific match when multiple matches", "text/plain", of("application/json", "*/*", "text/*"), "text/*"},
                    {"returns global wildcard when supplied", "*/*", of("application/json", "*/*", "text/*"), "*/*"},
                    {"returns global wildcard when no more specific match", "application/xml", of("application/json", "*/*", "text/*"), "*/*"},
                    {"returns empty when invalid media type", "foop", of("application/json", "*/*", "text/*"), null},
                    {"handles case differences in params", "application/json;charset=utf-8",
                            of("application/json;charset=UTF-8", "application/json;charset=UTF-16"), "application/json;charset=UTF-8"},
                    {"handles whitespace differences in params", "application/json; charset=utf-8",
                            of("application/json;charset=UTF-8", "application/json;charset=UTF-16"), "application/json;charset=UTF-8"},
            };
        }

        @Parameterized.Parameter(0)
        public String description;

        @Parameterized.Parameter(1)
        public String candidate;

        @Parameterized.Parameter(2)
        public Set<String> apiContentTypes;

        @Parameterized.Parameter(3)
        public String expected;

        @Test
        public void test() {
            assertThat(
                    findMostSpecificMatch(candidate, apiContentTypes),
                    expected == null ? emptyOptional() : optionalWithValue(is(expected))
            );
        }
    }

    @RunWith(Parameterized.class)
    public static class MatchesAnyTests {

        @Parameterized.Parameters(name = "matchesAny {0}")
        public static Object[][] params() {
            return new Object[][]{
                    {"returns false when null candidate", null, of("application/json", "text/xml"), false},
                    {"returns false when empty API list", "application/json", emptySet(), false},
                    {"returns true when direct match", "application/json", of("application/json", "text/xml"), true},
                    {"returns false when no direct match", "application/json", of("text/json", "text/xml"), false},
                    {"returns true when subtype range match", "application/json", of("application/*", "text/xml"), true},
                    {"returns false when no subtype range match", "application/json", of("text/*", "text/xml"), false},
                    {"returns true when global match", "application/hal+json", of("*/*", "text/xml"), true},
                    {"returns true when charsets defined and direct match", "application/json;charset=utf-8", of("application/json", "text/xml"), true},
                    {"returns true when suffix match", "application/hal+json", of("application/json", "application/hal+json", "text/xml"), true},
            };
        }

        @Parameterized.Parameter(0)
        public String description;

        @Parameterized.Parameter(1)
        public String candidate;

        @Parameterized.Parameter(2)
        public Collection<String> apiContentTypes;

        @Parameterized.Parameter(3)
        public boolean expected;

        @Test
        public void test() {
            assertThat(ContentTypeUtils.matchesAny(candidate, apiContentTypes), is(expected));
        }

    }

    @RunWith(Parameterized.class)
    public static class IsJsonContentTypeTests {

        @Parameterized.Parameters(name = "isJsonContentType({0}) expects {1}")
        public static Object[][] params() {
            return new Object[][]{
                    {"application/json", true},
                    {"application/hal+json", true},
                    {"application/custom+json", true},
                    {"application/*+json", true},
                    {"application/xml", false},
                    {"invalid-media-type", false},
                    {"application/*", false},
                    {null, false},
                    {"application/json;charset=utf-8", true},
            };
        }

        @Parameterized.Parameter(0)
        public String contentType;

        @Parameterized.Parameter(1)
        public boolean expectation;

        @Test
        public void test() {
            assertThat(isJsonContentType(contentType), is(expectation));
        }
    }

    @RunWith(JUnit4.class)
    public static class DefaultTests {
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
}
