# OpenAPI Request Validator — Features

## Specification Support

* [Swagger v2 (OpenAPI 2.0)](SWAGGERv2.md) — full request and response validation
* [OpenAPI v3.0](OPENAPIv3.md) — full request and response validation
* [OpenAPI v3.1](OPENAPIv3.md) — partial support (see notes in the OpenAPI v3 doc)
* YAML and JSON spec formats
* Specs loaded from:
  * HTTP / HTTPS URLs
  * Classpath resources
  * Local file paths
  * Inline string payloads

## Core Validation

* **Request validation**
  * Path and HTTP method matching
  * Path, query, header, and cookie parameter validation (type, format, constraints)
  * Request body validation (JSON Schema, content-type negotiation)
  * Required parameter and body enforcement
  * Security requirement presence checks (`apiKey`, `http` type schemes)
* **Response validation**
  * HTTP status code validation
  * Response header validation
  * Response body validation (JSON Schema, content-type negotiation)
* **Webhook validation** (OpenAPI v3.1)
  * `validateWebhookRequest()`, `validateWebhookResponse()`, `validateWebhook()`
* **JSON Schema validation** — full draft-07 support including `$ref`, `allOf`, `oneOf`, `anyOf`, composition, discriminators, and all standard keywords

## Standalone & Framework-Agnostic

The core library has no dependency on any HTTP framework or testing library. It works with plain `Request` and `Response` objects, making it easy to integrate anywhere.

## Framework Adapters

| Adapter | Description | Artifact |
|---|---|---|
| [Spring MockMVC](../openapi-request-validator-mockmvc/README.md) | `ResultMatcher` for Spring MockMVC test interactions (Spring 7 / Spring Boot 4+) | `openapi-request-validator-mockmvc` |
| [Spring WebMVC](../openapi-request-validator-spring-webmvc/README.md) | Servlet `Filter` + MVC `Interceptor` for runtime production validation | `openapi-request-validator-spring-webmvc` |
| [Spring Web Client](../openapi-request-validator-spring-web-client/pom.xml) | `ClientHttpRequestInterceptor` for Spring `RestTemplate` / `RestClient` | `openapi-request-validator-spring-web-client` |
| [REST Assured](../openapi-request-validator-restassured/README.md) | `OpenApiValidationFilter` for REST Assured test interactions | `openapi-request-validator-restassured` |
| [WireMock (JUnit 5)](../openapi-request-validator-wiremock-junit5/README.md) | `OpenApiValidationListener` (`ServeEventListener`) for WireMock stubs | `openapi-request-validator-wiremock-junit5` |
| [Pact](../openapi-request-validator-pact/README.md) | Consumer- and provider-side validation against Pact files | `openapi-request-validator-pact` |
| [Ktor Client](../openapi-request-validator-ktor-client/README.md) | `OpenAPIValidation` plugin for Ktor 3.x HTTP client (Kotlin) | `openapi-request-validator-ktor-client` |

## Fine-Grained Validation Control

See the [core module README](../openapi-request-validator-core/README.md) for full details.

### Message-Level Control

Validation messages have a configurable severity level (`ERROR`, `WARN`, `INFO`, `IGNORED`). Levels can be adjusted globally or per message key using a `LevelResolver`:

```java
OpenApiInteractionValidator.createForSpecificationUrl(specUrl)
    .withLevelResolver(
        LevelResolver.create()
            .withLevel("validation.request.body.missing", ValidationReport.Level.WARN)
            .build()
    )
    .build();
```

Framework-specific level resolver factories are also available (e.g. `SpringMVCLevelResolverFactory`).

### Whitelisting / Error Suppression

A `ValidationErrorsWhitelist` lets you suppress specific validation errors that are expected or acceptable in your context. Rules can be composed with `allOf` / `anyOf` logic:

```java
ValidationErrorsWhitelist whitelist = ValidationErrorsWhitelist.create()
    .withRule("Ignore 404 body errors",
        allOf(isResponse(), responseStatusIs(404), messageHasKey("validation.response.body.missing")))
    .withRule("Ignore OPTIONS requests",
        allOf(isRequest(), methodIs(HttpMethod.OPTIONS)));
```

Available built-in rules (`WhitelistRules`):

| Rule | Description |
|---|---|
| `messageHasKey(key)` | Match by validation message key |
| `messageContainsSubstring(s)` | Match if message text contains a substring |
| `messageContainsRegexp(re)` | Match if message text matches a regexp |
| `pathContainsSubstring(s)` | Match if request path contains a substring |
| `pathContainsRegexp(re)` | Match if request path matches a regexp |
| `isRequest()` / `isResponse()` | Match request-only or response-only messages |
| `methodIs(method)` | Match by HTTP method |
| `responseStatusIs(code)` | Match by HTTP response status code |
| `responseStatusTypeIs(type)` | Match by response status type (1xx, 2xx, …) |
| `entityIs(name)` | Match by entity/schema name |
| `headerContainsSubstring(header, s)` | Match if a header contains a substring |
| `headerContainsRegexp(header, re)` | Match if a header matches a regexp |
| `missingRequestContentType()` | Match missing `Content-Type` header errors |
| `allOf(rules…)` | All rules must match (AND composition) |
| `anyOf(rules…)` | Any rule must match (OR composition) |

### Custom Validators

Plug in your own validation logic alongside the built-in validators:

```java
OpenApiInteractionValidator.createForSpecificationUrl(specUrl)
    .withCustomRequestValidation(myCustomRequestValidator)
    .withCustomResponseValidation(myCustomResponseValidator)
    .build();
```

## Builder Configuration Options

The `OpenApiInteractionValidator.Builder` exposes the following options:

| Method | Description |
|---|---|
| `withInlineApiSpecification(spec)` | Load spec from an inline string |
| `withApiSpecificationUrl(url)` | Load spec from a URL (HTTP, classpath, file) |
| `withApi(OpenAPI)` | Use a pre-parsed `OpenAPI` model object |
| `withBasePathOverride(path)` | Override the base path from the spec |
| `withLevelResolver(resolver)` | Configure message severity levels |
| `withWhitelist(whitelist)` | Suppress specific validation errors |
| `withAuthHeaderData(key, value)` | Provide auth credentials for spec fetching |
| `withCustomRequestValidation(v)` | Add a custom request validator |
| `withCustomResponseValidation(v)` | Add a custom response validator |
| `withResolveRefs(bool)` | Control `$ref` resolution (default: `true`) |
| `withResolveCombinators(bool)` | Control combinator resolution (default: `true`) |
| `withParseOptions(opts)` | Pass custom `ParseOptions` to the OpenAPI parser |
| `withSchemaValidationConfiguration(cfg)` | Configure JSON Schema validation cache size |
| `withStrictOperationPathMatching()` | Require exact path matching (no trailing slash tolerance) |

## Validation Reports

* `ValidationReport` — immutable, thread-safe collection of validation messages
* Each `Message` contains:
  * A stable message **key** (e.g. `validation.request.body.schema.additionalProperties`)
  * A human-readable **message** string
  * A severity **level** (`ERROR`, `WARN`, `INFO`, `IGNORED`)
  * Rich **context**: operation, path, method, parameter, content-type, response status, JSON pointer location
* Reports can be **merged**: `MergedValidationReport`
* Report formatting:
  * `SimpleValidationReportFormat` — human-readable text for logging
  * `JsonValidationReportFormat` — machine-readable JSON

## JSON Schema Validation

* Full JSON Schema draft-07 support via the `SchemaValidator`
* Schema cache with configurable maximum size (`ValidationConfiguration.setMaxCacheSize()`)
* Built-in format validators: `int32`, `int64`, `float`, `double`, `byte` (Base64)
* Schema transformation pipeline applied before validation:
  * `AdditionalPropertiesInjectionTransformer` — prevents spurious `additionalProperties` errors
  * `DiscriminatorMappingTransformer` — resolves discriminator mappings
  * `RequiredFieldTransformer` — handles `readOnly`/`writeOnly` fields in request vs response context
  * `SchemaDefinitionsInjectionTransformer` — injects global definitions into schemas

## Request/Response Body Handling

Multiple body representations are supported to accommodate different integration patterns:

| Type | Description |
|---|---|
| `StringBody` | In-memory string body |
| `ByteArrayBody` | In-memory byte array body |
| `InputStreamBody` | Streaming body (read once) |
| `ResettableInputStreamBody` | Buffered/resettable stream (Spring WebMVC) |

## FAQ

See the [FAQ](FAQ.md) for answers to common questions about configuration, known limitations, and integration patterns.
