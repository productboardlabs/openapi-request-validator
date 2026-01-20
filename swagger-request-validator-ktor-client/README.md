# Swagger Request Validator - Ktor Client

OpenAPI / Swagger validation plugin for Ktor Client requests and responses.

## Compatibility

- **Ktor**: 3.0.x
- **Kotlin**: 2.0.x

## Installation

### Gradle (Kotlin DSL)

```kotlin
implementation("com.atlassian.oai:swagger-request-validator-ktor-client:3.0.0-SNAPSHOT")
```

### Maven

```xml
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-ktor-client</artifactId>
    <version>3.0.0-SNAPSHOT</version>
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
        reportFormat = SimpleValidationReportFormat.getInstance() // this is the default
        validator { builder ->
            builder.withApiSpecification("https://api.example.com/openapi.json")
        }
    }
}
```

The plugin will automatically validate all outgoing requests and responses against your OpenAPI specification and throw exceptions if validation fails.