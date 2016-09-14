package com.atlassian.oai.validator.restassured;

import com.atlassian.oai.validator.model.Request;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RestAssuredRequestTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule();

    @Before
    public void setup() {
        wireMock.stubFor(get(anyUrl()).willReturn(responseDefinition().withStatus(200)));
        wireMock.stubFor(post(anyUrl()).willReturn(responseDefinition().withStatus(200)));
    }

    @Test
    public void mapsRequestComponentsCorrectly() {

        final CapturingFilter requestCaptor = new CapturingFilter();
        given()
                .port(wireMock.port())
                .filter(requestCaptor)
        .when()
                .get("/path")
        .then()
                .assertThat()
                .statusCode(200);

        final RestAssuredRequest classUnderTest = requestCaptor.getRequest();
        assertThat(classUnderTest.getPath(), is("/path"));
        assertThat(classUnderTest.getMethod(), is(Request.Method.GET));
        assertThat(classUnderTest.getBody().isPresent(), is(false));
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

        final RestAssuredRequest classUnderTest = requestCaptor.getRequest();

        assertThat(classUnderTest.getQueryParameters(), contains("queryParam", "requestParam"));
        assertThat(classUnderTest.getQueryParameterValues("queryParam"), contains("value1"));
        assertThat(classUnderTest.getQueryParameterValues("requestParam"), contains("value2"));
    }

    @Test
    public void mapsQueryParamsOnly_whenPost() {

        final CapturingFilter requestCaptor = new CapturingFilter();
        given()
                .port(wireMock.port())
                .queryParam("queryParam", "value1")
                .param("requestParam", "value2")
                .filter(requestCaptor)
        .when()
                .post("/path")
        .then()
                .assertThat()
                .statusCode(200);

        final RestAssuredRequest classUnderTest = requestCaptor.getRequest();

        assertThat(classUnderTest.getQueryParameters(), contains("queryParam"));
        assertThat(classUnderTest.getQueryParameterValues("queryParam"), contains("value1"));
        assertThat(classUnderTest.getQueryParameterValues("requestParam"), empty());

    }

    @Test
    public void getBody_returnsEmpty_whenNoBodyInRequest() {
        final CapturingFilter requestCaptor = new CapturingFilter();
        given()
                .port(wireMock.port())
                .filter(requestCaptor)
        .when()
                .get("/path")
        .then()
                .assertThat()
                .statusCode(200);

        final RestAssuredRequest classUnderTest = requestCaptor.getRequest();

        assertThat(classUnderTest.getBody(), is(Optional.empty()));
    }

    @Test
    public void getBody_returnsBody_whenBodyInRequest() {
        final CapturingFilter requestCaptor = new CapturingFilter();
        given()
                .port(wireMock.port())
                .body("The body")
                .filter(requestCaptor)
        .when()
                .post("/path")
        .then()
                .assertThat()
                .statusCode(200);

        final RestAssuredRequest classUnderTest = requestCaptor.getRequest();
        assertThat(classUnderTest.getBody().get(), is("The body"));
    }

}
