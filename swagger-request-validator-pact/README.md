# Swagger Request Validator - Pact #

Integrations of the Swagger Request Validator with the [Pact Consumer Driven Contracts framework](http://docs.pact.io/).

This module includes request/response adaptors that allow validation of Pact interactions with the Swagger Request
Validator, and a `ValidatedPactProviderRule` that can be used as a drop-in replacement for the standard
[pact-jvm-consumer-junit](https://github.com/DiUS/pact-jvm/tree/master/pact-jvm-consumer-junit) `PactProviderRule`
to enable Swagger validation of consumer expectations.

## Usage ##

```
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-pact</artifactId>
    <version>${swagger-request-validator.version}</version>
</dependency>
```

See the [examples module](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/?at=master)
for examples of how the Pact module can be used.

### ValidatedPactProviderRule ###
The simplest way to use the integration is to replace the usage of the `PactProviderRule` (from the [pact-jvm-consumer-junit](https://github.com/DiUS/pact-jvm/tree/master/pact-jvm-consumer-junit) library) with the `ValidatedPactProviderRule`.

Replace:
```
@Rule
public PactProviderRule provider =
        new PactProviderRule(PROVIDER_ID, this);
```

With:
```
@Rule
public ValidatedPactProviderRule provider =
        new ValidatedPactProviderRule("http://petstore.swagger.io/v2/swagger.json", null, PROVIDER_ID, this);
```

*Note*:
To use the JUnit rule you will need to ensure the following dependencies are added to your project POM

```
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

### Manual interaction validation

Alternatively you can manually validate an interaction.
```
final RequestResponseInteraction interaction = ...
final SwaggerRequestResponseValidator validator = SwaggerRequestResponseValidator.createFor(swaggerJsonUrl).build();
final ValidationReport report = validator.validate(
                new PactRequest(interaction.getRequest()),
                new PactResponse(interaction.getResponse()));
```