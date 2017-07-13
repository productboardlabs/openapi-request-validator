package com.atlassian.oai.validator.model;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertThat;

public class SimpleResponseTest {

    @Test
    public void header_areNotMandatory() {
        final Response response = SimpleResponse.Builder.ok().build();

        assertThat(response.getHeaderValues("foo"), empty());
        assertThat(response.getHeaderValue("foo").isPresent(), is(false));
    }

    @Test
    public void header_namesAreCaseInsensitive() {
        final Response response = SimpleResponse.Builder.ok()
                .withHeader("foo")
                .withHeader("Foo", "bar0")
                .withHeader("fOO", "bar1", "bar2")
                .withHeader("FOO", Arrays.asList("bar3", "bar4"))
                .build();

        assertThat(response.getHeaderValues("FOO"), containsInAnyOrder("", "bar0", "bar1", "bar2", "bar3", "bar4"));
        assertThat(response.getHeaderValues("foo"), containsInAnyOrder("", "bar0", "bar1", "bar2", "bar3", "bar4"));
        assertThat(response.getHeaderValue("Foo").get(), equalTo("")); // the first set value to "foo"
    }

    @Test
    public void header_unsetValuesConsideredAsEmpty() {
        final Response response = SimpleResponse.Builder.ok()
                .withHeader("foo")
                .build();

        assertThat(response.getHeaderValues("foo"), containsInAnyOrder(""));
        assertThat(response.getHeaderValue("foo").get(), isEmptyString());
    }

    @Test
    public void header_valuesCanBeEmpty() {
        final Response response = SimpleResponse.Builder.ok()
                .withHeader("foo", "")
                .build();

        assertThat(response.getHeaderValues("foo"), containsInAnyOrder(""));
        assertThat(response.getHeaderValue("foo").get(), isEmptyString());
    }

    @Test
    public void header_valuesCanHaveOneValue() {
        final Response response = SimpleResponse.Builder.ok()
                .withHeader("foo", "bar")
                .build();

        assertThat(response.getHeaderValues("foo"), containsInAnyOrder("bar"));
        assertThat(response.getHeaderValue("foo").get(), equalTo("bar"));
    }

    @Test
    public void header_valuesCanHaveMultipleValues() {
        final Response response = SimpleResponse.Builder.ok()
                .withHeader("fOO", "bar1", "bar2")
                .build();

        assertThat(response.getHeaderValues("foo"), containsInAnyOrder("bar1", "bar2"));
        assertThat(response.getHeaderValue("foo").get(), equalTo("bar1"));
    }

    @Test
    public void body_isNotMandatory_andDoesNotNeedToBeSet() {
        final Response response = SimpleResponse.Builder.ok()
                .build();

        assertThat(response.getBody().isPresent(), is(false));
    }

    @Test
    public void body_isNotMandatory_andCanBeSetAsNull() {
        final Response response = SimpleResponse.Builder.ok()
                .withBody(null)
                .build();

        assertThat(response.getBody().isPresent(), is(false));
    }

    @Test
    public void body_canBeEmpty() {
        final Response response = SimpleResponse.Builder.ok()
                .withBody("")
                .build();

        assertThat(response.getBody().get(), isEmptyString());
    }

    @Test
    public void body_canBeSet() {
        final Response response = SimpleResponse.Builder.ok()
                .withBody("Body")
                .build();

        assertThat(response.getBody().get(), equalTo("Body"));
    }

    @Test
    public void status_isSet() {
        final Response response = SimpleResponse.Builder.status(101).build();

        assertThat(response.getStatus(), equalTo(101));
    }

    @Test
    public void status_convenienceMethods() {
        assertThat(SimpleResponse.Builder.ok().build().getStatus(), is(200));
        assertThat(SimpleResponse.Builder.noContent().build().getStatus(), is(204));
        assertThat(SimpleResponse.Builder.badRequest().build().getStatus(), is(400));
        assertThat(SimpleResponse.Builder.notFound().build().getStatus(), is(404));
        assertThat(SimpleResponse.Builder.serverError().build().getStatus(), is(500));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getHeaderValues_isUnmodifiable() {
        SimpleResponse.Builder.ok().build().getHeaderValues("foo").add("bar");
    }
}
