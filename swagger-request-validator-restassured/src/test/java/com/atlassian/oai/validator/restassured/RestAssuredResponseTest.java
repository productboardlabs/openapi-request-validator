package com.atlassian.oai.validator.restassured;

import com.atlassian.oai.validator.model.Response;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class RestAssuredResponseTest {

    @Rule
    public WireMockRule wireMock = new WireMockRule();

    @Before
    public void setup() {

    }

    @Test
    public void responseHeadersAreMappedCorrectly() {

        wireMock.stubFor(any(anyUrl()).willReturn(responseDefinition().withHeader("custom-header", "0").withStatus(200)));

        final Response response = RestAssuredResponse.of(given()
                .port(wireMock.port())
                .when()
                .get("/path")
                .then()
                .assertThat()
                .statusCode(200)
                .extract()
                .response());

        assertThat("custom-header must be preserved", response.getHeaderValue("custom-header"), is(Optional.of("0")));
        assertThat("ContentType must be empty", response.getContentType(), is(Optional.empty()));

    }

    @Test
    public void contentTypeHeaderIsMappedCorrectly() {

        wireMock.stubFor(any(anyUrl()).willReturn(responseDefinition().withHeader("Content-Type", "application/json").withStatus(200)));

        final Response response = RestAssuredResponse.of(given()
                .port(wireMock.port())
                .when()
                .get("/path")
                .then()
                .assertThat()
                .statusCode(200)
                .extract()
                .response());

        assertThat("Content-Type must be available via general headers", response.getHeaderValue("Content-Type"), is(Optional.of("application/json")));
        assertThat("ContentType must be available via direct method", response.getContentType(), is(Optional.of("application/json")));

    }

}

