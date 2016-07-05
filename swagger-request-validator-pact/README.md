# Swagger Request Validator - Pact #

Integrations of the Swagger Request Validator with the [Pact Consumer Driven Contracts framework](http://docs.pact.io/).

This module includes request/response adaptors that allow validation of Pact interactions with the Swagger Request
Validator, and a `ValidatedPactProviderRule` that can be used as a drop-in replacement for the standard
[pact-jvm-consumer-junit](https://github.com/DiUS/pact-jvm/tree/master/pact-jvm-consumer-junit) `PactProviderRule`
to enable Swagger validation of consumer expectations.

## Usage ##

See the [examples module](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/?at=master)
for examples of how the Pact module can be used.

### ValidatedPactProviderRule ###
The simplest way to use the integration is to replace the usage of the `PactProviderRule` with the `ValidatedPactProviderRule`.

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

### Manual interaction validation

```
final RequestResponseInteraction interaction = ...
final SwaggerRequestResponseValidator validator = new SwaggerRequestResponseValidator(swaggerJsonUrl, basePathOverride);
final ValidationReport report = validator.validate(
                new PactRequest(interaction.getRequest()),
                new PactResponse(interaction.getResponse()));
```