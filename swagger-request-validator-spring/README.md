# Swagger Request Validator - Spring #

Integrating the Swagger Request Validator within a Spring REST web service.

## Usage ##

Add the dependency to your project.

e.g. for Maven in your pom.xml:

```xml
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-spring</artifactId>
    <version>${swagger-request-validator.version}</version>
</dependency>
```

Add the interceptor to your Spring REST / web application.

```java
import java.io.IOException;

import javax.servlet.Filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.atlassian.oai.validator.spring.SwaggerValidationFilter;
import com.atlassian.oai.validator.spring.SwaggerValidationInterceptor;

@Configuration
public class SwaggerRequestValidationConfig extends WebMvcConfigurerAdapter {

    private final SwaggerValidationInterceptor swaggerValidationInterceptor;

    @Autowired
    public SwaggerRequestValidationConfig(@Value("classpath:swagger-api.json") final Resource swaggerInterface) throws IOException {
        this.swaggerValidationInterceptor = new SwaggerValidationInterceptor(swaggerInterface);
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

You might want to add logging for the package: ```com.atlassian.oai.validator.spring```

Please see the TestApplication in the test folder of this module for a running example.

## Caveats ##

Asynchronous requests are not supported. As well as requests with a content longer then 2GB, more specifically longer than ```Integer.MAX_VALUE``` bytes.