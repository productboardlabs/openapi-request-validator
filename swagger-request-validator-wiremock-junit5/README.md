# Swagger Request Validator - WireMock - JUnit5

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-wiremock-juni5/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-wiremock-junit5)

WireMock extension `OpenApiValidator` that applies OpenAPI / Swagger validation to [WireMock](http://wiremock.org/) requests and responses. This extension is compatible with JUnit5.

## Usage

1. Add dependency to your project.
```
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-wiremock-junit5</artifactId>
    <version>${swagger-request-validator.version}</version>
</dependency>
```
2. Add the WireMock extension `OpenApiValidator` to your WireMock instance. See examples below.
3. Use `getReport()` and/or the convenience method `assertValidationPassed()` to check the validations.

### How it works

The extension `OpenApiValidator` is compatible with JUnit5 and can be added to a {@link com.github.tomakehurst.wiremock.WireMockServer} instance. It can be:
- Globally applied by providing one OpenAPI spec url when instantiating. This applies to the case where only one specific service is stubbed by a WireMock server.
- Locally applied per stub mapping. This applies to cases where multiple services are being stubbed by one WireMock server.

The current validation report can be accessed with `getReport()`, and the convenience method `assertValidationPassed()` can be used to check for validation errors and throw an exception if any are found.

- Important: The extension will continue accumulating validation errors on each call to the WireMock server.
Call `reset()` before your test to ensure you only get validation errors for the current test execution.

Example with global validation (one OpenAPI spec URL for all):
```
private static final OpenApiValidator OPEN_API_VALIDATOR = new OpenApiValidator(SPEC_URL);

@RegisterExtension
private static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
    .options(wireMockConfig()
        .dynamicPort()
        .extensions(openApiValidator))
    .build();

@AfterEach
void teardown() {
    OPEN_API_VALIDATOR.reset();
}

@Test
void testFoo() {
    // Some interactions with the WireMock server
    OPEN_API_VALIDATOR.assertValidationPassed();
}
```

Example with local validation (OpenAPI spec URL per stub mapping)
```
private static final OpenApiValidator OPEN_API_VALIDATOR = new OpenApiValidator();

@RegisterExtension
private static final WireMockExtension WIREMOCK = WireMockExtension.newInstance()
 .options(wireMockConfig()
     .dynamicPort()
     .extensions(OPEN_API_VALIDATOR))
.build();

@BeforeEach
void setUp() {
    WIREMOCK.stubFor(get(urlPathMatching("/test"))
        .withPostServeAction("open-api-validator", new OpenApiValidator.OasUrlParameter(SPEC_URL))
        .willReturn(ok());
}
```
