# OpenAPI Request Validator - Spring Web Client #

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/openapi-request-validator-spring-web-client/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/openapi-request-validator-spring-web-client)

Integration between the OpenAPI Request Validator and the [Spring Web](https://docs.spring.io/spring-framework/docs/current/reference/html/web.html) `RestTemplate` / `RestClient`.

This module provides a `ClientHttpRequestInterceptor` that automatically validates outgoing requests and incoming responses against an OpenAPI / Swagger specification as they pass through a Spring `RestTemplate` (or any client using the `ClientHttpRequestInterceptor` interface).

If validation fails, an `OpenApiValidationException` (a subclass of `RestClientException`) is thrown containing the full `ValidationReport`.

## Requirements ##

- Java 17+
- Spring Framework 6+ (`spring-web`)

## Usage ##

### Adding the dependency ###

```xml
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>openapi-request-validator-spring-web-client</artifactId>
    <version>${openapi-request-validator.version}</version>
</dependency>
```

### Basic setup ###

Create an interceptor with your spec URL (HTTP, classpath, or file) and add it to a `RestTemplate`:

```java
import com.atlassian.oai.validator.springweb.client.OpenApiValidationClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

private static final String SPEC_URL = "classpath:openapi.yaml";

RestTemplate restTemplate = new RestTemplate();
restTemplate.setInterceptors(List.of(
    new OpenApiValidationClientHttpRequestInterceptor(SPEC_URL)
));
```

All requests and responses made through this `RestTemplate` will now be validated automatically.

### Advanced setup with a custom validator ###

For fine-grained control (whitelisting, custom level resolvers, base path overrides, etc.), supply a pre-configured `OpenApiInteractionValidator`:

```java
import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.springweb.client.OpenApiValidationClientHttpRequestInterceptor;

OpenApiInteractionValidator validator = OpenApiInteractionValidator
        .createForSpecificationUrl(SPEC_URL)
        .withBasePathOverride("/api/v1")
        .withLevelResolver(
            LevelResolver.create()
                .withLevel("validation.response.body.missing", ValidationReport.Level.WARN)
                .build()
        )
        .build();

RestTemplate restTemplate = new RestTemplate();
restTemplate.setInterceptors(List.of(
    new OpenApiValidationClientHttpRequestInterceptor(validator)
));
```

See the [core module README](../openapi-request-validator-core/README.md) for full details on configuring the validator.

### Handling validation failures ###

The interceptor throws `OpenApiValidationClientHttpRequestInterceptor.OpenApiValidationException` when validation fails. This extends `RestClientException`, so it fits naturally into Spring's exception handling:

```java
try {
    restTemplate.getForObject("/pets/1", Pet.class);
} catch (OpenApiValidationClientHttpRequestInterceptor.OpenApiValidationException e) {
    ValidationReport report = e.getValidationReport();
    // inspect or log the report
}
```

## How it works ##

The interceptor wraps the response body in a buffering wrapper so that the body can be read once for validation and then again by the caller. This avoids the need to configure a custom `BufferingClientHttpRequestFactory` on the `RestTemplate`.

## Examples ##

See the [examples module](../openapi-request-validator-examples/) for a runnable example of how this module can be used.
