# OpenAPI Request Validator - WireMock - JUnit5

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/openapi-request-validator-wiremock-junit5/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/openapi-request-validator-wiremock-junit5)

WireMock extension `OpenApiValidationListener` that applies OpenAPI / Swagger validation to [WireMock](http://wiremock.org/) requests and responses.
This extension is compatible with JUnit5+.

## Usage

`OpenApiValidationListener` implements the WireMock `ServeEventListener` interface and is the recommended extension
for new projects. It performs validation after the full request/response cycle has completed.

1. Add dependency to your project.
```
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>openapi-request-validator-wiremock-junit5</artifactId>
    <version>${openapi-request-validator.version}</version>
</dependency>
```
2. Add the WireMock extension `OpenApiValidationListener` to your WireMock instance. See examples below.
3. Use `getReport()` and/or the convenience method `assertValidationPassed()` to check the validations.

Example with global validation (one OpenAPI spec URL for all):
```java
private static final OpenApiValidationListener VALIDATION_LISTENER = new OpenApiValidationListener(SPEC_URL);

@RegisterExtension
private static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
    .options(wireMockConfig()
        .dynamicPort()
        .extensions(VALIDATION_LISTENER))
    .build();

@AfterEach
void teardown() {
    VALIDATION_LISTENER.reset();
}

@Test
void testFoo() {
    // Some interactions with the WireMock server
    VALIDATION_LISTENER.assertValidationPassed();
}
```

Example with local validation (OpenAPI spec URL per stub mapping):
```java
private static final OpenApiValidationListener VALIDATION_LISTENER = new OpenApiValidationListener();

@RegisterExtension
private static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
    .options(wireMockConfig()
        .dynamicPort()
        .extensions(VALIDATION_LISTENER))
    .build();

@BeforeEach
void setUp() {
    WIREMOCK.stubFor(get(urlPathMatching("/test"))
        .withServeEventListener("open-api-validation-listener",
            new OpenApiValidationListener.OasUrlParameter(SPEC_URL))
        .willReturn(ok()));
}
```
