# Swagger Request Validator - Spring MVC #

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-springmvc/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-springmvc)

Integrations between the Swagger Request Validator with the [Spring Web MVC framework](https://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html).

This module includes a `SwaggerValidationFilter` and a `SwaggerValidationInterceptor` that can be used to add request validation to a REST web service utilizing Spring MVC v4.1.3 or later. Including Spring Boot Starter applications utilizing Spring MVC with said version, e.g. spring-boot-starter-web-services or spring-boot-starter-web.

In case of invalid requests against the REST web service a `InvalidRequestException` is thrown containing the `ValidationReport`.

## Usage ##

Add this dependency to your project.

e.g. for Maven in your pom.xml:

```xml
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-spring</artifactId>
    <version>${swagger-request-validator.version}</version>
</dependency>
```

Add this interceptor to your application.

```java
import com.atlassian.oai.validator.springmvc.SwaggerValidationFilter;
import com.atlassian.oai.validator.springmvc.SwaggerValidationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.Filter;
import java.io.IOException;

@Configuration
public class SwaggerRequestValidationConfig extends WebMvcConfigurerAdapter {

    private final SwaggerValidationInterceptor swaggerValidationInterceptor;

    /**
     * @param swaggerApi the {@link Resource} to your Swagger schema
     */
    @Autowired
    public SwaggerRequestValidationConfig(@Value("classpath:swagger-api.json") final Resource swaggerApi) throws IOException {
        final EncodedResource swaggerResource = new EncodedResource(swaggerSchema, "UTF-8");
        this.swaggerValidationInterceptor = new SwaggerValidationInterceptor(swaggerApi);
    }

    @Bean
    public Filter swaggerValidationFilter() {
        return new SwaggerValidationFilter();
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(swaggerValidationInterceptor);
    }
}
```

You might want to add logging for the package: ```com.atlassian.oai.validator.springmvc```

## Example ##

Please see [the tests](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-springmvc/src/test/java/com/atlassian/oai/validator/springmvc/example/?at=master) for working examples.

* There is a simple example that shows how to add the Swagger Request Validation adapter.
* An advanced example shows how to additionally add an ExceptionHandler to map the `InvalidRequestException` to a custom response.

## Caveats ##

Asynchronous requests are not supported. As well as requests with a content larger then 2GB, more specifically longer than 2147483647 bytes.

## No caveats ##

Although the request is validated against the Swagger schema the response is not. Please ensure valid responses with tests and not during runtime.