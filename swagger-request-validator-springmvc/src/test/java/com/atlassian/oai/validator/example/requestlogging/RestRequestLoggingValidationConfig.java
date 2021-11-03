package com.atlassian.oai.validator.example.requestlogging;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.springmvc.OpenApiValidationFilter;
import com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor;
import com.atlassian.oai.validator.springmvc.ResettableRequestServletWrapper;
import com.atlassian.oai.validator.springmvc.SpringMVCLevelResolverFactory;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class RestRequestLoggingValidationConfig {
    @Bean
    public Filter openApiValidationFilter() {
        return new OpenApiValidationFilter(true, true);
    }

    @Bean
    public WebMvcConfigurer addOpenApiValidationInterceptor(@Value("classpath:api-spring-test.json") final Resource apiSpecification) throws IOException {
        final OpenApiInteractionValidator openApiInteractionValidator = OpenApiInteractionValidator
                .createForSpecificationUrl(apiSpecification.getURL().toExternalForm())
                .withLevelResolver(SpringMVCLevelResolverFactory.create())
                // the context path of the Spring application differs from the base path in the OpenAPI schema
                .withBasePathOverride("/v1")
                .build();
        final OpenApiValidationInterceptor openApiValidationInterceptor = new OpenApiValidationInterceptor(openApiInteractionValidator);
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(final InterceptorRegistry registry) {
                registry.addInterceptor(new RequestLoggingInterceptor());
                registry.addInterceptor(openApiValidationInterceptor);
            }
        };
    }

    static class RequestLoggingInterceptor implements HandlerInterceptor {

        private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

        @Override
        public boolean preHandle(final HttpServletRequest servletRequest, final HttpServletResponse servletResponse,
                                 final Object handler) throws Exception {
            final String requestLog = String.join(System.lineSeparator(),
                    "uri=" + servletRequest.getRequestURI() + "?" + servletRequest.getQueryString(),
                    "client=" + servletRequest.getRemoteAddr(),
                    "payload=" + getPayload(servletRequest)
            );
            LOG.info("Incoming request: {}", requestLog);
            return true;
        }

        private String getPayload(final HttpServletRequest request) throws IOException {
            if (request instanceof ResettableRequestServletWrapper) {
                final ResettableRequestServletWrapper resettableRequest = (ResettableRequestServletWrapper) request;
                final char[] chunk = new char[1024]; // only log the first 1kB
                IOUtils.read(request.getReader(), chunk);
                // reset the input stream - so it can be read again by the next interceptor / filter
                resettableRequest.resetInputStream();
                return new String(chunk);
            } else {
                return "[unknown]";
            }
        }
    }
}
