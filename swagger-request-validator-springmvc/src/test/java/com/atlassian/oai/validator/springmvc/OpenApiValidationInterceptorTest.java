package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Body;
import com.atlassian.oai.validator.model.ByteArrayBody;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.atlassian.oai.validator.springmvc.OpenApiValidationFilter.ATTRIBUTE_REQUEST_VALIDATION;
import static com.atlassian.oai.validator.springmvc.OpenApiValidationFilter.ATTRIBUTE_RESPONSE_VALIDATION;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OpenApiValidationInterceptorTest {

    private OpenApiValidationInterceptor classUnderTest;

    private OpenApiValidationService openApiValidationService;
    private ValidationReportHandler validationReportHandler;

    @BeforeEach
    public void setUp() {
        openApiValidationService = mock(OpenApiValidationService.class);
        validationReportHandler = mock(ValidationReportHandler.class);
        classUnderTest = new OpenApiValidationInterceptor(openApiValidationService, validationReportHandler);
    }

    @Test
    public void constructor_withEncodedResource() throws IOException {
        final EncodedResource encodedResource = mock(EncodedResource.class);
        when(encodedResource.getReader())
                .thenReturn(new InputStreamReader(getClass().getResourceAsStream("/api-spring-test.json")));

        final OpenApiValidationInterceptor interceptor = new OpenApiValidationInterceptor(encodedResource);
        assertThat(interceptor, notNullValue());
    }

    @Test
    public void constructor_withOpenApiInteractionValidator() {
        // given:
        final OpenApiInteractionValidator openApiInteractionValidator = mock(OpenApiInteractionValidator.class);

        // when:
        final OpenApiValidationInterceptor interceptor = new OpenApiValidationInterceptor(openApiInteractionValidator);

        // then:
        assertThat(interceptor, notNullValue());
    }

    @Test
    public void constructor_withOpenApiInteractionValidatorAndValidationReportHandler() {
        // given:
        final OpenApiInteractionValidator openApiInteractionValidator = mock(OpenApiInteractionValidator.class);
        final ValidationReportHandler validationReportHandler = mock(ValidationReportHandler.class);

        // when:
        final OpenApiValidationInterceptor interceptor = new OpenApiValidationInterceptor(openApiInteractionValidator, validationReportHandler);

        // then:
        assertThat(interceptor, notNullValue());
    }

    @Test
    public void preHandle_noRequestValidationIfNoServletRequestWrapper() throws Exception {
        // given:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_REQUEST_VALIDATION)).thenReturn(true);

        // when:
        final boolean result = classUnderTest.preHandle(servletRequest, null, null);

        // then:
        assertThat(result, equalTo(true));
        verify(servletRequest).getAttribute(ATTRIBUTE_REQUEST_VALIDATION);
        verify(openApiValidationService, never()).validateRequest(any());
    }

    @Test
    public void preHandle_noRequestValidationIfValidationAttributeMissing() throws Exception {
        // given:
        final ResettableRequestServletWrapper servletRequest = mock(ResettableRequestServletWrapper.class);

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_REQUEST_VALIDATION)).thenReturn(null);

        // when:
        final boolean result = classUnderTest.preHandle(servletRequest, null, null);

        // then:
        assertThat(result, equalTo(true));
        verify(servletRequest).getAttribute(ATTRIBUTE_REQUEST_VALIDATION);
        verify(openApiValidationService, never()).validateRequest(any(Request.class));
    }

    @Test
    public void preHandle_noRequestValidationIfValidationAttributeInvalid() throws Exception {
        // given:
        final ContentCachingRequestWrapper servletRequest = mock(ContentCachingRequestWrapper.class);

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_REQUEST_VALIDATION)).thenReturn("");

        // when:
        final boolean result = classUnderTest.preHandle(servletRequest, null, null);

        // then:
        assertThat(result, equalTo(true));
        verify(servletRequest).getAttribute(ATTRIBUTE_REQUEST_VALIDATION);
        verify(openApiValidationService, never()).validateRequest(any(Request.class));
    }

    @Test
    public void preHandle_noRequestValidationIfValidationAttributeFalse() throws Exception {
        // given:
        final ContentCachingRequestWrapper servletRequest = mock(ContentCachingRequestWrapper.class);

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_REQUEST_VALIDATION)).thenReturn(Boolean.FALSE);

        // when:
        final boolean result = classUnderTest.preHandle(servletRequest, null, null);

        // then:
        assertThat(result, equalTo(true));
        verify(servletRequest).getAttribute(ATTRIBUTE_REQUEST_VALIDATION);
        verify(openApiValidationService, never()).validateRequest(any(Request.class));
    }

    @Test
    public void preHandle_noRequestValidationButPreparationForResponseValidationNecessary() throws Exception {
        // given:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final OpenApiValidationContentCachingResponseWrapper servletResponse = mock(OpenApiValidationContentCachingResponseWrapper.class);
        final Map<String, List<String>> headers = mock(Map.class);

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_RESPONSE_VALIDATION)).thenReturn(Boolean.TRUE);
        when(openApiValidationService.resolveHeadersOnResponse(servletResponse)).thenReturn(headers);

        // when:
        final boolean result = classUnderTest.preHandle(servletRequest, servletResponse, null);

        // then:
        assertThat(result, equalTo(true));
        verify(servletRequest).setAttribute("com.atlassian.oai.validator.springmvc.alreadySetHeaders", headers);
    }

    @Test
    public void preHandle_theRequestIsValid_ContentCachingRequestWrapper() throws Exception {
        // given:
        final ContentCachingRequestWrapper servletRequest = mock(ContentCachingRequestWrapper.class);
        final Request request = mock(Request.class);
        final ValidationReport validationReport = mock(ValidationReport.class);

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_REQUEST_VALIDATION)).thenReturn(Boolean.TRUE);
        when(servletRequest.getMethod()).thenReturn("METHOD");
        when(servletRequest.getRequestURI()).thenReturn("/request/uri");

        final ArgumentCaptor<Supplier<Body>> bodySupplierArgument = ArgumentCaptor.forClass(Supplier.class);
        when(openApiValidationService.buildRequest(eq(servletRequest), bodySupplierArgument.capture())).thenReturn(request);
        when(openApiValidationService.validateRequest(request)).thenReturn(validationReport);

        // when:
        final boolean result = classUnderTest.preHandle(servletRequest, null, null);

        // then:
        assertThat(result, equalTo(true));
        verify(validationReportHandler).handleRequestReport("METHOD#/request/uri", validationReport);
        verify(servletRequest, never()).getContentAsByteArray(); // the body has not been read, yet

        // when: 'the body is not read until the supplier is called'
        final Supplier<Body> bodySupplier = bodySupplierArgument.getValue();
        final Body body = bodySupplier.get();

        // then:
        assertThat(body, instanceOf(ByteArrayBody.class));
        verify(servletRequest).getContentAsByteArray();
    }

    @Test
    public void preHandle_theRequestIsValid_ResettableRequestServletWrapper() throws Exception {
        // given:
        final ResettableRequestServletWrapper servletRequest = mock(ResettableRequestServletWrapper.class);
        final ResettableRequestServletWrapper.CachingServletInputStream cachingServletInputStream =
                mock(ResettableRequestServletWrapper.CachingServletInputStream.class);
        final Request request = mock(Request.class);
        final ValidationReport validationReport = mock(ValidationReport.class);

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_REQUEST_VALIDATION)).thenReturn(Boolean.TRUE);
        when(servletRequest.getMethod()).thenReturn("METHOD");
        when(servletRequest.getRequestURI()).thenReturn("/request/uri");
        when(servletRequest.getInputStream()).thenReturn(cachingServletInputStream);

        final ArgumentCaptor<Supplier<Body>> bodySupplierArgument = ArgumentCaptor.forClass(Supplier.class);
        when(openApiValidationService.buildRequest(eq(servletRequest), bodySupplierArgument.capture())).thenReturn(request);
        when(openApiValidationService.validateRequest(request)).thenReturn(validationReport);

        // when:
        final boolean result = classUnderTest.preHandle(servletRequest, null, null);

        // then:
        assertThat(result, equalTo(true));

        // and: 'the InputStream on a ResettableRequestServletWrapper is reset after validation, so it can be read again'
        verify(servletRequest).resetInputStream();

        // when: 'the body is not read until the supplier is called'
        final Supplier<Body> bodySupplier = bodySupplierArgument.getValue();
        final Body body = bodySupplier.get();

        // then:
        assertThat(body, instanceOf(ResettableInputStreamBody.class));
    }

    @Test
    public void preHandle_theRequestIsInvalid() throws Exception {
        // given:
        final HttpServletRequest servletRequest = mock(ResettableRequestServletWrapper.class);
        final ResettableRequestServletWrapper.CachingServletInputStream cachingServletInputStream =
                mock(ResettableRequestServletWrapper.CachingServletInputStream.class);
        final Request request = mock(Request.class);
        final ValidationReport validationReport = mock(ValidationReport.class);

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_REQUEST_VALIDATION)).thenReturn(Boolean.TRUE);
        when(servletRequest.getMethod()).thenReturn("METHOD");
        when(servletRequest.getRequestURI()).thenReturn("/request/uri");
        when(servletRequest.getInputStream()).thenReturn(cachingServletInputStream);

        when(openApiValidationService.buildRequest(eq(servletRequest), any(Supplier.class))).thenReturn(request);
        when(openApiValidationService.validateRequest(request)).thenReturn(validationReport);
        doThrow(new InvalidRequestException(validationReport)).when(validationReportHandler)
                .handleRequestReport("METHOD#/request/uri", validationReport);

        // expect:
        assertThrows(InvalidRequestException.class,
                () -> classUnderTest.preHandle(servletRequest, null, null));
    }

    @Test
    public void postHandle_noResponseValidationIfNoServletResponseWrapper() throws Exception {
        // given:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_RESPONSE_VALIDATION)).thenReturn(null);

        // when:
        classUnderTest.postHandle(servletRequest, servletResponse, null, null);

        // then:
        verify(openApiValidationService, never()).validateResponse(any(), any());
    }

    @Test
    public void postHandle_noResponseValidationIfValidationAttributeMissing() throws Exception {
        // given:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ContentCachingResponseWrapper servletResponse = mockResponseWrapper();

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_RESPONSE_VALIDATION)).thenReturn(null);

        // when:
        classUnderTest.postHandle(servletRequest, servletResponse, null, null);

        // then:
        verify(servletRequest).getAttribute(ATTRIBUTE_RESPONSE_VALIDATION);
        verify(openApiValidationService, never()).validateResponse(any(), any());
    }

    @Test
    public void postHandle_noResponseValidationIfValidationAttributeInvalid() throws Exception {
        // given:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ContentCachingResponseWrapper servletResponse = mockResponseWrapper();

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_RESPONSE_VALIDATION)).thenReturn("");

        // when:
        classUnderTest.postHandle(servletRequest, servletResponse, null, null);

        // then:
        verify(servletRequest).getAttribute(ATTRIBUTE_RESPONSE_VALIDATION);
        verify(openApiValidationService, never()).validateResponse(any(), any());
    }

    @Test
    public void postHandle_noResponseValidationIfValidationAttributeFalse() throws Exception {
        // given:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ContentCachingResponseWrapper servletResponse = mockResponseWrapper();

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_RESPONSE_VALIDATION)).thenReturn(Boolean.FALSE);

        // when:
        classUnderTest.postHandle(servletRequest, servletResponse, null, null);

        // then:
        verify(servletRequest).getAttribute(ATTRIBUTE_RESPONSE_VALIDATION);
        verify(openApiValidationService, never()).validateResponse(any(), any());
    }

    @Test
    public void postHandle_theResponseIsValid() throws Exception {
        // given:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ContentCachingResponseWrapper servletResponse = mockResponseWrapper();
        final Response response = mock(Response.class);
        final ValidationReport validationReport = mock(ValidationReport.class);

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_RESPONSE_VALIDATION)).thenReturn(Boolean.TRUE);
        when(servletRequest.getMethod()).thenReturn("METHOD");
        when(servletRequest.getRequestURI()).thenReturn("/request/uri");

        when(openApiValidationService.buildResponse(servletResponse)).thenReturn(response);
        when(openApiValidationService.validateResponse(servletRequest, response)).thenReturn(validationReport);

        // when:
        classUnderTest.postHandle(servletRequest, servletResponse, null, null);

        // then:
        verify(validationReportHandler).handleResponseReport("METHOD#/request/uri", validationReport);
    }

    @Test
    public void postHandle_theResponseIsValidWrapperWrapped() throws Exception {
        // given:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final HttpServletResponse servletResponse = new HttpServletResponseWrapper(mockResponseWrapper());
        final Response response = mock(Response.class);
        final ValidationReport validationReport = mock(ValidationReport.class);

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_RESPONSE_VALIDATION)).thenReturn(Boolean.TRUE);
        when(servletRequest.getMethod()).thenReturn("METHOD");
        when(servletRequest.getRequestURI()).thenReturn("/request/uri");

        when(openApiValidationService.buildResponse(any())).thenReturn(response);
        when(openApiValidationService.validateResponse(servletRequest, response)).thenReturn(validationReport);

        // when:
        classUnderTest.postHandle(servletRequest, servletResponse, null, null);

        // then:
        verify(validationReportHandler).handleResponseReport("METHOD#/request/uri", validationReport);
    }

    @Test
    public void postHandle_theResponseIsInvalid() throws Exception {
        // setup:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final ContentCachingResponseWrapper servletResponse = mockResponseWrapper();
        final Response response = mock(Response.class);
        final ValidationReport validationReport = mock(ValidationReport.class);
        final Map<String, List<String>> headers = mock(Map.class);
        final InvalidResponseException exception = new InvalidResponseException(validationReport);

        // and:
        when(servletRequest.getAttribute(ATTRIBUTE_RESPONSE_VALIDATION)).thenReturn(Boolean.TRUE);
        when(servletRequest.getMethod()).thenReturn("METHOD");
        when(servletRequest.getRequestURI()).thenReturn("/request/uri");
        when(servletRequest.getAttribute("com.atlassian.oai.validator.springmvc.alreadySetHeaders")).thenReturn(headers);
        when(openApiValidationService.buildResponse(servletResponse)).thenReturn(response);
        when(openApiValidationService.validateResponse(servletRequest, response)).thenReturn(validationReport);
        doThrow(exception).when(validationReportHandler)
                .handleResponseReport("METHOD#/request/uri", validationReport);

        // when:
        assertThatThrownBy(() -> classUnderTest.postHandle(servletRequest, servletResponse, null, null))
                .isEqualTo(exception);

        // then:
        verify(servletResponse).reset();
        verify(openApiValidationService).addHeadersToResponse(servletResponse, headers);
    }

    private OpenApiValidationContentCachingResponseWrapper mockResponseWrapper() {
        return mock(OpenApiValidationContentCachingResponseWrapper.class);
    }
}
