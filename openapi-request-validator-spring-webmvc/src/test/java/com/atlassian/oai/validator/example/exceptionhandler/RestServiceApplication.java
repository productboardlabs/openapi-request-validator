package com.atlassian.oai.validator.example.exceptionhandler;

import com.atlassian.oai.validator.example.simple.RestServiceController;
import com.atlassian.oai.validator.springmvc.OpenApiValidationFilter;
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.Filter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class RestServiceApplication {
    public static void main(final String[] args) {
        SpringApplication.run(RestServiceApplication.class, args);
    }

    @Bean
    public Filter openApiValidationFilter() {
        return new OpenApiValidationFilter(true, true);
    }

    @Bean
    public WebMvcConfigurer addOpenApiValidationInterceptor(@Value("classpath:api-spring-test.json") final Resource apiSpecification) throws IOException {
        final EncodedResource specResource = new EncodedResource(apiSpecification, StandardCharsets.UTF_8);
        final OpenApiValidationInterceptor openApiValidationInterceptor = new OpenApiValidationInterceptor(specResource);
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(final InterceptorRegistry registry) {
                registry.addInterceptor(openApiValidationInterceptor);
            }
        };
    }

    @Bean // reuse the controller of the simple example
    public RestServiceController restServiceController() {
        return new RestServiceController();
    }
}
