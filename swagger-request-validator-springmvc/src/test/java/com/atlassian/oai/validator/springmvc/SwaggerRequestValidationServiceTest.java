package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.support.EncodedResource;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.Mockito.times;

public class SwaggerRequestValidationServiceTest {

    private SwaggerRequestValidationService classUnderTest;

    private SwaggerRequestResponseValidator requestValidator;

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
    public void constructor_withEncodedResource() throws IOException {
        final EncodedResource encodedResource = Mockito.mock(EncodedResource.class);
        Mockito.when(encodedResource.getReader()).thenReturn(new StringReader("{}"));

        final SwaggerRequestValidationService service = new SwaggerRequestValidationService(encodedResource);
        Assert.assertThat(service, notNullValue());
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
    public void isDefinedSwaggerRequest_theValidationReportSuggestsThatTheRequestIsNotDefined() {
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);
        final ValidationReport.Message message1 = Mockito.mock(ValidationReport.Message.class);
        final ValidationReport.Message message2 = Mockito.mock(ValidationReport.Message.class);
        Mockito.when(validationReport.getMessages()).thenReturn(Arrays.asList(message1, message2));
        Mockito.when(message1.getKey()).thenReturn("other.validation.error");
        Mockito.when(message2.getKey()).thenReturn("validation.request.path.missing");

        final boolean result = classUnderTest.isDefinedSwaggerRequest(validationReport);

        Assert.assertThat(result, is(false));
    }

    @Test
    public void isDefinedSwaggerRequest_theValidationReportSuggestsThatTheRequestIsDefined() {
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);
        final ValidationReport.Message message1 = Mockito.mock(ValidationReport.Message.class);
        final ValidationReport.Message message2 = Mockito.mock(ValidationReport.Message.class);
        Mockito.when(validationReport.getMessages()).thenReturn(Arrays.asList(message1, message2));
        Mockito.when(message1.getKey()).thenReturn("other.validation.error");
        Mockito.when(message2.getKey()).thenReturn("another.validation.error");

        final boolean result = classUnderTest.isDefinedSwaggerRequest(validationReport);

        Assert.assertThat(result, is(true));
    }
}
