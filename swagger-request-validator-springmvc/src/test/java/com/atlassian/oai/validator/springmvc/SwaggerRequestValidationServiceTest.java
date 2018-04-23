package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.support.EncodedResource;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class SwaggerRequestValidationServiceTest {

    private SwaggerRequestValidationService classUnderTest;

    private SwaggerRequestResponseValidator requestValidator;

    @Before
    public void setUp() {
        requestValidator = Mockito.mock(SwaggerRequestResponseValidator.class);
        classUnderTest = new SwaggerRequestValidationService(requestValidator);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_failsWithoutRequiredValidator() throws IOException {
        new SwaggerRequestValidationService((SwaggerRequestResponseValidator) null);
    }

    @Test
    public void constructor_withEncodedResource() throws IOException {
        final EncodedResource encodedResource = Mockito.mock(EncodedResource.class);
        when(encodedResource.getReader())
                .thenReturn(new InputStreamReader(getClass().getResourceAsStream("/api-spring-test.json")));

        final SwaggerRequestValidationService service = new SwaggerRequestValidationService(encodedResource);
        assertThat(service, notNullValue());
    }

    @Test(expected = NullPointerException.class)
    public void buildRequest_failsWithoutRequiredRequest() throws IOException {
        classUnderTest.buildRequest(null);
    }

    @Test
    public void buildRequest_withoutBodyHeaderAndQueryString() throws IOException {
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
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
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
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
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        when(servletRequest.getMethod()).thenReturn("POST");
        when(servletRequest.getQueryString())
                .thenReturn("query1=QUERY_ONE&query2=query_two&query2=QUERY_TWO");
        when(servletRequest.getRequestURI()).thenReturn("/swagger-request-validator");
        when(servletRequest.getContentLength()).thenReturn(-1);
        final BufferedReader reader = new BufferedReader(new StringReader("Body"));
        when(servletRequest.getReader()).thenReturn(reader);
        when(servletRequest.getHeaderNames())
                .thenReturn(Collections.enumeration(Arrays.asList("header1", "header2")));
        when(servletRequest.getHeaders("header1"))
                .thenReturn(Collections.enumeration(Arrays.asList("HEADER_ONE")));
        when(servletRequest.getHeaders("header2"))
                .thenReturn(Collections.enumeration(Arrays.asList("header_two", "HEADER_TWO")));

        final Request result = classUnderTest.buildRequest(servletRequest);

        assertThat(result.getPath(), equalTo("/swagger-request-validator"));
        assertThat(result.getMethod(), equalTo(Request.Method.POST));
        assertThat(result.getBody().get(), equalTo("Body"));
        assertThat(result.getHeaders().size(), equalTo(2));
        assertThat(result.getHeaderValues("header1"),
                equalTo(Arrays.asList("HEADER_ONE")));
        assertThat(result.getHeaderValues("header2"),
                equalTo(Arrays.asList("header_two", "HEADER_TWO")));
        assertThat(result.getQueryParameters().size(), equalTo(2));
        assertThat(result.getQueryParameterValues("query1"),
                equalTo(Arrays.asList("QUERY_ONE")));
        assertThat(result.getQueryParameterValues("query2"),
                equalTo(Arrays.asList("query_two", "QUERY_TWO")));
    }

    @Test
    public void validateRequest_returnsTheValidationReport() {
        final Request request = Mockito.mock(Request.class);
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);
        when(requestValidator.validateRequest(request)).thenReturn(validationReport);

        final ValidationReport result = classUnderTest.validateRequest(request);

        Mockito.verify(requestValidator, times(1)).validateRequest(request);
        assertThat(result, is(validationReport));
    }

    @Test
    public void isDefinedSwaggerRequest_theValidationReportSuggestsThatTheRequestIsNotDefined() {
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);
        final ValidationReport.Message message1 = Mockito.mock(ValidationReport.Message.class);
        final ValidationReport.Message message2 = Mockito.mock(ValidationReport.Message.class);
        when(validationReport.getMessages()).thenReturn(Arrays.asList(message1, message2));
        when(message1.getKey()).thenReturn("other.validation.error");
        when(message2.getKey()).thenReturn("validation.request.path.missing");

        final boolean result = classUnderTest.isDefinedSwaggerRequest(validationReport);

        assertThat(result, is(false));
    }

    @Test
    public void isDefinedSwaggerRequest_theValidationReportSuggestsThatTheRequestIsDefined() {
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);
        final ValidationReport.Message message1 = Mockito.mock(ValidationReport.Message.class);
        final ValidationReport.Message message2 = Mockito.mock(ValidationReport.Message.class);
        when(validationReport.getMessages()).thenReturn(Arrays.asList(message1, message2));
        when(message1.getKey()).thenReturn("other.validation.error");
        when(message2.getKey()).thenReturn("another.validation.error");

        final boolean result = classUnderTest.isDefinedSwaggerRequest(validationReport);

        assertThat(result, is(true));
    }
}
