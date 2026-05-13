# OpenAPI Request Validator - Ktor Client

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/openapi-request-validator-ktor-client/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/openapi-request-validator-ktor-client)

OpenAPI / Swagger validation plugin for Ktor Client requests and responses.

## Compatibility

- **Ktor**: 3.0.x
- **Kotlin**: 2.0.x

## Installation

### Gradle (Kotlin DSL)

```kotlin
implementation("com.atlassian.oai:openapi-request-validator-ktor-client:${openapi-request-validator.version}")
```

### Maven

```xml
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>openapi-request-validator-ktor-client</artifactId>
    <version>${openapi-request-validator.version}</version>
</dependency>
```

## Usage

Install the `OpenAPIValidation` plugin on your Ktor client and configure it with your OpenAPI specification:

```kotlin
import com.atlassian.oai.validator.ktor.client.OpenAPIValidation
import com.atlassian.oai.validator.report.SimpleValidationReportFormat
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java

val client = HttpClient(Java) {
    install(OpenAPIValidation) {
        validator {
            withApiSpecification("https://api.example.com/openapi.json")
        }
        reportFormat = SimpleValidationReportFormat.getInstance() // this is the default
        disableReplayableOutgoingContentMapping = false
    }
}
```

The plugin will automatically validate all outgoing requests and responses against your OpenAPI specification and throw exceptions if validation fails.

## Examples ##

See the [examples module](../openapi-request-validator-examples/README.md) for runnable examples.