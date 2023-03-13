package com.atlassian.oai.validator.springweb.client;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class OpenApiValidationClientHttpRequestInterceptorTest {

    private static final String FILENAME_API_WITH_POST = "api-with-post.json";

    private OpenApiValidationClientHttpRequestInterceptor classUnderTest = new OpenApiValidationClientHttpRequestInterceptor("api.json");

    @Test(expected = IllegalArgumentException.class)
    public void create_withNullString_throwsException() {
        new OpenApiValidationClientHttpRequestInterceptor((String) null);
    }

    @Test(expected = NullPointerException.class)
    public void create_withNullSwaggerRequestResponseValidator_throwsException() {
        new OpenApiValidationClientHttpRequestInterceptor((OpenApiInteractionValidator) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_withEmpty_throwsException() {
        new OpenApiValidationClientHttpRequestInterceptor("");
    }

    @Test
    public void filter_returnsResponse_ifValidationSucceeds() throws IOException {
        assertThat(executeInterceptor(HttpMethod.GET, "/hello/bob", null,
                response(HttpStatus.OK, "{\"message\":\"Hello bob!\"}")),
                notNullValue());
    }

    @Test(expected = OpenApiValidationClientHttpRequestInterceptor.OpenApiValidationException.class)
    public void filter_throwsException_ifValidationFails() throws IOException {
        executeInterceptor(HttpMethod.GET, "/hello/bob", null,
                response(HttpStatus.OK, "{\"msg\":\"Hello bob!\"}")); // Wrong field name
    }

    @Test
    public void filter_validationHandlesEmptyResponse() throws IOException {
        assertThat(executeInterceptor(HttpMethod.POST, "/empty", null, response(HttpStatus.NO_CONTENT, "")),
                notNullValue());
    }

    /**
     * Test result before fix:
     * OpenApiValidationFilter$OpenApiValidationException: Validation failed.
     * [ERROR] POST operation not allowed on path '/hello/{name}'.
     */
    @Test
    public void filter_validationTakesMethodIntoAccount() throws IOException {
        classUnderTest = new OpenApiValidationClientHttpRequestInterceptor(FILENAME_API_WITH_POST);
        assertThat(executeInterceptor(HttpMethod.POST, "/hello/create", "{\"name\" : \"John Doe\"}",
                response(HttpStatus.CREATED, "{\"message\":\"Hello !\"}")),
                notNullValue());
    }

    private ClientHttpResponse executeInterceptor(final HttpMethod method, final String path,
                                                  final String body, final MockClientHttpResponse response) throws IOException {
        final InterceptingClientHttpRequestFactory requestFactory = new InterceptingClientHttpRequestFactory((uri, httpMethod) -> {
            final MockClientHttpRequest request = new MockClientHttpRequest(httpMethod, uri);
            request.setResponse(response);
            return request;
        }, Collections.singletonList(classUnderTest));
        final ClientHttpRequest request = requestFactory.createRequest(URI.create(path), method);
        if (StringUtils.hasText(body)) {
            try (OutputStream out = request.getBody()) {
                StreamUtils.copy(body, StandardCharsets.ISO_8859_1, out);
            }
        }
        return request.execute();
    }

    private MockClientHttpResponse response(final HttpStatus status, final String body) {
        final MockClientHttpResponse response = new MockClientHttpResponse(body.getBytes(StandardCharsets.ISO_8859_1), status);
        if (StringUtils.hasText(body)) {
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        }
        return response;
    }

}
