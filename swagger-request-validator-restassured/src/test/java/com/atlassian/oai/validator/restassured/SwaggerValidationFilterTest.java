package com.atlassian.oai.validator.restassured;

import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.FilterableRequestSpecification;
import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SwaggerValidationFilterTest {

    private SwaggerValidationFilter classUnderTest = new SwaggerValidationFilter("api.json");

    @Test(expected = IllegalArgumentException.class)
    public void create_withNull_throwsException() {
        new SwaggerValidationFilter(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_withEmpty_throwsException() {
        new SwaggerValidationFilter("");
    }

    @Test
    public void filter_returnsResponse_ifValidationSucceeds() {
        assertThat(classUnderTest.filter(
                requestSpec("GET", "/hello/bob"), null,
                response(200, "{\"message\":\"Hello bob!\"}")),
                notNullValue());
    }

    @Test (expected = SwaggerValidationFilter.SwaggerValidationException.class)
    public void filter_throwsException_ifValidationFails() {
        assertThat(classUnderTest.filter(
                requestSpec("GET", "/hello/bob"), null,
                response(200, "{\"msg\":\"Hello bob!\"}")), // Wrong field name
                notNullValue());
    }

    private FilterableRequestSpecification requestSpec(final String method, final String path) {
        final FilterableRequestSpecification request = mock(FilterableRequestSpecification.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getDerivedPath()).thenReturn(path);
        return request;
    }

    private FilterContext response(final int status, final String body) {
        final ResponseBody responseBody = mock(ResponseBody.class);
        when(responseBody.asString()).thenReturn(body);

        final Response response = mock(Response.class);
        when(response.getStatusCode()).thenReturn(status);
        when(response.getBody()).thenReturn(responseBody);

        final FilterContext ctx = mock(FilterContext.class);
        when(ctx.next(any(), any())).thenReturn(response);

        return ctx;
    }
}
