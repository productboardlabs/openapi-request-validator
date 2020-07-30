package com.atlassian.oai.validator.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import static com.google.common.collect.ImmutableList.of;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class HttpAcceptUtilsTest {

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Object[][] params() {
        return new Object[][]{
                {"null returns empty list", null, emptyList()},
                {"empty returns empty list", "", emptyList()},
                {"single media type returns empty single value", "application/json",
                        of("application/json")},
                {"strips whitespace from entries", "application/json, application/hal+json  ,  */*",
                        of("application/json", "application/hal+json", "*/*")},
                {"preserves params without commas", "application/hal+json;charset=UTF-8, foo/bar; q=0.5",
                        of("application/hal+json;charset=UTF-8", "foo/bar; q=0.5")},
                {"preserves params with commas", "application/hal+json;charset=UTF-8, foo/bar; q=\"A,B,C\"",
                        of("application/hal+json;charset=UTF-8", "foo/bar; q=\"A,B,C\"")},
        };
    }

    @Parameterized.Parameter(0)
    public String name;

    @Parameterized.Parameter(1)
    public String input;

    @Parameterized.Parameter(2)
    public List<String> expected;

    @Test
    public void test() {
        assertThat(HttpAcceptUtils.splitAcceptHeader(input), is(expected));
    }

}