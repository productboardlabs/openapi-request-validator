# Swagger Request Validator - WireMock #

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-wiremock/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-wiremock)

Integrations between the Swagger Request Validator and the [WireMock HTTP mocking framework](http://wiremock.org/).

This module includes request/response adaptors that allow validation of WireMock stubs with the Swagger Request
Validator, and a `ValidatedWireMockRule` that can be used as a drop-in replacement for the standard WireMock
`WireMockRule` to enable OpenAPI / Swagger validation of stubs.

## Usage ##

```
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-wiremock</artifactId>
    <version>${swagger-request-validator.version}</version>
</dependency>
```

See the [examples module](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/?at=master)
for running examples of how the WireMock module can be used.

### ValidatedWireMockRule ###
The simplest way to use the integration is to replace the usage of the `WireMockRule` with the `ValidatedWireMockRule`.

Replace:
```
@Rule
public WireMockRule wireMockRule = new WireMockRule(PORT);
```

With:
```
@Rule
public ValidatedWireMockRule wireMockRule = new ValidatedWireMockRule(SPEC_URL, PORT);
```

*Note*:
To use the JUnit rule you will need to ensure the following dependencies are added to your project POM

```
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <scope>test</scope>
</dependency>
```

### Validation listener ##

If you already have WireMock setup, or are using the `WireMockServer` instead of the `WireMockRule` you can use the
`OpenApiValidationListener` to perform request/response validation on your WireMock stubs.

See the javadoc for more details on usage.

```
@Rule
public WireMockRule wireMockRule;
private OpenApiValidationListener validationListener;

public ValidatedWireMockListenerTestExample() {
    this.validationListener = new OpenApiValidationListener(SPEC_URL);

    this.wireMockRule = new WireMockRule(PORT);
    this.wireMockRule.addMockServiceRequestListener(validationListener);
}

@After
public void teardown() {
    this.validationListener.reset();
}

@Test
public void testFoo() {
    ...
    validationListener.assertValidationPassed();
}
```