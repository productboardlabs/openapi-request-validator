package com.atlassian.oai.validator.model;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class NormalisedPathImplTest {

    @Test
    public void normalises_withNullPathPrefix() {
        final NormalisedPathImpl normalisedPath = new NormalisedPathImpl("foo/bar", null);
        assertThat(normalisedPath.normalised(), is("/foo/bar"));
    }

    @Test
    public void normalises_withEmptyPathPrefix() {
        final NormalisedPathImpl normalisedPath = new NormalisedPathImpl("foo/bar", "");
        assertThat(normalisedPath.normalised(), is("/foo/bar"));
    }

    @Test
    public void normalises_withSingleSlashPathPrefix() {
        final NormalisedPathImpl normalisedPath = new NormalisedPathImpl("foo/bar", "/");
        assertThat(normalisedPath.normalised(), is("/foo/bar"));
    }

    @Test
    public void normalises_withPathPrefix_withNoTrailingSlash() {
        final NormalisedPathImpl normalisedPath = new NormalisedPathImpl("foo/bar", "foo");
        assertThat(normalisedPath.normalised(), is("/bar"));
    }

    @Test
    public void normalises_withPathPrefix_withTrailingSlash() {
        final NormalisedPathImpl normalisedPath = new NormalisedPathImpl("foo/bar", "foo/");
        assertThat(normalisedPath.normalised(), is("/bar"));
    }

    @Test
    public void normalises_withPathPrefix_withLeadingSlash() {
        final NormalisedPathImpl normalisedPath = new NormalisedPathImpl("/foo/bar", "/foo");
        assertThat(normalisedPath.normalised(), is("/bar"));
    }

    @Test
    public void normalises_withPathPrefix_withLeadingAndTrailingSlash() {
        final NormalisedPathImpl normalisedPath = new NormalisedPathImpl("foo/bar", "/foo/");
        assertThat(normalisedPath.normalised(), is("/bar"));
    }

    @Test
    public void normalises_withPathPrefix_withWhitespace() {
        final NormalisedPathImpl normalisedPath = new NormalisedPathImpl("foo/bar", " /foo ");
        assertThat(normalisedPath.normalised(), is("/bar"));
    }

    @Test
    public void normalises_withNullPathPrefix_withWhitespace() {
        final NormalisedPathImpl normalisedPath = new NormalisedPathImpl("  foo/bar ", null);
        assertThat(normalisedPath.normalised(), is("/foo/bar"));
    }

    @Test
    public void normalises_withPathOnlyStartingWithPrefix() {
        final NormalisedPathImpl normalisedPath = new NormalisedPathImpl("/foo123", "/foo");
        assertThat(normalisedPath.normalised(), is("/foo123"));
    }

    @Test
    public void normalises_withTrailingSlash() {
        final NormalisedPathImpl normalisedPath = new NormalisedPathImpl("/foo/bar/", null);
        assertThat(normalisedPath.normalised(), is("/foo/bar/"));
    }
}
