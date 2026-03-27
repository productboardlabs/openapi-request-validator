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

/**
 * Tests for the global mode constructor of {@link OpenApiValidationListener} — where the OpenAPI spec URL
 * is provided at construction time and {@link OpenApiValidationListener#applyGlobally()} returns {@code true},
 * so WireMock applies the listener to every served request without per-stub wiring.
 */
public class OpenApiValidationListenerGlobalModeTest {

    private static final String VALID_PATH = "/hello/" + UUID.randomUUID();
    private static final String INVALID_API_PATH = "/world/" + UUID.randomUUID();

    private static final String VALID_RESPONSE_BODY = "{\"message\":\"Hello world!\"}";
    private static final String INVALID_RESPONSE_BODY = "{\"msg\":\"Hello world!\"}";

    private static final String OAS3_FILE = "api-oai3.yaml";
    private static final String OAS2_FILE = "api-swagger2.json";

    private static final OpenApiValidationListener OAS3_LISTENER = new OpenApiValidationListener(OAS3_FILE);

    @RegisterExtension
    private static final WireMockExtension WIREMOCK_OAS3 = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .extensions(OAS3_LISTENER))
            .build();

    private static final OpenApiValidationListener OAS2_LISTENER = new OpenApiValidationListener(OAS2_FILE);

    @RegisterExtension
    private static final WireMockExtension WIREMOCK_OAS2 = WireMockExtension.newInstance()
            .options(wireMockConfig()
                    .dynamicPort()
                    .extensions(OAS2_LISTENER))
            .build();

    @AfterEach
    void teardown() {
        OAS3_LISTENER.reset();
        OAS2_LISTENER.reset();
    }

    @Test
    void shouldPassValidationForValidInteractionOas3() {
        createStub(WIREMOCK_OAS3, VALID_PATH, VALID_RESPONSE_BODY);
        given().port(WIREMOCK_OAS3.getPort())
                .when().get(VALID_PATH)
                .then().statusCode(200);

        OAS3_LISTENER.assertValidationPassed();
    }

    @Test
    void shouldPassValidationForValidInteractionOas2() {
        createStub(WIREMOCK_OAS2, VALID_PATH, VALID_RESPONSE_BODY);
        given().port(WIREMOCK_OAS2.getPort())
                .when().get(VALID_PATH)
                .then().statusCode(200);

        OAS2_LISTENER.assertValidationPassed();
    }

    @Test
    void shouldDetectInvalidRequestOas3() {
        createStub(WIREMOCK_OAS3, INVALID_API_PATH, VALID_RESPONSE_BODY);
        given().port(WIREMOCK_OAS3.getPort())
                .when().get(INVALID_API_PATH)
                .then().statusCode(200);

        await().pollDelay(Duration.ofMillis(100))
                .until(() -> !OAS3_LISTENER.getReport().getMessages().isEmpty());

        final OpenApiValidationException ex = assertThrows(OpenApiValidationException.class,
                OAS3_LISTENER::assertValidationPassed);
        final List<ValidationReport.Message> messages = ex.getValidationReport().getMessages();

        assertThat(messages, hasSize(1));
        assertThat(messages.get(0).getKey(), is("validation.request.path.missing"));
        assertThat(messages.get(0).getLevel(), is(ValidationReport.Level.ERROR));
        assertThat(messages.get(0).getMessage(),
                is("No API path found that matches request '" + INVALID_API_PATH + "'."));
    }

    @Test
    void shouldDetectInvalidRequestOas2() {
        createStub(WIREMOCK_OAS2, INVALID_API_PATH, VALID_RESPONSE_BODY);
        given().port(WIREMOCK_OAS2.getPort())
                .when().get(INVALID_API_PATH)
                .then().statusCode(200);

        await().pollDelay(Duration.ofMillis(100))
                .until(() -> !OAS2_LISTENER.getReport().getMessages().isEmpty());

        final OpenApiValidationException ex = assertThrows(OpenApiValidationException.class,
                OAS2_LISTENER::assertValidationPassed);
        final List<ValidationReport.Message> messages = ex.getValidationReport().getMessages();

        assertThat(messages, hasSize(1));
        assertThat(messages.get(0).getKey(), is("validation.request.path.missing"));
        assertThat(messages.get(0).getLevel(), is(ValidationReport.Level.ERROR));
        assertThat(messages.get(0).getMessage(),
                is("No API path found that matches request '" + INVALID_API_PATH + "'."));
    }

    @Test
    void shouldDetectInvalidResponseOas3() {
        createStub(WIREMOCK_OAS3, VALID_PATH, INVALID_RESPONSE_BODY);
        given().port(WIREMOCK_OAS3.getPort())
                .when().get(VALID_PATH)
                .then().statusCode(200);

        await().pollDelay(Duration.ofMillis(100))
                .until(() -> !OAS3_LISTENER.getReport().getMessages().isEmpty());

        final OpenApiValidationException ex = assertThrows(OpenApiValidationException.class,
                OAS3_LISTENER::assertValidationPassed);
        final List<ValidationReport.Message> messages = ex.getValidationReport().getMessages();

        assertThat(messages, hasSize(2));
        assertThat(messages.get(0).getKey(), is("validation.response.body.schema.required"));
        assertThat(messages.get(0).getLevel(), is(ValidationReport.Level.ERROR));
        assertThat(messages.get(0).getMessage(), is("required property 'message' not found"));
        assertThat(messages.get(1).getKey(), is("validation.response.body.schema.additionalProperties"));
        assertThat(messages.get(1).getLevel(), is(ValidationReport.Level.ERROR));
        assertThat(messages.get(1).getMessage(),
                is("property 'msg' is not defined in the schema and the schema does not allow additional properties"));
    }

    @Test
    void shouldDetectInvalidResponseOas2() {
        createStub(WIREMOCK_OAS2, VALID_PATH, INVALID_RESPONSE_BODY);
        given().port(WIREMOCK_OAS2.getPort())
                .when().get(VALID_PATH)
                .then().statusCode(200);

        await().pollDelay(Duration.ofMillis(100))
                .until(() -> !OAS2_LISTENER.getReport().getMessages().isEmpty());

        final OpenApiValidationException ex = assertThrows(OpenApiValidationException.class,
                OAS2_LISTENER::assertValidationPassed);
        final List<ValidationReport.Message> messages = ex.getValidationReport().getMessages();

        assertThat(messages, hasSize(2));
        assertThat(messages.get(0).getKey(), is("validation.response.body.schema.required"));
        assertThat(messages.get(0).getLevel(), is(ValidationReport.Level.ERROR));
        assertThat(messages.get(0).getMessage(), is("required property 'message' not found"));
        assertThat(messages.get(1).getKey(), is("validation.response.body.schema.additionalProperties"));
        assertThat(messages.get(1).getLevel(), is(ValidationReport.Level.ERROR));
        assertThat(messages.get(1).getMessage(),
                is("property 'msg' is not defined in the schema and the schema does not allow additional properties"));
    }

    /** Creates a stub without per-stub listener wiring — the global listener handles all requests. */
    private void createStub(final WireMockExtension wiremock, final String path, final String responseBody) {
        wiremock.stubFor(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody))
        );
    }
}
