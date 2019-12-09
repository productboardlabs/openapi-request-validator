package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.Level;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class OpenApiValidationInterceptorTest {

    private OpenApiValidationInterceptor classUnderTest;

    private OpenApiValidationService openApiValidationService;

    @Before
    public void setUp() {
        openApiValidationService = Mockito.mock(OpenApiValidationService.class);
        classUnderTest = new OpenApiValidationInterceptor(openApiValidationService);
    }

    @Test
    public void constructor_withEncodedResource() throws IOException {
        final EncodedResource encodedResource = Mockito.mock(EncodedResource.class);
        when(encodedResource.getReader())
                .thenReturn(new InputStreamReader(getClass().getResourceAsStream("/api-spring-test.json")));

        final OpenApiValidationInterceptor interceptor = new OpenApiValidationInterceptor(encodedResource);
        assertThat(interceptor, notNullValue());
    }

    @Test
    public void constructor_withSwaggerRequestResponseValidator() {
        // given:
        final OpenApiInteractionValidator openApiInteractionValidator =
                Mockito.mock(OpenApiInteractionValidator.class);

        // when:
        final OpenApiValidationInterceptor interceptor = new OpenApiValidationInterceptor(openApiInteractionValidator);

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

        when(openApiValidationService.buildRequest(servletRequest)).thenReturn(request);
        when(openApiValidationService.validateRequest(request)).thenReturn(validationReport);
        when(validationReport.sortedValidationLevels()).thenReturn(emptySet());

        final boolean result = classUnderTest.preHandle(servletRequest, null, null);

        Mockito.verify(validationReport, times(1)).sortedValidationLevels();
        assertThat(result, equalTo(true));
    }

    @Test(expected = InvalidRequestException.class)
    public void preHandle_theRequestIsInvalid() throws Exception {
        final HttpServletRequest servletRequest = Mockito.mock(ResettableRequestServletWrapper.class);
        final Request request = Mockito.mock(Request.class);
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);

        when(servletRequest.getMethod()).thenReturn("METHOD");
        when(servletRequest.getRequestURI()).thenReturn("/request/uri");

        Mockito.when(openApiValidationService.buildRequest(servletRequest)).thenReturn(request);
        Mockito.when(openApiValidationService.validateRequest(request)).thenReturn(validationReport);
        Mockito.when(validationReport.hasErrors()).thenReturn(true);
        Mockito.when(validationReport.sortedValidationLevels()).thenReturn(singleton(Level.ERROR));

        final boolean result = classUnderTest.preHandle(servletRequest, null, null);

        Mockito.verify(validationReport, times(1)).sortedValidationLevels();
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

        Mockito.when(openApiValidationService.buildResponse(servletResponse)).thenReturn(response);
        Mockito.when(openApiValidationService.validateResponse(servletRequest, response)).thenReturn(validationReport);
        Mockito.when(validationReport.sortedValidationLevels()).thenReturn(emptySet());

        // when:
        classUnderTest.postHandle(servletRequest, servletResponse, null, null);

        // then:
        Mockito.verify(validationReport, times(1)).sortedValidationLevels();
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

        Mockito.when(openApiValidationService.buildResponse(servletResponse)).thenReturn(response);
        Mockito.when(openApiValidationService.validateResponse(servletRequest, response)).thenReturn(validationReport);
        Mockito.when(validationReport.hasErrors()).thenReturn(true);
        Mockito.when(validationReport.sortedValidationLevels()).thenReturn(singleton(Level.ERROR));

        // when:
        classUnderTest.postHandle(servletRequest, servletResponse, null, null);

        // then:
        Mockito.verify(validationReport, times(1)).sortedValidationLevels();
        Mockito.verify(servletResponse, times(1)).reset();
    }
}
