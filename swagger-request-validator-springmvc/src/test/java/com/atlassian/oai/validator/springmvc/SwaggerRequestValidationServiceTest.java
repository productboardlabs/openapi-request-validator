package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;

public class SwaggerRequestValidationServiceTest {

    private SwaggerRequestValidationService classUnderTest;

    private SwaggerRequestResponseValidator requestValidator;

    private static Map<String, Collection<String>> getHeadersFromResponse(final Response response) {
        final Field headersField = ReflectionUtils.findField(SimpleResponse.class, "headers");
        ReflectionUtils.makeAccessible(headersField);
        return (Map<String, Collection<String>>) ReflectionUtils.getField(headersField, response);
    }

    @Before
    public void setUp() {
        this.requestValidator = Mockito.mock(SwaggerRequestResponseValidator.class);
        this.classUnderTest = new SwaggerRequestValidationService(requestValidator);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_failsWithoutRequiredValidator() throws IOException {
        new SwaggerRequestValidationService((SwaggerRequestResponseValidator) null);
    }

    @Test
    public void aMissingPathIsNotTreatedAsError() throws IOException {
        // given:
        final EncodedResource encodedResource = Mockito.mock(EncodedResource.class);
        Mockito.when(encodedResource.getReader()).thenReturn(new StringReader("{}"));
        final SwaggerRequestValidationService service = new SwaggerRequestValidationService(encodedResource);

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
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(servletRequest.getMethod()).thenReturn("GET");
        Mockito.when(servletRequest.getQueryString()).thenReturn("");
        Mockito.when(servletRequest.getRequestURI()).thenReturn("/swagger-request-validator");
        Mockito.when(servletRequest.getContentLength()).thenReturn(-1);
        final BufferedReader reader = new BufferedReader(new StringReader(""));
        Mockito.when(servletRequest.getReader()).thenReturn(reader);
        Mockito.when(servletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        final Request result = classUnderTest.buildRequest(servletRequest);

        Assert.assertThat(result.getPath(), equalTo("/swagger-request-validator"));
        Assert.assertThat(result.getMethod(), equalTo(Request.Method.GET));
        Assert.assertThat(result.getBody().isPresent(), equalTo(false));
        Assert.assertThat(result.getHeaders().size(), equalTo(0));
        Assert.assertThat(result.getQueryParameters().size(), equalTo(0));
    }

    @Test
    public void buildRequest_withEmptyBody() throws IOException {
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(servletRequest.getMethod()).thenReturn("PUT");
        Mockito.when(servletRequest.getQueryString()).thenReturn("");
        Mockito.when(servletRequest.getRequestURI()).thenReturn("/swagger-request-validator");
        Mockito.when(servletRequest.getContentLength()).thenReturn(0);
        final BufferedReader reader = new BufferedReader(new StringReader(""));
        Mockito.when(servletRequest.getReader()).thenReturn(reader);
        Mockito.when(servletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        final Request result = classUnderTest.buildRequest(servletRequest);

        Assert.assertThat(result.getPath(), equalTo("/swagger-request-validator"));
        Assert.assertThat(result.getMethod(), equalTo(Request.Method.PUT));
        Assert.assertThat(result.getBody().isPresent(), equalTo(true));
    }

    @Test
    public void buildRequest_withBodyHeaderAndQueryString() throws IOException {
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        Mockito.when(servletRequest.getMethod()).thenReturn("POST");
        Mockito.when(servletRequest.getQueryString())
                .thenReturn("query1=QUERY_ONE&query2=query_two&query2=QUERY_TWO");
        Mockito.when(servletRequest.getRequestURI()).thenReturn("/swagger-request-validator");
        Mockito.when(servletRequest.getContentLength()).thenReturn(-1);
        final BufferedReader reader = new BufferedReader(new StringReader("Body"));
        Mockito.when(servletRequest.getReader()).thenReturn(reader);
        Mockito.when(servletRequest.getHeaderNames())
                .thenReturn(Collections.enumeration(Arrays.asList("header1", "header2")));
        Mockito.when(servletRequest.getHeaders("header1"))
                .thenReturn(Collections.enumeration(Arrays.asList("HEADER_ONE")));
        Mockito.when(servletRequest.getHeaders("header2"))
                .thenReturn(Collections.enumeration(Arrays.asList("header_two", "HEADER_TWO")));

        final Request result = classUnderTest.buildRequest(servletRequest);

        Assert.assertThat(result.getPath(), equalTo("/swagger-request-validator"));
        Assert.assertThat(result.getMethod(), equalTo(Request.Method.POST));
        Assert.assertThat(result.getBody().get(), equalTo("Body"));
        Assert.assertThat(result.getHeaders().size(), equalTo(2));
        Assert.assertThat(result.getHeaderValues("header1"),
                equalTo(Arrays.asList("HEADER_ONE")));
        Assert.assertThat(result.getHeaderValues("header2"),
                equalTo(Arrays.asList("header_two", "HEADER_TWO")));
        Assert.assertThat(result.getQueryParameters().size(), equalTo(2));
        Assert.assertThat(result.getQueryParameterValues("query1"),
                equalTo(Arrays.asList("QUERY_ONE")));
        Assert.assertThat(result.getQueryParameterValues("query2"),
                equalTo(Arrays.asList("query_two", "QUERY_TWO")));
    }

    @Test(expected = NullPointerException.class)
    public void buildResponse_failsWithoutRequiredResponse() throws IOException {
        // expect:
        classUnderTest.buildResponse(null);
    }

    @Test
    public void buildResponse_withEmptyBodyAndHeader() throws IOException {
        // given:
        final ContentCachingResponseWrapper servletResponse = Mockito.mock(ContentCachingResponseWrapper.class);

        // and:
        Mockito.when(servletResponse.getStatusCode()).thenReturn(202);
        Mockito.when(servletResponse.getContentAsByteArray()).thenReturn(new byte[0]);
        Mockito.when(servletResponse.getCharacterEncoding()).thenReturn("ISO-8859-1");
        Mockito.when(servletResponse.getHeaderNames()).thenReturn(Collections.emptySet());

        // when:
        final Response result = classUnderTest.buildResponse(servletResponse);

        // then:
        Assert.assertThat(result.getBody().isPresent(), equalTo(true));
        Assert.assertThat(result.getStatus(), is(202));
        Assert.assertThat(getHeadersFromResponse(result).isEmpty(), is(true));
    }

    @Test
    public void buildResponse_withBodyAndHeader() throws IOException {
        // given:
        final ContentCachingResponseWrapper servletResponse = Mockito.mock(ContentCachingResponseWrapper.class);

        // and:
        Mockito.when(servletResponse.getStatusCode()).thenReturn(404);
        Mockito.when(servletResponse.getContentAsByteArray()).thenReturn(new byte[0]);
        Mockito.when(servletResponse.getCharacterEncoding()).thenReturn("UTF-8");
        Mockito.when(servletResponse.getHeaderNames()).thenReturn(Arrays.asList("header 1", "header 2"));
        Mockito.when(servletResponse.getHeaders("header 1"))
                .thenReturn(Arrays.asList("header value 1", "header value 2"));
        Mockito.when(servletResponse.getHeaders("header 2")).thenReturn(Arrays.asList("header value 3"));

        // when:
        final Response result = classUnderTest.buildResponse(servletResponse);

        // then:
        Assert.assertThat(result.getBody().isPresent(), equalTo(true));
        Assert.assertThat(result.getStatus(), is(404));
        Assert.assertThat(getHeadersFromResponse(result), equalTo(ImmutableMap.of(
                "header 1", Arrays.asList("header value 1", "header value 2"),
                "header 2", Arrays.asList("header value 3")
        )));
    }

    @Test
    public void validateRequest_returnsTheValidationReport() {
        final Request request = Mockito.mock(Request.class);
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);
        Mockito.when(requestValidator.validateRequest(request)).thenReturn(validationReport);

        final ValidationReport result = classUnderTest.validateRequest(request);

        Mockito.verify(requestValidator, times(1)).validateRequest(request);
        Assert.assertThat(result, is(validationReport));
    }

    @Test
    public void validateResponse_returnsTheValidationReport() {
        // given:
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        final Response response = Mockito.mock(Response.class);
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);

        // and:
        Mockito.when(servletRequest.getMethod()).thenReturn("POST");
        Mockito.when(servletRequest.getQueryString()).thenReturn(null);
        Mockito.when(servletRequest.getRequestURI()).thenReturn("/swagger-request-validator");

        Mockito.when(requestValidator.validateResponse("/swagger-request-validator",
                Request.Method.POST, response)).thenReturn(validationReport);

        // when:
        final ValidationReport result = classUnderTest.validateResponse(servletRequest, response);

        // then:
        Mockito.verify(requestValidator, times(1))
                .validateResponse("/swagger-request-validator", Request.Method.POST, response);
        Assert.assertThat(result, is(validationReport));
    }
}
