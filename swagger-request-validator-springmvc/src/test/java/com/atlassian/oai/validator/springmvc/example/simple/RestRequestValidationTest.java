package com.atlassian.oai.validator.springmvc.example.simple;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
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
        assertBadRequest(response,
                "Header parameter 'headerValue' is required on path '/spring/{pathVariable}' but not found in request., " +
                        "Query parameter 'requestParam' is required on path '/spring/{pathVariable}' but not found in request.");
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
                "Object has missing required properties ([\"object\",\"string\"]), " +
                        "[Path '/integer'] Instance type (string) does not match any allowed primitive type (allowed: [\"integer\"])");
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
                "Instance type (string) does not match any allowed primitive type (allowed: [\"integer\"])");
    }

    private ResponseEntity<HashMap> restRequest(final String uri, final HttpMethod method) {
        return restRequest(uri, method, null /* no body */);
    }

    private ResponseEntity<HashMap> restRequest(final String uri, final HttpMethod method,
                                                final Object body) {
        return restRequest(uri, method, body, ImmutableMap.of());
    }

    private ResponseEntity<HashMap> restRequest(final String uri, final HttpMethod method,
                                                final Object body,
                                                final Map<String, List<String>> additionalHeader) {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.putAll(additionalHeader);
        final HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        return restTemplate.exchange(uri, method, entity, HashMap.class);
    }

    private void assertOkRequest(final ResponseEntity<HashMap> response, final Map<String, Object> body) {
        assertThat(response.getStatusCode(), equalTo(HttpStatus.OK));
        assertThat(response.getBody().entrySet(), equalTo(body.entrySet()));
    }

    private void assertBadRequest(final ResponseEntity<HashMap> response, final String message) {
        assertThat(response.getStatusCode(), equalTo(HttpStatus.BAD_REQUEST));
        assertThat(response.getBody().get("message"), equalTo(message));
    }
}