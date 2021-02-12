package com.atlassian.oai.validator.example.exceptionhandler;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.error.include-message=always"})
public class RestRequestValidationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testGet_success() {
        final Map<String, List<String>> additionalHeaders = ImmutableMap
                .of("headerValue", Arrays.asList("valueHeader"));
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
        assertBadRequest(response, "validation.request.parameter.header.missing",
                "validation.request.parameter.query.missing");
    }

    @Test
    public void testGet_invalidResponse() {
        final Map<String, List<String>> additionalHeaders = ImmutableMap
                .of("headerValue", singletonList("valueHeader"));
        final ResponseEntity<HashMap> response = requestWithInvalidResponse("/spring/variablePath?requestParam=paramRequest",
                HttpMethod.GET, null /* no body */, additionalHeaders);

        // then: 'invalid response, empty body'
        assertBadResponse(response, "validation.response.body.schema.required");
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
                "validation.request.body.schema.required", "validation.request.body.schema.type");
    }

    @Test
    public void testPost_invalidResponse() {
        final Map<String, Object> sendBody = ImmutableMap.of("string", "text",
                "integer", 1022, "object", ImmutableMap.of("boolean", true));
        final ResponseEntity<HashMap> response = requestWithInvalidResponse(
                "/spring", HttpMethod.POST, sendBody, emptyMap());

        // then: 'invalid response, empty body'
        assertBadResponse(response, "validation.response.body.schema.required");
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
        assertBadRequest(response, "validation.request.body.missing");
    }

    @Test
    public void testPut_invalidResponse() {
        final Map<String, Object> sendBody = ImmutableMap.of("putValue", "valuePut");
        final ResponseEntity<HashMap> response = requestWithInvalidResponse("/spring/variablePath",
                HttpMethod.PUT, sendBody, emptyMap());

        // then: 'invalid response, empty body'
        assertBadResponse(response, "validation.response.body.schema.required");
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
        assertBadRequest(response, "validation.request.parameter.schema.type");
    }

    @Test
    public void testDelete_invalidResponse() {
        final ResponseEntity<HashMap> response = requestWithInvalidResponse("/spring/1", HttpMethod.DELETE,
                null, emptyMap());

        // then: 'invalid response, wrong status code'
        assertBadResponse(response, "validation.response.status.unknown");
    }

    private ResponseEntity<HashMap> restRequest(final String uri, final HttpMethod method) {
        return restRequest(uri, method, null /* no body */);
    }

    private ResponseEntity<HashMap> restRequest(final String uri, final HttpMethod method, final Object body) {
        return restRequest(uri, method, body, ImmutableMap.of());
    }

    private ResponseEntity<HashMap> restRequest(final String uri, final HttpMethod method, final Object body,
                                                final Map<String, List<String>> additionalHeader) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.putAll(additionalHeader);
        final HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(uri, method, entity, HashMap.class);
    }

    private ResponseEntity<HashMap> requestWithInvalidResponse(final String uri, final HttpMethod method,
                                                               final Object body, final Map<String, List<String>> additionalHeader) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.putAll(additionalHeader);
        headers.put("invalidResponse", singletonList("true"));
        final HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(uri, method, entity, HashMap.class);
    }

    private ResponseEntity<HashMap> octetStreamRequest(final String uri, final HttpMethod method, final Object body) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(singletonList(MediaType.APPLICATION_JSON));
        final HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(uri, method, entity, HashMap.class);
    }

    private void assertOkRequest(final ResponseEntity<HashMap> response, final Map<String, Object> body) {
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody().entrySet(), equalTo(body.entrySet()));
    }

    private void assertBadRequest(final ResponseEntity<HashMap> response, final String... expectedMessageKeys) {
        assertThat(response.getStatusCode(), equalTo(HttpStatus.UNPROCESSABLE_ENTITY));
        final List<String> messageKeys = ((List<HashMap>) response.getBody().get("messages")).stream()
                .map(map -> (String) map.get("key"))
                .collect(Collectors.toList());
        assertThat(messageKeys, Matchers.containsInAnyOrder(expectedMessageKeys));
    }

    private void assertBadResponse(final ResponseEntity<HashMap> response, final String... expectedMessageKeys) {
        assertThat(response.getStatusCode(), equalTo(HttpStatus.INTERNAL_SERVER_ERROR));
        final List<String> messageKeys = ((List<HashMap>) response.getBody().get("messages")).stream()
                .map(map -> (String) map.get("key"))
                .collect(Collectors.toList());
        assertThat(messageKeys, Matchers.containsInAnyOrder(expectedMessageKeys));
    }
}
