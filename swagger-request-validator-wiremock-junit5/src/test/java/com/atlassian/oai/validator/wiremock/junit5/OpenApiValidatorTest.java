package com.atlassian.oai.validator.wiremock.junit5;

import com.atlassian.oai.validator.report.ValidationReport;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OpenApiValidatorTest {

    private static final String VALID_PATH = "/hello/" + UUID.randomUUID();

    private static final String INVALID_API_PATH = "/world/" + UUID.randomUUID();

    private static final String VALID_RESPONSE_BODY = "{\"message\":\"Hello world!\"}";

    private static final String INVALID_RESPONSE_BODY = "{\"msg\":\"Hello world!\"}";

    private static final String OAS3_FILE = "api-oai3.yaml";

    private static final String OAS2_FILE = "api-swagger2.json";

    private static final OpenApiValidator OPEN_API_VALIDATOR = new OpenApiValidator();

    @RegisterExtension
    private static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .extensions(OPEN_API_VALIDATOR))
            .build();

    @AfterEach
    void teardown() {
        OPEN_API_VALIDATOR.reset();
    }

    @Test
    void shouldPassValidationInCaseOfValidInteractionOas3() {
        testEndpoint(OAS3_FILE, VALID_PATH, VALID_RESPONSE_BODY);
        OPEN_API_VALIDATOR.assertValidationPassed();
    }

    @Test
    void shouldPassValidationInCaseOfValidInteractionOas2() {
        testEndpoint(OAS2_FILE, VALID_PATH, VALID_RESPONSE_BODY);
        OPEN_API_VALIDATOR.assertValidationPassed();
    }

    @Test
    void shouldDetectInvalidRequestInCaseOfOas3() {
        final List<ValidationReport.Message> actualMessages = testValidationException(OAS3_FILE, INVALID_API_PATH, VALID_RESPONSE_BODY);

        assertThat(actualMessages, hasSize(1));
        assertThat(actualMessages.get(0).getKey(), is("validation.request.path.missing"));
        assertThat(actualMessages.get(0).getLevel(), is(ValidationReport.Level.ERROR));
        assertThat(actualMessages.get(0).getMessage(), is("No API path found that matches request '" + INVALID_API_PATH + "'."));
    }

    @Test
    void shouldDetectInvalidRequestInCaseOfOas2() {
        final List<ValidationReport.Message> actualMessages = testValidationException(OAS2_FILE, INVALID_API_PATH, VALID_RESPONSE_BODY);

        assertThat(actualMessages, hasSize(1));
        assertThat(actualMessages.get(0).getKey(), is("validation.request.path.missing"));
        assertThat(actualMessages.get(0).getLevel(), is(ValidationReport.Level.ERROR));
        assertThat(actualMessages.get(0).getMessage(), is("No API path found that matches request '" + INVALID_API_PATH + "'."));
    }

    @Test
    void shouldDetectInvalidResponseInCaseOfOas3() {
        final List<ValidationReport.Message> actualMessages = testValidationException(OAS3_FILE, VALID_PATH, INVALID_RESPONSE_BODY);

        assertThat(actualMessages, hasSize(2));
        assertThat(actualMessages.get(0).getKey(), is("validation.response.body.schema.additionalProperties"));
        assertThat(actualMessages.get(0).getLevel(), is(ValidationReport.Level.ERROR));
        assertThat(actualMessages.get(0).getMessage(), is("Object instance has properties which are not allowed by the schema: [\"msg\"]"));
        assertThat(actualMessages.get(1).getKey(), is("validation.response.body.schema.required"));
        assertThat(actualMessages.get(1).getLevel(), is(ValidationReport.Level.ERROR));
        assertThat(actualMessages.get(1).getMessage(), is("Object has missing required properties ([\"message\"])"));
    }

    @Test
    void shouldDetectInvalidResponseInCaseOfOas2() {
        final List<ValidationReport.Message> actualMessages = testValidationException(OAS2_FILE, VALID_PATH, INVALID_RESPONSE_BODY);

        assertThat(actualMessages, hasSize(2));
        assertThat(actualMessages.get(0).getKey(), is("validation.response.body.schema.additionalProperties"));
        assertThat(actualMessages.get(0).getLevel(), is(ValidationReport.Level.ERROR));
        assertThat(actualMessages.get(0).getMessage(), is("Object instance has properties which are not allowed by the schema: [\"msg\"]"));
        assertThat(actualMessages.get(1).getKey(), is("validation.response.body.schema.required"));
        assertThat(actualMessages.get(1).getLevel(), is(ValidationReport.Level.ERROR));
        assertThat(actualMessages.get(1).getMessage(), is("Object has missing required properties ([\"message\"])"));
    }

    private List<ValidationReport.Message> testValidationException(final String specFile, final String requestPath, final String responseBody) {
        testEndpoint(specFile, requestPath, responseBody);

        await().pollDelay(Duration.ofMillis(100))
                .until(() -> !OPEN_API_VALIDATOR.getReport().getMessages().isEmpty());

        final OpenApiValidationException openApiValidationException = assertThrows(OpenApiValidationException.class,
                OPEN_API_VALIDATOR::assertValidationPassed);

        return openApiValidationException.getValidationReport().getMessages();
    }

    private void testEndpoint(final String specFile, final String requestPath, final String responseBody) {
        createStub(specFile, requestPath, responseBody);
        given().log().all()
                .port(WIREMOCK.getPort())
                .when()
                .get(requestPath)
                .then()
                .log().all()
                .statusCode(200);
    }

    private void createStub(final String spec, final String path, final String responseBody) {
        WIREMOCK.stubFor(get(urlEqualTo(path))
                .withPostServeAction("open-api-validator", new OpenApiValidator.OasUrlParameter(spec))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody))
        );
    }

}
