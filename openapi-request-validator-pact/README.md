# OpenAPI Request Validator - Pact #

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/openapi-request-validator-pact/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/openapi-request-validator-pact)

Integrations between the OpenAPI Request Validator and the [Pact Consumer Driven Contracts framework](http://docs.pact.io/).

This module bridges two complementary testing strategies:
- **Pact** ensures that consumers and providers agree on their API contract through consumer-driven contract testing.
- **openapi-request-validator** ensures that HTTP interactions conform to an OpenAPI / Swagger specification.

Together they give you confidence that your consumer expectations are not only agreed upon with providers, but also
valid according to the published API specification.

## Contents ##

- [Consumer-side validation](#consumer-side-validation) — validate consumer expectations against an API spec during consumer tests
- [Provider-side validation](#provider-side-validation) — validate consumer pact files against a provider API spec
- [Manual interaction validation](#manual-interaction-validation) — validate individual pact interactions directly

---

## Dependency ##

```xml
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>openapi-request-validator-pact</artifactId>
    <version>${openapi-request-validator.version}</version>
</dependency>
```

---

## Consumer-side validation ##

On the consumer side, the module validates consumer expectations against a provider OpenAPI / Swagger spec during
the consumer test execution. This gives consumers fast feedback if their expectations fail to meet the format
expected by the provider API — before a pact is even published.

### ValidatedPactConsumerTestExtension (JUnit 5) ###

`ValidatedPactConsumerTestExtension` is a JUnit 5 extension that wraps the standard `PactConsumerTestExt` from
the `pact-jvm` library and additionally validates each pact interaction against the provider OpenAPI spec before
the test body executes.

If validation fails, a `PactValidationError` is thrown, the test is marked as failed, and the specific validation
errors are included in the failure message.

#### Setup ####

Register the extension with `@RegisterExtension` (instead of `@ExtendWith(PactConsumerTestExt.class)`), and
annotate your test class with `@PactTestFor` as usual:

```java
@PactTestFor(providerName = "MyProvider")
public class MyConsumerTest {

    @RegisterExtension
    static final ValidatedPactConsumerTestExtension provider =
            new ValidatedPactConsumerTestExtension("http://my-provider/api-spec.yaml", null);

    @Pact(provider = "MyProvider", consumer = "MyConsumer")
    public V4Pact getWidget(final PactBuilder builder) {
        return builder
                .usingLegacyDsl()
                .uponReceiving("get a widget")
                .method("GET")
                .path("/widgets/123")
                .willRespondWith()
                .status(200)
                .body(new PactDslJsonBody()
                        .numberValue("id", 123)
                        .stringValue("name", "My Widget"))
                .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "getWidget")
    public void getWidget_returnsWidget(final MockServer mockServer) {
        // make HTTP call to mockServer.getUrl() and assert on response
        get(mockServer.getUrl() + "/widgets/123")
                .then().statusCode(200);
    }
}
```

#### Constructors ####

There are three ways to create a `ValidatedPactConsumerTestExtension`:

**1. From a spec URL or classpath resource** — the simplest option, uses `PactLevelResolverFactory` automatically:

```java
// From a URL
new ValidatedPactConsumerTestExtension("http://my-provider/api-spec.yaml", null);

// From a classpath resource
new ValidatedPactConsumerTestExtension("oai/my-api-spec.yaml", null);

// With a base path override (useful when the spec base path differs from the mock server path)
new ValidatedPactConsumerTestExtension("oai/my-api-spec.yaml", "/api/v1");
```

**2. From a pre-configured `OpenApiInteractionValidator`** — for full control over validation behaviour:

```java
new ValidatedPactConsumerTestExtension(
        OpenApiInteractionValidator
                .createFor("http://my-provider/api-spec.yaml")
                .withLevelResolver(PactLevelResolverFactory.create())
                .withBasePathOverride("/api/v1")
                .build()
);
```

#### Convenience annotation ####

A `@ValidatedPactConsumerTest` meta-annotation is also provided as a shortcut for `@ExtendWith(ValidatedPactConsumerTestExtension.class)`.
Note that when using this annotation you must supply the validator via a `@RegisterExtension` field or configure
the extension in another way, since the annotation does not accept configuration parameters:

```java
@ValidatedPactConsumerTest
@PactTestFor(providerName = "MyProvider")
public class MyConsumerTest {
    // ...
}
```

#### Skipping validation for specific tests ####

If a particular test is expected to fail validation (e.g. you are testing an error case, or building expectations
for an endpoint that does not yet exist in the spec), you can suppress validation for that test using the
`@IgnoreApiValidation` annotation:

```java
@Test
@PactTestFor(pactMethod = "getUnknownEndpoint")
@IgnoreApiValidation
public void getUnknownEndpoint_notYetInSpec(final MockServer mockServer) {
    // validation against the spec is skipped for this test
}
```

#### PactLevelResolverFactory ####

Pact consumer tests define the **minimum** fields a consumer cares about, not necessarily the full response.
As a result, some validation rules that would normally be errors need to be relaxed in the Pact context:

| Message key | Default level | Pact level |
|---|---|---|
| `validation.response.body.schema.required` | `ERROR` | `INFO` |
| `validation.response.body.missing` | `ERROR` | `INFO` |

`PactLevelResolverFactory.create()` returns a `LevelResolver` with these adjustments pre-configured.
When constructing a `ValidatedPactConsumerTestExtension` from a spec URL (constructor 1 above), this resolver
is applied automatically. When using a pre-configured validator (constructor 2), you should apply it explicitly:

```java
OpenApiInteractionValidator
        .createFor("oai/my-api-spec.yaml")
        .withLevelResolver(PactLevelResolverFactory.create())
        .build()
```

#### Required JUnit 5 dependencies ####

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>au.com.dius.pact.consumer</groupId>
    <artifactId>junit5</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Provider-side validation ##

On the provider side, the module can validate consumer pact files against a provider OpenAPI / Swagger spec,
either as part of the provider's test suite or as a standalone CI step.

### PactProviderValidator ###

`PactProviderValidator` validates one or more consumer pact files against a provider OpenAPI / Swagger spec.
It supports pacts from a [Pact Broker](https://docs.pact.io/pact_broker), from the file system, or from
remote URLs.

#### Basic usage ####

```java
final PactProviderValidator validator =
        PactProviderValidator
                .createFor("http://my-provider/api-spec.yaml")
                .withConsumer("MyConsumer", "path/to/MyConsumer-MyProvider.json")
                .build();

final PactProviderValidationResults results = validator.validate();

assertThat(results.hasErrors(), is(false));
```

#### Loading pacts from a Pact Broker ####

```java
final PactProviderValidator validator =
        PactProviderValidator
                .createFor("http://my-provider/api-spec.yaml")
                .withPactsFrom("http://my-pact-broker", "MyProvider")
                .build();
```

#### Loading pacts from a URL ####

```java
final PactProviderValidator validator =
        PactProviderValidator
                .createFor("http://my-provider/api-spec.yaml")
                .withConsumer("MyConsumer", new URL("http://my-pact-broker/pacts/provider/MyProvider/consumer/MyConsumer/latest"))
                .build();
```

#### Using a pre-configured validator ####

```java
final OpenApiInteractionValidator openApiValidator =
        OpenApiInteractionValidator
                .createFor("http://my-provider/api-spec.yaml")
                .withBasePathOverride("/api/v1")
                .build();

final PactProviderValidator validator =
        PactProviderValidator
                .createFor(openApiValidator)
                .withConsumer("MyConsumer", "path/to/MyConsumer-MyProvider.json")
                .build();
```

#### Inspecting results ####

`PactProviderValidationResults` provides detailed results per consumer and per interaction:

```java
final PactProviderValidationResults results = validator.validate();

if (results.hasErrors()) {
    // Get a formatted summary of all failures
    System.out.println(results.getValidationFailureReport());

    // Or inspect results per consumer
    for (final PactProviderValidationResults.ConsumerResult consumerResult : results.getFailedConsumerResults()) {
        System.out.println("Consumer: " + consumerResult.getConsumerName());
        consumerResult.getFailedInteractions().forEach((interaction, report) -> {
            System.out.println("  Interaction: " + interaction);
            report.getMessages().forEach(m -> System.out.println("    [" + m.getLevel() + "] " + m.getMessage()));
        });
    }
}
```

#### Required provider dependency ####

```xml
<dependency>
    <groupId>au.com.dius.pact</groupId>
    <artifactId>provider</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Manual interaction validation ##

You can also validate individual pact interactions directly using the `PactRequest` and `PactResponse` adapters,
without using either the consumer extension or the provider validator:

```java
final SynchronousRequestResponse interaction = ...; // from a V4Pact or RequestResponsePact

final OpenApiInteractionValidator validator =
        OpenApiInteractionValidator
                .createFor("http://my-provider/api-spec.yaml")
                .withLevelResolver(PactLevelResolverFactory.create())
                .build();

final ValidationReport report = validator.validate(
        PactRequest.of(interaction.getRequest()),
        PactResponse.of(interaction.getResponse()));

if (report.hasErrors()) {
    report.getMessages().forEach(m -> System.out.println("[" + m.getLevel() + "] " + m.getMessage()));
}
```

---

## Examples ##

See the [examples module](https://bitbucket.org/atlassian/openapi-request-validator/src/master/openapi-request-validator-examples/?at=master)
for runnable examples of how this module can be used in practice.
