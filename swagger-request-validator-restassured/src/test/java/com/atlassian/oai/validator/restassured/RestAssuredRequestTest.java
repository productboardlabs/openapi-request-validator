package com.atlassian.oai.validator.restassured;

import com.atlassian.oai.validator.model.Request;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static io.restassured.RestAssured.given;
import static java.util.Arrays.stream;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RestAssuredRequestTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule();

    @Before
    public void setup() {
        wireMock.stubFor(any(anyUrl()).willReturn(responseDefinition().withStatus(200)));
    }

    @Test
    public void mapsRequestComponentsCorrectly() {

        final CapturingFilter requestCaptor = new CapturingFilter();
        given()
                .port(wireMock.port())
                .filter(requestCaptor)
                .when()
                .header("X-My-Header", "foo", "bar")
                .get("/path")
                .then()
                .assertThat()
                .statusCode(200);

        final Request classUnderTest = requestCaptor.getRequest();
        assertThat(classUnderTest.getPath(), is("/path"));
        assertThat(classUnderTest.getMethod(), is(Request.Method.GET));
        assertThat(classUnderTest.getRequestBody().isPresent(), is(false));
        assertThat(classUnderTest.getHeaderValues("x-my-header"), contains("foo", "bar"));
        assertThat(classUnderTest.getHeaderValue("x-my-HEADER").isPresent(), is(true));
        assertThat(classUnderTest.getHeaderValue("not-a-header").isPresent(), is(false));
    }

    @Test
    public void mapsQueryAndRequestParams_whenGet() {

        final CapturingFilter requestCaptor = new CapturingFilter();
        given()
                .port(wireMock.port())
                .queryParam("queryParam", "value1")
                .param("requestParam", "value2")
                .filter(requestCaptor)
                .when()
                .get("/path")
                .then()
                .assertThat()
                .statusCode(200);

        final Request classUnderTest = requestCaptor.getRequest();
        assertThat(classUnderTest.getQueryParameters(), contains("queryParam", "requestParam"));
        assertThat(classUnderTest.getQueryParameterValues("queryParam"), contains("value1"));
        assertThat(classUnderTest.getQueryParameterValues("requestParam"), contains("value2"));
    }

    @Test
    public void mapsQueryParamsOnly_whenPost() {

        final CapturingFilter requestCaptor = new CapturingFilter();
        given()
                .port(wireMock.port())
                .queryParam("queryParam", "value0", "value1")
                .queryParam("queryparam", "VALUE0")
                .param("requestParam", "value2")
                .filter(requestCaptor)
                .when()
                .post("/path")
                .then()
                .assertThat()
                .statusCode(200);

        final Request classUnderTest = requestCaptor.getRequest();
        assertThat(classUnderTest.getQueryParameters(), containsInAnyOrder("queryparam"));
        assertThat(classUnderTest.getQueryParameterValues("queryParam"), containsInAnyOrder("value0", "value1", "VALUE0"));
        assertThat(classUnderTest.getQueryParameterValues("requestParam"), empty());
    }

    @Test
    public void mapsRequestBodyCorrectly_whenByteArray() throws IOException {
        final CapturingFilter requestCaptor = new CapturingFilter();
        given()
                .port(wireMock.port())
                .contentType("multipart/form-data")
                .filter(requestCaptor)
                .multiPart("requestParam", "value2")
                .body(new byte[0])
                .when()
                .put("/path")
                .then()
                .assertThat()
                .statusCode(200);

        final Request classUnderTest = requestCaptor.getRequest();
        assertThat(classUnderTest.getMethod(), is(Request.Method.PUT));
        assertThat(classUnderTest.getRequestBody().get().toString(StandardCharsets.UTF_8), is(""));
        assertThat(classUnderTest.getContentType(), optionalWithValue(is("multipart/form-data")));
    }

    @Test
    public void mapsRequestBodyCorrectly_forByteArrays_whenNoContentTypeDefined() throws IOException {
        final CapturingFilter requestCaptor = new CapturingFilter();
        given()
                .port(wireMock.port())
                .filter(requestCaptor)
                .body("Something 123 !@#")
                .when()
                .put("/path")
                .then()
                .assertThat()
                .statusCode(200);

        final Request classUnderTest = requestCaptor.getRequest();
        assertThat(classUnderTest.getRequestBody().get().toString(StandardCharsets.UTF_8), is("Something 123 !@#"));
    }

    @Test
    public void mapsRequestBodyCorrectly_forInputStream() throws IOException {
        final CapturingFilter requestCaptor = new CapturingFilter();
        given()
                .port(wireMock.port())
                .contentType("text/plain")
                .filter(requestCaptor)
                .body(new ByteArrayInputStream("foo".getBytes()))
                .when()
                .put("/path")
                .then()
                .assertThat()
                .statusCode(200);

        final Request classUnderTest = requestCaptor.getRequest();
        assertThat(classUnderTest.getRequestBody().get().toString(StandardCharsets.UTF_8), is("foo"));
    }

    @Test
    public void supportsAllRequestMethods() {
        stream(io.restassured.http.Method.values())
                .forEach(m -> assertThat(captureRequest(m).getMethod(), is(Request.Method.valueOf(m.name()))));
    }

    private Request captureRequest(final io.restassured.http.Method method) {
        final CapturingFilter requestCaptor = new CapturingFilter();
        given()
                .port(wireMock.port())
                .filter(requestCaptor)
                .when()
                .request(method, "/path")
                .then()
                .assertThat()
                .statusCode(200);

        return requestCaptor.getRequest();
    }

}
