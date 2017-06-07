package com.atlassian.oai.validator.model;

import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class SimpleRequestTest {

    @Test
    public void buildingRequestAndTestingGetter() {
        final SimpleRequest request = SimpleRequest.Builder.delete("/path")
                .withHeader("header0")
                .withHeader("hEAdEr1", Arrays.asList("abc", "123"))
                .withHeader("HEADER2", "+-*")
                .withHeader("header3", "123", "abc")
                .withHeader("header4", new String[]{"xyz"})
                .withHeader("header4", new String[]{"XYZ"})
                .withBody("body")
                .withQueryParam("query0")
                .withQueryParam("qUEry1", Arrays.asList("abcd", "1234"))
                .withQueryParam("QUERY2", "+-*/")
                .withQueryParam("query3", "1234", "abcd")
                .withQueryParam("query4", new String[]{"xyz"})
                .withQueryParam("query4", new String[]{"XYZ"})
                .build();

        assertThat(request.getPath(), is("/path"));
        assertThat(request.getMethod(), is(Request.Method.DELETE));
        assertTrue(request.getBody().isPresent());
        assertThat(request.getBody().get(), is("body"));
        assertThat(request.getQueryParameters(), containsInAnyOrder("query0", "query1", "query2", "query3", "query4"));
        assertThat(request.getQueryParameterValues("query1"), containsInAnyOrder("abcd", "1234"));
        assertThat(request.getQueryParameterValues("query2"), containsInAnyOrder("+-*/"));
        assertThat(request.getQueryParameterValues("QUERY3"), containsInAnyOrder("1234", "abcd"));
        assertThat(request.getQueryParameterValues("QuErY4"), containsInAnyOrder("xyz", "XYZ"));
        assertTrue(request.getQueryParameterValues("query0").isEmpty());
        assertTrue(request.getQueryParameterValues("does not exist").isEmpty());
        assertThat(request.getHeaders().keySet(), containsInAnyOrder("header0", "hEAdEr1", "HEADER2", "header3", "header4"));
        assertThat(request.getHeaderValues("header1"), containsInAnyOrder("abc", "123"));
        assertThat(request.getHeaderValues("header2"), containsInAnyOrder("+-*"));
        assertThat(request.getHeaderValues("HEADER3"), containsInAnyOrder("123", "abc"));
        assertThat(request.getHeaderValues("HeAdEr4"), containsInAnyOrder("xyz", "XYZ"));
        assertTrue(request.getHeaderValues("header0").isEmpty());
        assertTrue(request.getHeaderValues("does not exist").isEmpty());
    }
}
