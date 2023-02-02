# Swagger Request Validator - Spring Web MVC 6+ #

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-spring-webvmvc/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-spring-webmvc)

Integrations between the Swagger Request Validator and the 
[Spring Web MVC framework](https://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html).

This module includes an `OpenApiValidationFilter` and an `OpenApiValidationInterceptor` that can be used to add request
and / or response validation to a REST web service utilizing Spring WebMVC 6 or later, including Spring Boot Starter 
applications utilizing Spring MVC (e.g. `spring-boot-starter-web-services` or `spring-boot-starter-web`).

In case of invalid requests against the REST web service an `InvalidRequestException` is thrown containing the `ValidationReport`.  
In case of invalid responses coming from the REST web service an `InvalidResponseException` is thrown containing 
the `ValidationReport`.

This module is compatible with Spring 6+, Spring Boot 3+ and the Jakarta namespace. For use with older versions of 
Spring see [swagger-request-validator-springmvc](../swagger-request-validator-springmvc/README.md). 

## Usage ##

### Adding the dependency ###

Add this dependency to your project.

e.g. for Maven in your pom.xml:

```xml
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-spring-webmvc</artifactId>
    <version>${swagger-request-validator.version}</version>
</dependency>
```

### Configuration ###

The following property is necessary to receive the error message in case of validation errors:

```properties
server.error.include-message=always
```

If this property is not set the client will receive an `InvalidRequestException` or `InvalidResponseException` without 
knowing what is wrong with the request / response as the `message` field will be missing.

### Adding filter and interceptor ###

Add this configuration to your application.

```java
import com.atlassian.oai.validator.springmvc.OpenApiValidationFilter;
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.Filter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class OpenApiValidationConfig {
    @Bean
    public Filter validationFilter() {
        return new OpenApiValidationFilter(
                true, // enable request validation
                true  // enable response validation
        );
    }

    @Bean
    public WebMvcConfigurer addOpenApiValidationInterceptor(@Value("classpath:api-spring-test.json") final Resource apiSpecification) throws IOException {
        final EncodedResource specResource = new EncodedResource(openApiSpecification, StandardCharsets.UTF_8);
        final OpenApiValidationInterceptor openApiValidationInterceptor = new OpenApiValidationInterceptor(encodedResource);
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(final InterceptorRegistry registry) {
                registry.addInterceptor(openApiValidationInterceptor);
            }
        };
    }
}
```

To get better control over the validation a custom `OpenApiInteractionValidator` can be used.  

```java
    @Bean
    public WebMvcConfigurer addOpenApiValidationInterceptor(@Value("${open.api.spec.url}") final Resource specificationUrl) throws IOException {
        final OpenApiInteractionValidator validator = OpenApiInteractionValidator
                .createForSpecificationUrl(specificationUrl)
                .withLevelResolver(SpringMVCLevelResolverFactory.create())
                .withBasePathOverride("/v1")
                .build();
        final OpenApiValidationInterceptor openApiValidationInterceptor = new OpenApiValidationInterceptor(validator);
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(final InterceptorRegistry registry) {
                registry.addInterceptor(openApiValidationInterceptor);
            }
        };
    }
```

See the [Swagger Request Validator - Core](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-core/README.md?at=master) for more information on customizing the validator.

You might want to add logging for the package: ```com.atlassian.oai.validator.springmvc```

## Example ##

Please see [the tests](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-springmvc6/src/test/java/com/atlassian/oai/validator/example/?at=master) for working examples.

* There is a simple example that shows how to add the Swagger Request Validation adapter.
* An advanced example shows how to additionally add an ExceptionHandler to map the `InvalidRequestException` and `InvalidResponseException` to a custom response.
* Another example shows how to add custom request logging before each validation. A custom `OpenApiInteractionValidator` is used in this example.
* Not much different is the example for async processing.

## Limitations ##

A mapped `Controller` \ `RESTController` method might throw an exception, which will be mapped by Spring to a generic error response. Those error responses will not be validated.
