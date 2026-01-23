package com.atlassian.oai.validator.example.requestlogging;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.EntityExchangeResult;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Testing with non-default context-path and a custom created
 * {@link OpenApiInteractionValidator} with base path override.
 *
 * @see RestRequestLoggingValidationConfig
 */
@AutoConfigureRestTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.contextPath=/v1", "spring.web.error.include-message=always"})
public class RestRequestValidationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    public void testGet_success() {
        final Map<String, List<String>> additionalHeaders = ImmutableMap
                .of("headerValue", singletonList("valueHeader"));
        final ResponseEntity<HashMap> response = restRequest("/spring/variablePath?requestParam=paramRequest",
                HttpMethod.GET, null /* no body */, additionalHeaders);

        // then: 'the response contains the header, path variable and query parameter'
        final Map<String, Object> expectedBody = ImmutableMap.of("headerValue", "valueHeader",
                "pathVariable", "variablePath",
                "requestParam", "paramRequest");
        assertOkRequest(response, expectedBody);
    }

    @Test
    public void testGet_invalidRequest() {
        final ResponseEntity<HashMap> response = restRequest("/spring/variablePath", HttpMethod.GET);

        // then: 'invalid request, the header and query parameter is missing'
        assertBadRequest(response,
                "Header parameter 'headerValue' is required on path '/spring/{pathVariable}' but not found in request.");
        assertBadRequest(response,
                "Query parameter 'requestParam' is required on path '/spring/{pathVariable}' but not found in request.");
    }

    @Test
    public void testGet_invalidResponse() {
        final Map<String, List<String>> additionalHeaders = ImmutableMap
                .of("headerValue", singletonList("valueHeader"));
        final ResponseEntity<HashMap> response = requestWithInvalidResponse("/spring/variablePath?requestParam=paramRequest",
                HttpMethod.GET, null /* no body */, additionalHeaders);

        // then: 'invalid response, empty body'
        assertBadResponse(response,
                "required property 'headerValue' not found");
        assertBadResponse(response,
            "required property 'pathVariable' not found");
        assertBadResponse(response,
            "required property 'requestParam' not found");
    }

    @Test
    public void testPost_success() {
        final Map<String, Object> sendBody = ImmutableMap.of("string", "text",
                "integer", 1022, "object", ImmutableMap.of("boolean", true));
        final ResponseEntity<HashMap> response = restRequest(
                "/spring", HttpMethod.POST, sendBody);

        // then: 'the response contains an exact copy of the request'
        assertOkRequest(response, sendBody);
    }

    @Test
    public void testPost_invalidRequest() {
        final Map<String, Object> sendBody = ImmutableMap.of("integer", "noInteger");
        final ResponseEntity<HashMap> response = restRequest("/spring",
                HttpMethod.POST, sendBody);

        // then: 'invalid request, all required request fields are missing'
        assertBadRequest(response,
                "required property 'object' not found");
        assertBadRequest(response,
            "required property 'string' not found");
    }

    @Test
    public void testPost_invalidResponse() {
        final Map<String, Object> sendBody = ImmutableMap.of("string", "text",
                "integer", 1022, "object", ImmutableMap.of("boolean", true));
        final ResponseEntity<HashMap> response = requestWithInvalidResponse(
                "/spring", HttpMethod.POST, sendBody, Collections.emptyMap());

        // then: 'invalid response, empty body'
        assertBadResponse(response,
                "required property 'integer' not found");
        assertBadResponse(response,
            "required property 'object' not found");
        assertBadResponse(response,
            "required property 'string' not found");
    }

    @Test
    public void testPostBlob_success() {
        final ResponseEntity<HashMap> response = octetStreamRequest(
                "/spring/post/blob", HttpMethod.POST, "bytes".getBytes(StandardCharsets.UTF_8));

        // then: 'the response contains the size of the send blob'
        final Map<String, Object> expectedBody = ImmutableMap.of("size", 5);
        assertOkRequest(response, expectedBody);
    }

    @Test
    public void testPut_success() {
        final Map<String, Object> sendBody = ImmutableMap.of("putValue", "valuePut");
        final ResponseEntity<HashMap> response = restRequest("/spring/variablePath",
                HttpMethod.PUT, sendBody);

        // then: 'the response contains a copy of the request including the path parameter'
        final Map<String, Object> expectedBody = ImmutableMap.<String, Object>builder()
                .putAll(sendBody).put("pathVariable", "variablePath").build();
        assertOkRequest(response, expectedBody);
    }

    @Test
    public void testPut_invalidRequest() {
        final ResponseEntity<HashMap> response = restRequest("/spring/variablePath", HttpMethod.PUT);

        // then: 'invalid request, missing body'
        assertBadRequest(response, "A request body is required but none found.");
    }

    @Test
    public void testPut_invalidResponse() {
        final Map<String, Object> sendBody = ImmutableMap.of("putValue", "valuePut");
        final ResponseEntity<HashMap> response = requestWithInvalidResponse("/spring/variablePath",
                HttpMethod.PUT, sendBody, Collections.emptyMap());

        // then: 'invalid response, empty body'
        assertBadResponse(response,
                "required property 'pathVariable' not found");
        assertBadResponse(response,
            "required property 'putValue' not found");
    }

    @Test
    public void testDelete_success() {
        final ResponseEntity<HashMap> response = restRequest("/spring/1", HttpMethod.DELETE);

        // then: 'a successful request'
        assertThat(response.getStatusCode(), equalTo(HttpStatus.NO_CONTENT));
    }

    @Test
    public void testDelete_invalidRequest() {
        final ResponseEntity<HashMap> response = restRequest("/spring/noInteger", HttpMethod.DELETE);

        // then: 'invalid request, the path variable is no integer'
        assertBadRequest(response,
                "string found, integer expected");
    }

    @Test
    public void testDelete_invalidResponse() {
        final ResponseEntity<HashMap> response = requestWithInvalidResponse("/spring/1", HttpMethod.DELETE,
                null, Collections.emptyMap());

        // then: 'invalid response, wrong status code'
        assertBadResponse(response,
                "Response status 200 not defined for path '/spring/{pathVariable}'.");
    }

    private ResponseEntity<HashMap> restRequest(final String uri, final HttpMethod method) {
        return restRequest(uri, method, null /* no body */);
    }

    private ResponseEntity<HashMap> restRequest(final String uri, final HttpMethod method, final Object body) {
        return restRequest(uri, method, body, ImmutableMap.of());
    }

    private ResponseEntity<HashMap> restRequest(final String uri, final HttpMethod method, final Object body,
                                                final Map<String, List<String>> additionalHeader) {
        final EntityExchangeResult<HashMap> result = restTestClient.method(method).uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> additionalHeader.forEach((key, values) -> headers.addAll(key, values)))
                .body(body != null ? body : "")
                .exchange()
                .expectBody(HashMap.class)
                .returnResult();
        return new ResponseEntity<>(result.getResponseBody(), result.getResponseHeaders(), result.getStatus());
    }

    private ResponseEntity<HashMap> requestWithInvalidResponse(final String uri, final HttpMethod method,
                                                               final Object body, final Map<String, List<String>> additionalHeader) {
        final EntityExchangeResult<HashMap> result = restTestClient.method(method).uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .headers(headers -> {
                    additionalHeader.forEach((key, values) -> headers.addAll(key, values));
                    headers.add("invalidResponse", "true");
                })
                .body(body != null ? body : "")
                .exchange()
                .expectBody(HashMap.class)
                .returnResult();
        return new ResponseEntity<>(result.getResponseBody(), result.getResponseHeaders(), result.getStatus());
    }

    private ResponseEntity<HashMap> octetStreamRequest(final String uri, final HttpMethod method, final Object body) {
        final EntityExchangeResult<HashMap> result = ((RestTestClient.RequestBodySpec) restTestClient.method(method).uri(uri))
                .accept(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange()
                .expectBody(HashMap.class)
                .returnResult();
        return new ResponseEntity<>(result.getResponseBody(), result.getResponseHeaders(), result.getStatus());
    }

    private void assertOkRequest(final ResponseEntity<HashMap> response, final Map<String, Object> body) {
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody().entrySet(), equalTo(body.entrySet()));
    }

    private void assertBadRequest(final ResponseEntity<HashMap> response, final String message) {
        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody().get("message").toString(), containsString(message));
    }

    private void assertBadResponse(final ResponseEntity<HashMap> response, final String message) {
        assertThat(response.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
        assertThat(response.getBody().get("message").toString(), containsString(message));
    }
}
