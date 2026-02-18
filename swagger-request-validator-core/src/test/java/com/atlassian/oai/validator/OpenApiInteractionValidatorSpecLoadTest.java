package com.atlassian.oai.validator;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class OpenApiInteractionValidatorSpecLoadTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    public void withAuthHeaderData_sendsMultipleHeaders_whenCalledMultipleTimes() {
        wireMock.stubFor(get("/openapi.json")
                .willReturn(aResponse()
                        .withStatus(200)
                        // Actual spec doesn't matter, just needs to be valid
                        .withBody("{\n" +
                                "\"openapi\": \"3.0.0\",\n" +
                                "\"info\": {\"title\": \"Test\", \"version\": \"1.0.0\"},\n" +
                                "\"paths\": {}\n" +
                                "}")));

        OpenApiInteractionValidator.createForSpecificationUrl(wireMock.url("/openapi.json"))
                .withAuthHeaderData("X-Header-1", "foo")
                .withAuthHeaderData("X-Header-2", "bar")
                .build();

        wireMock.verify(getRequestedFor(urlPathEqualTo("/openapi.json"))
                .withHeader("X-Header-1", equalTo("foo"))
                .withHeader("X-Header-2", equalTo("bar"))
        );
    }
}
