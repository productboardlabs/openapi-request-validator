package com.atlassian.oai.validator.restassured;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.http.Headers;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.FilterableRequestSpecification;
import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SwaggerValidationFilterTest {

    private static final String FILENAME_API_WITH_POST = "api-with-post.json";

    private SwaggerValidationFilter classUnderTest = new SwaggerValidationFilter("api.json");

    @Test(expected = IllegalArgumentException.class)
    public void create_withNullString_throwsException() {
        new SwaggerValidationFilter((String) null);
    }

    @Test(expected = NullPointerException.class)
    public void create_withNullSwaggerRequestResponseValidator_throwsException() {
        new SwaggerValidationFilter((SwaggerRequestResponseValidator) null);
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

    @Test
    public void filter_validationHandlesEmptyResponse() {
        assertThat(classUnderTest.filter(
                requestSpec("POST", "/empty"), null,
                emptyResponse()),
                notNullValue());
    }

    /**
     * Test result before fix:
     * com.atlassian.oai.validator.restassured.SwaggerValidationFilter$SwaggerValidationException: Validation failed.
     * [ERROR] POST operation not allowed on path '/hello/{name}'.
     */
    @Test
    public void filter_validationTakesMethodIntoAccount() {
        classUnderTest = new SwaggerValidationFilter(FILENAME_API_WITH_POST);
        assertThat(classUnderTest.filter(
            requestSpec("POST", "/hello/create", "{\"name\" : \"John Doe\"}"), null,
            response(201, "{\"message\":\"Hello !\"}")),
            notNullValue());
    }

    private FilterableRequestSpecification requestSpec(final String method, final String path, final String body) {
        final FilterableRequestSpecification request = mock(FilterableRequestSpecification.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getDerivedPath()).thenReturn(path);
        when(request.getBody()).thenReturn(body);
        return request;
    }

    private FilterableRequestSpecification requestSpec(final String method, final String path) {
        final FilterableRequestSpecification request = mock(FilterableRequestSpecification.class);
        when(request.getContentType()).thenReturn("application/json");
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
        when(response.getHeaders()).thenReturn(Headers.headers(new Header("Content-Type", "application/json")));

        final FilterContext ctx = mock(FilterContext.class);
        when(ctx.next(any(), any())).thenReturn(response);

        return ctx;
    }

    private FilterContext emptyResponse() {
        final ResponseBody responseBody = mock(ResponseBody.class);
        when(responseBody.asString()).thenReturn(""); // This is what RestAssured will return by default

        final Response response = mock(Response.class);
        when(response.getStatusCode()).thenReturn(204);
        when(response.getBody()).thenReturn(responseBody);
        when(response.getHeaders()).thenReturn(Headers.headers(new Header("Content-Length", "0")));
        when(response.getContentType()).thenReturn(""); // This is what RestAssured will return by default

        final FilterContext ctx = mock(FilterContext.class);
        when(ctx.next(any(), any())).thenReturn(response);

        return ctx;
    }

}
