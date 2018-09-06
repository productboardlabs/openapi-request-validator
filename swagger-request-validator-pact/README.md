# Swagger Request Validator - Pact #

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-pact/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-pact)

Integrations between the Swagger Request Validator and the [Pact Consumer Driven Contracts framework](http://docs.pact.io/).

This module includes request/response adaptors that allow validation of Pact interactions with the Swagger Request
Validator. It also includes a `ValidatedPactProviderRule` that can be used as a drop-in replacement for the standard
[pact-jvm-consumer-junit](https://github.com/DiUS/pact-jvm/tree/master/pact-jvm-consumer-junit) `PactProviderRule`
to enable OpenAPI / Swagger validation of consumer expectations, and a `PactProviderValidator` that can be 
used to validate Consumer Pacts against a Provider OpenAPI / Swagger specification.

## Usage ##

```xml
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-pact</artifactId>
    <version>${swagger-request-validator.version}</version>
</dependency>
```

See the [examples module](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/?at=master)
for examples of how the Pact module can be used.

This module includes support for Pact validation both from the Consumer and Provider side of the Pact interaction.

### Consumer validation

On the Consumer side, the module can be used to validate Consumer expectations against a Provider OpenAPI / Swagger spec 
during the Consumer test execution. This can lead to faster detection of invalid expectations on the Consumer side.

There are two ways to perform Consumer-side validation:

#### ValidatedPactProviderRule
The simplest way to use the integration is to replace the usage of the `PactProviderRule` (from the [pact-jvm-consumer-junit](https://github.com/DiUS/pact-jvm/tree/master/pact-jvm-consumer-junit) library) with the `ValidatedPactProviderRule`.

Replace:
```java
@Rule
public PactProviderRule provider =
        new PactProviderRule(PROVIDER_ID, this);
```

With:
```java
@Rule
public ValidatedPactProviderRule provider =
        new ValidatedPactProviderRule("http://petstore.swagger.io/v2/swagger.json", null, PROVIDER_ID, this);
```

If you want more control over how the validator is configured, you can optionally pass in a pre-configured `OpenApiInteractionValidator`:

```java
@Rule
public ValidatedPactProviderRule provider =
        new ValidatedPactProviderRule(
                OpenApiInteractionValidator.createFor("http://petstore.swagger.io/v2/swagger.json").build(), null, PROVIDER_ID, this);
```
When configuring the validator you might want to set the level resolver using `PactLevelResolverFactory.create()` to ensure that validation
behavior matches the conventions in Pact of only defining the minimal set of fields etc. you care about.

*Note*:
To use the JUnit rule you will need to ensure the following dependencies are added to your project POM

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-consumer-junit_2.11</artifactId>
    <scope>test</scope>
</dependency>
```

#### Manual interaction validation

Alternatively you can manually validate an interaction.

```java
final RequestResponseInteraction interaction = ...
final OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(apiSpecUrl).build();
final ValidationReport report = validator.validate(
                new PactRequest(interaction.getRequest()),
                new PactResponse(interaction.getResponse()));
```

### Provider validation

On the Provider side, the module can be used to validate Consumer Pacts against a Provider OpenAPI / Swagger spec as part 
of the Provider's test suite or during the CI process etc.

#### PactProviderValidator

The `PactProviderValidator` validates Consumer Pact files against a Provider OpenAPI / Swagger spec. It can be used with
Consumer Pacts from a [Pact broker](https://docs.pact.io/documentation/sharings_pacts.html), and/or with Pact files
retrieved from specific locations (file system, remote URLs etc.)

```java
final PactProviderValidator validator = 
        PactProviderValidator
            .createFor(API_SPEC_URL)
            .withPactsFrom(BROKER_URL, PROVIDER_ID)
            .build();
            
final PactProviderValidationResults results = validator.validate();

assertThat(results.getValidationFailureReport(), result.hasErrors(), is(false));
```

*Note*:
To use the `PactProviderValidator` rule you will need to ensure the `pact-jvm-provider` library is on the classpath.

```xml
<dependency>
    <groupId>au.com.dius</groupId>
    <artifactId>pact-jvm-provider_2.11</artifactId>
    <scope>test</scope>
</dependency>
```
