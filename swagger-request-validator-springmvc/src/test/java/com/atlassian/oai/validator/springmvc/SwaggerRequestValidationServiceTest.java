package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class SwaggerRequestValidationServiceTest {

    private SwaggerRequestValidationService classUnderTest;

    private OpenApiInteractionValidator requestValidator;

    private static Map<String, Collection<String>> getHeadersFromResponse(final Response response) {
        final Field headersField = ReflectionUtils.findField(SimpleResponse.class, "headers");
        ReflectionUtils.makeAccessible(headersField);
        return (Map<String, Collection<String>>) ReflectionUtils.getField(headersField, response);
    }

    @Before
    public void setUp() {
        requestValidator = mock(OpenApiInteractionValidator.class);
        classUnderTest = new SwaggerRequestValidationService(requestValidator);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_failsWithoutRequiredValidator() throws IOException {
        new SwaggerRequestValidationService((OpenApiInteractionValidator) null);
    }

    @Test
    public void constructor_withEncodedResource() throws IOException {
        final EncodedResource encodedResource = mock(EncodedResource.class);
        when(encodedResource.getReader())
                .thenReturn(new InputStreamReader(getClass().getResourceAsStream("/api-spring-test.json")));

        final SwaggerRequestValidationService service = new SwaggerRequestValidationService(encodedResource);
        assertThat(service, notNullValue());
    }

    @Test
    public void aMissingPathIsNotTreatedAsError() throws IOException {
        // given:
        final EncodedResource encodedResource = mock(EncodedResource.class);
        when(encodedResource.getReader())
                .thenReturn(new InputStreamReader(getClass().getResourceAsStream("/api-spring-test.json")));
        final SwaggerRequestValidationService service = new SwaggerRequestValidationService(encodedResource);
        assertThat(service, notNullValue());

        // and:
        final Request request = new SimpleRequest.Builder(Request.Method.GET, "/unknownPath").build();

        // when:
        final ValidationReport validationReport = service.validateRequest(request);

        // then:
        assertThat(validationReport.hasErrors(), is(false));
    }

    @Test(expected = NullPointerException.class)
    public void buildRequest_failsWithoutRequiredRequest() throws IOException {
        classUnderTest.buildRequest(null);
    }

    @Test
    public void buildRequest_withoutBodyHeaderAndQueryString() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getMethod()).thenReturn("GET");
        when(servletRequest.getQueryString()).thenReturn("");
        when(servletRequest.getRequestURI()).thenReturn("/swagger-request-validator");
        when(servletRequest.getContentLength()).thenReturn(-1);
        final BufferedReader reader = new BufferedReader(new StringReader(""));
        when(servletRequest.getReader()).thenReturn(reader);
        when(servletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        final Request result = classUnderTest.buildRequest(servletRequest);

        assertThat(result.getPath(), equalTo("/swagger-request-validator"));
        assertThat(result.getMethod(), equalTo(Request.Method.GET));
        assertThat(result.getBody().isPresent(), equalTo(false));
        assertThat(result.getHeaders().size(), equalTo(0));
        assertThat(result.getQueryParameters().size(), equalTo(0));
    }

    @Test
    public void buildRequest_withEmptyBody() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getMethod()).thenReturn("PUT");
        when(servletRequest.getQueryString()).thenReturn("");
        when(servletRequest.getRequestURI()).thenReturn("/swagger-request-validator");
        when(servletRequest.getContentLength()).thenReturn(0);
        final BufferedReader reader = new BufferedReader(new StringReader(""));
        when(servletRequest.getReader()).thenReturn(reader);
        when(servletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        final Request result = classUnderTest.buildRequest(servletRequest);

        assertThat(result.getPath(), equalTo("/swagger-request-validator"));
        assertThat(result.getMethod(), equalTo(Request.Method.PUT));
        assertThat(result.getBody().isPresent(), equalTo(true));
    }

    @Test
    public void buildRequest_withBodyHeaderAndQueryString() throws IOException {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getMethod()).thenReturn("POST");
        when(servletRequest.getQueryString())
                .thenReturn("query1=QUERY_ONE&query2=query_two&query2=QUERY_TWO");
        when(servletRequest.getRequestURI()).thenReturn("/swagger-request-validator");
        when(servletRequest.getContentLength()).thenReturn(-1);
        final BufferedReader reader = new BufferedReader(new StringReader("Body"));
        when(servletRequest.getReader()).thenReturn(reader);
        when(servletRequest.getHeaderNames())
                .thenReturn(Collections.enumeration(asList("header1", "header2")));
        when(servletRequest.getHeaders("header1"))
                .thenReturn(Collections.enumeration(asList("HEADER_ONE")));
        when(servletRequest.getHeaders("header2"))
                .thenReturn(Collections.enumeration(asList("header_two", "HEADER_TWO")));

        final Request result = classUnderTest.buildRequest(servletRequest);

        assertThat(result.getPath(), equalTo("/swagger-request-validator"));
        assertThat(result.getMethod(), equalTo(Request.Method.POST));
        assertThat(result.getBody().get(), equalTo("Body"));
        assertThat(result.getHeaders().size(), equalTo(2));
        assertThat(result.getHeaderValues("header1"),
                equalTo(asList("HEADER_ONE")));
        assertThat(result.getHeaderValues("header2"),
                equalTo(asList("header_two", "HEADER_TWO")));
        assertThat(result.getQueryParameters().size(), equalTo(2));
        assertThat(result.getQueryParameterValues("query1"),
                equalTo(asList("QUERY_ONE")));
        assertThat(result.getQueryParameterValues("query2"),
                equalTo(asList("query_two", "QUERY_TWO")));
    }

    @Test(expected = NullPointerException.class)
    public void buildResponse_failsWithoutRequiredResponse() throws IOException {
        // expect:
        classUnderTest.buildResponse(null);
    }

    @Test
    public void buildResponse_withEmptyBodyAndHeader() throws IOException {
        // given:
        final ContentCachingResponseWrapper servletResponse = mock(ContentCachingResponseWrapper.class);

        // and:
        when(servletResponse.getStatusCode()).thenReturn(202);
        when(servletResponse.getContentAsByteArray()).thenReturn(new byte[0]);
        when(servletResponse.getCharacterEncoding()).thenReturn("ISO-8859-1");
        when(servletResponse.getHeaderNames()).thenReturn(emptySet());

        // when:
        final Response result = classUnderTest.buildResponse(servletResponse);

        // then:
        assertThat(result.getBody().isPresent(), equalTo(true));
        assertThat(result.getStatus(), is(202));
        assertThat(getHeadersFromResponse(result).size(), is(1)); // Content type header will be set
    }

    @Test
    public void buildResponse_withBodyAndHeader() throws IOException {
        // given:
        final ContentCachingResponseWrapper servletResponse = mock(ContentCachingResponseWrapper.class);

        // and:
        when(servletResponse.getStatusCode()).thenReturn(404);
        when(servletResponse.getContentAsByteArray()).thenReturn(new byte[0]);
        when(servletResponse.getCharacterEncoding()).thenReturn("UTF-8");
        when(servletResponse.getHeaderNames()).thenReturn(asList("header 1", "header 2"));
        when(servletResponse.getHeaders("header 1")).thenReturn(asList("header value 1", "header value 2"));
        when(servletResponse.getHeaders("header 2")).thenReturn(asList("header value 3"));
        when(servletResponse.getContentType()).thenReturn("application/json");

        // when:
        final Response result = classUnderTest.buildResponse(servletResponse);

        // then:
        assertThat(result.getBody().isPresent(), equalTo(true));
        assertThat(result.getStatus(), is(404));
        assertThat(getHeadersFromResponse(result), equalTo(ImmutableMap.of(
                "header 1", asList("header value 1", "header value 2"),
                "header 2", asList("header value 3"),
                "Content-Type", asList("application/json")
        )));
    }

    @Test
    public void validateRequest_returnsTheValidationReport() {
        final Request request = mock(Request.class);
        final ValidationReport validationReport = mock(ValidationReport.class);
        when(requestValidator.validateRequest(request)).thenReturn(validationReport);

        final ValidationReport result = classUnderTest.validateRequest(request);

        Mockito.verify(requestValidator, times(1)).validateRequest(request);
        assertThat(result, is(validationReport));
    }

    @Test
    public void validateResponse_returnsTheValidationReport() {
        // given:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final Response response = mock(Response.class);
        final ValidationReport validationReport = mock(ValidationReport.class);

        // and:
        when(servletRequest.getMethod()).thenReturn("POST");
        when(servletRequest.getQueryString()).thenReturn(null);
        when(servletRequest.getRequestURI()).thenReturn("/swagger-request-validator");

        when(requestValidator.validateResponse("/swagger-request-validator",
                Request.Method.POST, response)).thenReturn(validationReport);

        // when:
        final ValidationReport result = classUnderTest.validateResponse(servletRequest, response);

        // then:
        Mockito.verify(requestValidator, times(1))
                .validateResponse("/swagger-request-validator", Request.Method.POST, response);
        assertThat(result, is(validationReport));
    }
}
