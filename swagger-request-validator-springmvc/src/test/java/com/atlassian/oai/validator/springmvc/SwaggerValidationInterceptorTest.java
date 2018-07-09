package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class SwaggerValidationInterceptorTest {

    private SwaggerValidationInterceptor classUnderTest;

    private SwaggerRequestValidationService swaggerRequestValidationService;

    @Before
    public void setUp() {
        swaggerRequestValidationService = Mockito.mock(SwaggerRequestValidationService.class);
        classUnderTest = new SwaggerValidationInterceptor(swaggerRequestValidationService);
    }

    @Test
    public void constructor_withEncodedResource() throws IOException {
        final EncodedResource encodedResource = Mockito.mock(EncodedResource.class);
        when(encodedResource.getReader())
                .thenReturn(new InputStreamReader(getClass().getResourceAsStream("/api-spring-test.json")));

        final SwaggerValidationInterceptor interceptor = new SwaggerValidationInterceptor(encodedResource);
        assertThat(interceptor, notNullValue());
    }

    @Test
    public void constructor_withSwaggerRequestResponseValidator() {
        // given:
        final SwaggerRequestResponseValidator swaggerRequestResponseValidator =
                Mockito.mock(SwaggerRequestResponseValidator.class);

        // when:
        final SwaggerValidationInterceptor interceptor = new SwaggerValidationInterceptor(swaggerRequestResponseValidator);

        // then:
        assertThat(interceptor, notNullValue());
    }

    @Test
    public void preHandle_noRequestValidationIfNoWrappedServletRequest() throws Exception {
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);

        final boolean result = classUnderTest.preHandle(servletRequest, null, null);
        assertThat(result, equalTo(true));
    }

    @Test
    public void preHandle_theRequestIsValid() throws Exception {
        final HttpServletRequest servletRequest = Mockito.mock(ResettableRequestServletWrapper.class);
        final Request request = Mockito.mock(Request.class);
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);

        when(servletRequest.getMethod()).thenReturn("METHOD");
        when(servletRequest.getRequestURI()).thenReturn("/request/uri");

        when(swaggerRequestValidationService.buildRequest(servletRequest)).thenReturn(request);
        when(swaggerRequestValidationService.validateRequest(request)).thenReturn(validationReport);
        when(validationReport.hasErrors()).thenReturn(false);

        final boolean result = classUnderTest.preHandle(servletRequest, null, null);

        Mockito.verify(validationReport, times(1)).hasErrors();
        assertThat(result, equalTo(true));
    }

    @Test(expected = InvalidRequestException.class)
    public void preHandle_theRequestIsInvalid() throws Exception {
        final HttpServletRequest servletRequest = Mockito.mock(ResettableRequestServletWrapper.class);
        final Request request = Mockito.mock(Request.class);
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);

        when(servletRequest.getMethod()).thenReturn("METHOD");
        when(servletRequest.getRequestURI()).thenReturn("/request/uri");

        Mockito.when(swaggerRequestValidationService.buildRequest(servletRequest)).thenReturn(request);
        Mockito.when(swaggerRequestValidationService.validateRequest(request)).thenReturn(validationReport);
        Mockito.when(validationReport.hasErrors()).thenReturn(true);
        Mockito.when(validationReport.getMessages()).thenReturn(Collections.emptyList());

        final boolean result = classUnderTest.preHandle(servletRequest, null, null);

        Mockito.verify(validationReport, times(1)).getMessages();
        assertThat(result, equalTo(true));
    }

    @Test
    public void postHandle_noResponseValidationIfNoWrappedServletResponse() throws Exception {
        // given:
        final HttpServletResponse servletResponse = Mockito.mock(HttpServletResponse.class);

        // expect:
        classUnderTest.postHandle(null, servletResponse, null, null);
    }

    @Test
    public void postHandle_theResponseIsValid() throws Exception {
        // given:
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        final ContentCachingResponseWrapper servletResponse = Mockito.mock(ContentCachingResponseWrapper.class);
        final Response response = Mockito.mock(Response.class);
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);

        // and:
        Mockito.when(servletRequest.getMethod()).thenReturn("METHOD");
        Mockito.when(servletRequest.getRequestURI()).thenReturn("/request/uri");

        Mockito.when(swaggerRequestValidationService.buildResponse(servletResponse)).thenReturn(response);
        Mockito.when(swaggerRequestValidationService.validateResponse(servletRequest, response)).thenReturn(validationReport);
        Mockito.when(validationReport.hasErrors()).thenReturn(false);

        // when:
        classUnderTest.postHandle(servletRequest, servletResponse, null, null);

        // then:
        Mockito.verify(validationReport, times(1)).hasErrors();
    }

    @Test(expected = InvalidResponseException.class)
    public void postHandle_theResponseIsInvalid() throws Exception {
        // setup:
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);
        final ContentCachingResponseWrapper servletResponse = Mockito.mock(ContentCachingResponseWrapper.class);
        final Response response = Mockito.mock(Response.class);
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);

        // and:
        Mockito.when(servletRequest.getMethod()).thenReturn("METHOD");
        Mockito.when(servletRequest.getRequestURI()).thenReturn("/request/uri");

        Mockito.when(swaggerRequestValidationService.buildResponse(servletResponse)).thenReturn(response);
        Mockito.when(swaggerRequestValidationService.validateResponse(servletRequest, response)).thenReturn(validationReport);
        Mockito.when(validationReport.hasErrors()).thenReturn(true);
        Mockito.when(validationReport.getMessages()).thenReturn(Collections.emptyList());

        // when:
        classUnderTest.postHandle(servletRequest, servletResponse, null, null);

        // then:
        Mockito.verify(validationReport, times(1)).getMessages();
        Mockito.verify(servletResponse, times(1)).reset();
    }
}
