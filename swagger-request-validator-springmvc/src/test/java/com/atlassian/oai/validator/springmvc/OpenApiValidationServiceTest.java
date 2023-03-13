package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.example.simple.RestServiceApplication;
import com.atlassian.oai.validator.model.Body;
import com.atlassian.oai.validator.model.InputStreamBody;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.collection.IsMapWithSize.aMapWithSize;
import static org.hamcrest.collection.IsMapWithSize.anEmptyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.contextPath=/v1", "server.error.include-message=always"})
public class OpenApiValidationServiceTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private OpenApiValidationService classUnderTest;

    private OpenApiInteractionValidator requestValidator;
    private UrlPathHelper urlPathHelper;

    private static Map<String, Collection<String>> getHeadersFromResponse(final Response response) {
        final Field headersField = ReflectionUtils.findField(SimpleResponse.class, "headers");
        ReflectionUtils.makeAccessible(headersField);
        return (Map<String, Collection<String>>) ReflectionUtils.getField(headersField, response);
    }

    private static Enumeration<String> asEnumeration(final String... values) {
        return Iterators.asEnumeration(Arrays.asList(values).iterator());
    }

    @BeforeEach
    public void setUp() {
        requestValidator = mock(OpenApiInteractionValidator.class);
        urlPathHelper = mock(UrlPathHelper.class);
        classUnderTest = new OpenApiValidationService(requestValidator, urlPathHelper);
    }

    @Test
    public void constructor_failsWithoutRequiredValidator() {
        assertThrows(NullPointerException.class,
                () -> new OpenApiValidationService((OpenApiInteractionValidator) null, urlPathHelper));
    }

    @Test
    public void constructor_withEncodedResource() throws IOException {
        final EncodedResource encodedResource = mock(EncodedResource.class);
        when(encodedResource.getReader())
                .thenReturn(new InputStreamReader(getClass().getResourceAsStream("/api-spring-test.json")));

        final OpenApiValidationService service = new OpenApiValidationService(encodedResource, urlPathHelper);
        assertThat(service, notNullValue());
    }

    @Test
    public void aMissingPathIsNotTreatedAsError() throws IOException {
        // given:
        final EncodedResource encodedResource = mock(EncodedResource.class);
        when(encodedResource.getReader())
                .thenReturn(new InputStreamReader(getClass().getResourceAsStream("/api-spring-test.json")));
        final OpenApiValidationService service = new OpenApiValidationService(encodedResource, urlPathHelper);
        assertThat(service, notNullValue());

        // and:
        final Request request = new SimpleRequest.Builder(Request.Method.GET, "/unknownPath").build();

        // when:
        final ValidationReport validationReport = service.validateRequest(request);

        // then:
        assertThat(validationReport.hasErrors(), is(false));
    }

    @Test
    public void buildRequest_failsWithoutRequiredRequest() {
        final Supplier<Body> bodySupplier = mock(Supplier.class);
        assertThrows(NullPointerException.class,
                () -> classUnderTest.buildRequest(null, bodySupplier));
    }

    @Test
    public void buildRequest_failsWithoutRequiredBodySupplier() {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        assertThrows(NullPointerException.class,
                () -> classUnderTest.buildRequest(servletRequest, null));
    }

    @Test
    public void buildRequest_withoutHeaderAndQueryString() {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getMethod()).thenReturn("GET");
        when(servletRequest.getQueryString()).thenReturn("");
        when(servletRequest.getParameterNames()).thenReturn(asEnumeration("not-a-query-parameter"));
        when(servletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
        when(urlPathHelper.getPathWithinApplication(servletRequest)).thenReturn("/swagger-request-validator");
        final Body body = mock(Body.class);
        final Supplier<Body> bodySupplier = mock(Supplier.class);
        when(bodySupplier.get()).thenReturn(body);

        final Request result = classUnderTest.buildRequest(servletRequest, bodySupplier);

        assertThat(result.getPath(), equalTo("/swagger-request-validator"));
        assertThat(result.getMethod(), equalTo(Request.Method.GET));
        assertThat(result.getRequestBody().get(), is(body));
        assertThat(result.getHeaders().size(), equalTo(0));
        assertThat(result.getQueryParameters().size(), equalTo(0));
    }

    @Test
    public void buildRequest_withHeaderAndQueryString() {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        when(servletRequest.getMethod()).thenReturn("POST");
        when(servletRequest.getQueryString())
                .thenReturn("query1=QUERY_ONE&&query2=query_two&query2=QUERY_TWO&");
        when(urlPathHelper.getPathWithinApplication(servletRequest)).thenReturn("/swagger-request-validator");
        when(servletRequest.getParameterNames())
                .thenReturn(asEnumeration("query1", "query2", "query3"));
        when(servletRequest.getParameterValues("query1"))
                .thenReturn(new String[]{"QUERY_1"});
        when(servletRequest.getParameterValues("query2"))
                .thenReturn(new String[]{"query_2", "QUERY_2"});
        when(servletRequest.getHeaderNames())
                .thenReturn(Collections.enumeration(asList("header1", "header2")));
        when(servletRequest.getHeaders("header1"))
                .thenReturn(Collections.enumeration(asList("HEADER_ONE")));
        when(servletRequest.getHeaders("header2"))
                .thenReturn(Collections.enumeration(asList("header_two", "HEADER_TWO")));
        final Body body = mock(Body.class);
        final Supplier<Body> bodySupplier = mock(Supplier.class);
        when(bodySupplier.get()).thenReturn(body);

        final Request result = classUnderTest.buildRequest(servletRequest, bodySupplier);

        assertThat(result.getPath(), equalTo("/swagger-request-validator"));
        assertThat(result.getMethod(), equalTo(Request.Method.POST));
        assertThat(result.getRequestBody().get(), is(body));
        assertThat(result.getHeaders().size(), equalTo(2));
        assertThat(result.getHeaderValues("header1"),
                equalTo(asList("HEADER_ONE")));
        assertThat(result.getHeaderValues("header2"),
                equalTo(asList("header_two", "HEADER_TWO")));
        assertThat(result.getQueryParameters().size(), equalTo(2));
        assertThat(result.getQueryParameterValues("query1"),
                equalTo(asList("QUERY_1")));
        assertThat(result.getQueryParameterValues("query2"),
                equalTo(asList("query_2", "QUERY_2")));
    }

    @Test
    public void buildRequest_queryParametersAreResolvedBeforeTheBody() {
        // given:
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        final Supplier<Body> bodySupplier = mock(Supplier.class);
        final Body body = mock(Body.class);
        final InOrder ensureOrder = inOrder(servletRequest, bodySupplier);

        // and:
        when(servletRequest.getMethod()).thenReturn("POST");
        when(servletRequest.getQueryString())
                .thenReturn("query1=QUERY_ONE&&query2=query_two&query2=QUERY_TWO&");
        when(urlPathHelper.getPathWithinApplication(servletRequest)).thenReturn("/swagger-request-validator");
        when(servletRequest.getParameterNames())
                .thenReturn(asEnumeration("query1", "query2", "query3"));
        when(servletRequest.getParameterValues("query1"))
                .thenReturn(new String[]{"QUERY_1"});
        when(servletRequest.getParameterValues("query2"))
                .thenReturn(new String[]{"query_2", "QUERY_2"});
        when(servletRequest.getHeaderNames()).thenReturn(asEnumeration());
        when(bodySupplier.get()).thenReturn(body);

        // when:
        final Request result = classUnderTest.buildRequest(servletRequest, bodySupplier);

        // then:
        // for ContentCachingRequestWrapper it is important that the query parameters are read before (!!) the body
        ensureOrder.verify(servletRequest).getParameterNames();
        ensureOrder.verify(servletRequest).getQueryString();
        ensureOrder.verify(bodySupplier).get();
        ensureOrder.verify(servletRequest).getParameterValues("query1");
        ensureOrder.verify(servletRequest).getParameterValues("query2");

        // and:
        assertThat(result.getPath(), equalTo("/swagger-request-validator"));
        assertThat(result.getMethod(), equalTo(Request.Method.POST));
        assertThat(result.getRequestBody().get(), is(body));
        assertThat(result.getHeaders().size(), equalTo(0));
        assertThat(result.getQueryParameters().size(), equalTo(2));
        assertThat(result.getQueryParameterValues("query1"),
                equalTo(asList("QUERY_1")));
        assertThat(result.getQueryParameterValues("query2"),
                equalTo(asList("query_2", "QUERY_2")));
    }

    @Test
    public void buildResponse_failsWithoutRequiredResponse() {
        // expect:
        assertThrows(NullPointerException.class,
                () -> classUnderTest.buildResponse(null));
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
        assertThat(result.getResponseBody().isPresent(), equalTo(true));
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
        assertThat(result.getResponseBody().isPresent(), equalTo(true));
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

        verify(requestValidator, times(1)).validateRequest(request);
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
        when(urlPathHelper.getPathWithinApplication(servletRequest)).thenReturn("/swagger-request-validator");

        when(requestValidator.validateResponse("/swagger-request-validator",
                Request.Method.POST, response)).thenReturn(validationReport);

        // when:
        final ValidationReport result = classUnderTest.validateResponse(servletRequest, response);

        // then:
        verify(requestValidator, times(1))
                .validateResponse("/swagger-request-validator", Request.Method.POST, response);
        assertThat(result, is(validationReport));
    }

    @Test
    public void resolveHeadersOnResponse_noHeaderOnResponse() {
        // given:
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        // expect:
        when(servletResponse.getHeaderNames()).thenReturn(null);
        assertThat(classUnderTest.resolveHeadersOnResponse(servletResponse), anEmptyMap());

        // and:
        when(servletResponse.getHeaderNames()).thenReturn(emptyList());
        assertThat(classUnderTest.resolveHeadersOnResponse(servletResponse), anEmptyMap());
    }

    @Test
    public void resolveHeadersOnResponse_saveHeadersCurrentlyOnResponse() {
        // given:
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        // and:
        when(servletResponse.getHeaderNames()).thenReturn(asList("Header 1", "Header 2", "Header 3"));
        when(servletResponse.getHeaders("Header 1")).thenReturn(asList("Header 1: Value 1"));
        when(servletResponse.getHeaders("Header 2")).thenReturn(asList("Header 2: Value 1", "Header 2: Value 2"));
        when(servletResponse.getHeaders("Header 3")).thenReturn(asList(""));
        when(servletResponse.getHeaders("Header 1")).thenReturn(asList("Header 1: New value 1"));

        // when:
        final Map<String, List<String>> result = classUnderTest.resolveHeadersOnResponse(servletResponse);

        // then:
        assertThat(result, aMapWithSize(3));
        assertThat(result, hasEntry("Header 1", singletonList("Header 1: New value 1")));
        assertThat(result, hasEntry("Header 2", asList("Header 2: Value 1", "Header 2: Value 2")));
        assertThat(result, hasEntry("Header 3", singletonList("")));
    }

    @Test
    public void addHeadersToResponse_noHeadersToAdd() {
        // given:
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);

        // when:
        classUnderTest.addHeadersToResponse(servletResponse, null);

        // then:
        verifyNoInteractions(servletResponse);

        // when:
        classUnderTest.addHeadersToResponse(servletResponse, emptyMap());

        // then:
        verifyNoInteractions(servletResponse);
    }

    @Test
    public void addHeadersToResponse_addEachHeaderValue() {
        // given:
        final HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("Header 1", asList("Header 1: Value 1"));
        headers.put("Header 2", asList("Header 2: Value 1", "Header 2: Value 2"));
        headers.put("Header 3", asList(""));

        // when:
        classUnderTest.addHeadersToResponse(servletResponse, headers);

        // then:
        verify(servletResponse).addHeader("Header 1", "Header 1: Value 1");
        verify(servletResponse).addHeader("Header 2", "Header 2: Value 2");
        verify(servletResponse).addHeader("Header 2", "Header 2: Value 2");
        verify(servletResponse).addHeader("Header 3", "");
    }

    @Test
    public void buildRequest_realServletRequestTest() {
        // setup: prepare request
        final HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.put("headerValue", Arrays.asList("header value"));
        final HttpEntity<Object> entity = new HttpEntity<>(null, headers);

        // when: send request
        final ResponseEntity<Map> responseEntity = restTemplate
                .exchange("/test controller/path variable?queryParam=query param", HttpMethod.GET, entity, Map.class);

        // then: assert the values that spring has been set in the controller method
        final Map springRequest = (Map) responseEntity.getBody().get("springRequest");
        assertThat(springRequest.get("pathVariable"), is("path variable"));
        assertThat(springRequest.get("queryParam"), is("query param"));
        assertThat(springRequest.get("headerValue"), is("header value"));

        // and: assert the values that the validation service has been set from the servlet request
        final Map validationRequest = (Map) responseEntity.getBody().get("validationRequest");
        assertThat(validationRequest.get("path"), is("/test controller/path variable"));
        assertThat(validationRequest.get("queryParam"), is("query param"));
        assertThat(validationRequest.get("headerValue"), is("header value"));
    }

    @Test
    public void buildRequest_withUTF8EncodedQueryString() {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        when(servletRequest.getMethod()).thenReturn("GET");
        when(servletRequest.getQueryString())
                .thenReturn("name%3Da=value%3Da&q%5Bname%5D=q%7Bvalue%7D&q%5Bname%5D=q%5Bvalue%5D");
        when(servletRequest.getParameterNames()).thenReturn(asEnumeration("other", "name=a", "q[name]"));
        when(servletRequest.getParameterValues("name=a")).thenReturn(new String[]{"value=a"});
        when(servletRequest.getParameterValues("q[name]")).thenReturn(new String[]{"q{value}", "q[value]"});
        when(urlPathHelper.getPathWithinApplication(servletRequest)).thenReturn("/swagger-request-validator");
        when(servletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        final Body body = mock(Body.class);
        final Supplier<Body> bodySupplier = mock(Supplier.class);
        when(bodySupplier.get()).thenReturn(body);

        final Request result = classUnderTest.buildRequest(servletRequest, bodySupplier);

        assertThat(result.getPath(), equalTo("/swagger-request-validator"));
        assertThat(result.getMethod(), equalTo(Request.Method.GET));
        assertThat(result.getRequestBody().get(), is(body));
        assertThat(result.getHeaders().size(), equalTo(0));
        assertThat(result.getQueryParameters().size(), equalTo(2));
        assertThat(result.getQueryParameterValues("name=a"), equalTo(Arrays.asList("value=a")));
        assertThat(result.getQueryParameterValues("q[name]"), equalTo(Arrays.asList("q{value}", "q[value]")));
    }

    @Test
    public void buildRequest_withNotUTF8EncodedQueryString() {
        final HttpServletRequest servletRequest = mock(HttpServletRequest.class);

        when(servletRequest.getMethod()).thenReturn("GET");
        when(servletRequest.getQueryString())
                .thenReturn("name%3Da=value%3Da&q%5Bname%5D=q%7Bvalue%7D&q%5Bname%5D=q%5Bvalue%5D");
        when(servletRequest.getParameterNames()).thenReturn(asEnumeration("other", "name=a", "q[name]"));
        when(servletRequest.getParameterValues("name=a")).thenReturn(new String[]{"value=a"});
        when(servletRequest.getParameterValues("q[name]")).thenReturn(new String[]{"q{value}", "q[value]"});
        when(urlPathHelper.getPathWithinApplication(servletRequest)).thenReturn("/swagger-request-validator");
        when(servletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());

        final Body body = mock(Body.class);
        final Supplier<Body> bodySupplier = mock(Supplier.class);
        when(bodySupplier.get()).thenReturn(body);

        final Request result = classUnderTest.buildRequest(servletRequest, bodySupplier);

        assertThat(result.getPath(), equalTo("/swagger-request-validator"));
        assertThat(result.getMethod(), equalTo(Request.Method.GET));
        assertThat(result.getRequestBody().get(), is(body));
        assertThat(result.getHeaders().size(), equalTo(0));
        assertThat(result.getQueryParameters().size(), equalTo(2));
        assertThat(result.getQueryParameterValues("name=a"), equalTo(Arrays.asList("value=a")));
        assertThat(result.getQueryParameterValues("q[name]"), equalTo(Arrays.asList("q{value}", "q[value]")));
    }

    @SpringBootApplication
    @RestController
    @RequestMapping(value = "/test controller", produces = "application/json")
    public static class TestController {

        public static void main(final String[] args) {
            SpringApplication.run(RestServiceApplication.class, args);
        }

        @RequestMapping(method = RequestMethod.GET, value = "/{pathVariable}", produces = "application/json")
        public Map get(@PathVariable("pathVariable") final String pathVariable, @RequestParam("queryParam") final String queryParam,
                       @RequestHeader("headerValue") final String headerValue, final HttpServletRequest servletRequest) throws IOException {
            final OpenApiValidationService openApiValidationService = new OpenApiValidationService(Mockito.mock(OpenApiInteractionValidator.class),
                    new UrlPathHelper());
            final ServletInputStream inputStream = servletRequest.getInputStream();
            final Request request = openApiValidationService.buildRequest(servletRequest, () -> new InputStreamBody(inputStream));
            return new ImmutableMap.Builder()
                    .put("springRequest",
                            new ImmutableMap.Builder()
                                    .put("pathVariable", pathVariable)
                                    .put("queryParam", queryParam)
                                    .put("headerValue", headerValue)
                                    .build()
                    )
                    .put("validationRequest",
                            new ImmutableMap.Builder()
                                    .put("path", request.getPath())
                                    .put("queryParam", request.getQueryParameterValues("queryParam").iterator().next())
                                    .put("headerValue", request.getHeaderValue("headerValue"))
                                    .build()
                    )
                    .build();
        }
    }
}
