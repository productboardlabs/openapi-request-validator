package com.atlassian.oai.validator.example.simple;

import com.atlassian.oai.validator.springmvc.OpenApiValidationFilter;
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor;
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
public class RestRequestValidationConfig extends WebMvcConfigurerAdapter {

    private final OpenApiValidationInterceptor openApiValidationInterceptor;

    @Autowired
    public RestRequestValidationConfig(@Value("classpath:api-spring-test.json") final Resource swaggerSchema) throws IOException {
        final EncodedResource swaggerResource = new EncodedResource(swaggerSchema, "UTF-8");
        openApiValidationInterceptor = new OpenApiValidationInterceptor(swaggerResource);
    }

    @Bean
    public Filter swaggerValidationFilter() {
        return new OpenApiValidationFilter(true, true);
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(openApiValidationInterceptor);
    }
}
