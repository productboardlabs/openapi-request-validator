package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.report.ValidationReport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.core.io.support.EncodedResource;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.mockito.Mockito.times;

public class SwaggerValidationInterceptorTest {

    private SwaggerValidationInterceptor classUnderTest;

    private SwaggerRequestValidationService swaggerRequestValidationService;

    @Before
    public void setUp() {
        this.swaggerRequestValidationService = Mockito.mock(SwaggerRequestValidationService.class);
        this.classUnderTest = new SwaggerValidationInterceptor(swaggerRequestValidationService);
    }

    @Test
    public void constructor_withEncodedResource() throws IOException {
        final EncodedResource encodedResource = Mockito.mock(EncodedResource.class);
        Mockito.when(encodedResource.getReader()).thenReturn(new StringReader("{}"));

        final SwaggerValidationInterceptor interceptor = new SwaggerValidationInterceptor(encodedResource);
        Assert.assertThat(interceptor, notNullValue());
    }

    @Test
    public void preHandle_noValidationIfNoWrappedServletRequest() throws Exception {
        final HttpServletRequest servletRequest = Mockito.mock(HttpServletRequest.class);

        final boolean result = classUnderTest.preHandle(servletRequest, null, null);
        Assert.assertThat(result, equalTo(true));
    }

    @Test
    public void preHandle_theRequestIsValid() throws Exception {
        final HttpServletRequest servletRequest = Mockito.mock(ResettableRequestServletWrapper.class);
        final Request request = Mockito.mock(Request.class);
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);

        Mockito.when(servletRequest.getMethod()).thenReturn("METHOD");
        Mockito.when(servletRequest.getRequestURI()).thenReturn("/request/uri");

        Mockito.when(swaggerRequestValidationService.buildRequest(servletRequest)).thenReturn(request);
        Mockito.when(swaggerRequestValidationService.validateRequest(request)).thenReturn(validationReport);
        Mockito.when(validationReport.hasErrors()).thenReturn(false);

        final boolean result = classUnderTest.preHandle(servletRequest, null, null);

        Mockito.verify(validationReport, times(1)).hasErrors();
        Assert.assertThat(result, equalTo(true));
    }

    @Test
    public void preHandle_theRequestIsNotPartOfTheSwaggerDefinition() throws Exception {
        final HttpServletRequest servletRequest = Mockito.mock(ResettableRequestServletWrapper.class);
        final Request request = Mockito.mock(Request.class);
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);

        Mockito.when(servletRequest.getMethod()).thenReturn("METHOD");
        Mockito.when(servletRequest.getRequestURI()).thenReturn("/request/uri");

        Mockito.when(swaggerRequestValidationService.buildRequest(servletRequest)).thenReturn(request);
        Mockito.when(swaggerRequestValidationService.validateRequest(request)).thenReturn(validationReport);
        Mockito.when(validationReport.hasErrors()).thenReturn(true);
        Mockito.when(swaggerRequestValidationService.isDefinedSwaggerRequest(validationReport)).thenReturn(false);

        final boolean result = classUnderTest.preHandle(servletRequest, null, null);

        Mockito.verify(swaggerRequestValidationService, times(1)).isDefinedSwaggerRequest(validationReport);
        Assert.assertThat(result, equalTo(true));
    }

    @Test(expected = InvalidRequestException.class)
    public void preHandle_theRequestIsInvalid() throws Exception {
        final HttpServletRequest servletRequest = Mockito.mock(ResettableRequestServletWrapper.class);
        final Request request = Mockito.mock(Request.class);
        final ValidationReport validationReport = Mockito.mock(ValidationReport.class);

        Mockito.when(servletRequest.getMethod()).thenReturn("METHOD");
        Mockito.when(servletRequest.getRequestURI()).thenReturn("/request/uri");

        Mockito.when(swaggerRequestValidationService.buildRequest(servletRequest)).thenReturn(request);
        Mockito.when(swaggerRequestValidationService.validateRequest(request)).thenReturn(validationReport);
        Mockito.when(validationReport.hasErrors()).thenReturn(true);
        Mockito.when(swaggerRequestValidationService.isDefinedSwaggerRequest(validationReport)).thenReturn(true);
        Mockito.when(validationReport.getMessages()).thenReturn(Collections.emptyList());

        final boolean result = classUnderTest.preHandle(servletRequest, null, null);

        Mockito.verify(validationReport, times(1)).getMessages();
        Assert.assertThat(result, equalTo(true));
    }
}
