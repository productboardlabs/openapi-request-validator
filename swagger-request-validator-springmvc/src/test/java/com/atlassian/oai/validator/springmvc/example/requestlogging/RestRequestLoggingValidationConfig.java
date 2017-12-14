package com.atlassian.oai.validator.springmvc.example.requestlogging;

import com.atlassian.oai.validator.springmvc.ResettableRequestServletWrapper;
import com.atlassian.oai.validator.springmvc.SwaggerValidationFilter;
import com.atlassian.oai.validator.springmvc.SwaggerValidationInterceptor;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.Filter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class RestRequestLoggingValidationConfig extends WebMvcConfigurerAdapter {

    private final SwaggerValidationInterceptor swaggerValidationInterceptor;

    @Autowired
    public RestRequestLoggingValidationConfig(@Value("classpath:api-spring-test.json") final Resource swaggerSchema) throws IOException {
        final EncodedResource swaggerResource = new EncodedResource(swaggerSchema, "UTF-8");
        this.swaggerValidationInterceptor = new SwaggerValidationInterceptor(swaggerResource);
    }

    @Bean
    public Filter swaggerValidationFilter() {
        return new SwaggerValidationFilter();
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        // add the logging interceptor before the Swagger validation
        registry.addInterceptor(new RequestLoggingInterceptor());
        registry.addInterceptor(swaggerValidationInterceptor);
    }

    private static class RequestLoggingInterceptor extends HandlerInterceptorAdapter {

        private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingInterceptor.class);

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
                final String requestBody = IOUtils.toString(resettableRequest.getReader());
                // reset the input stream - so it can be read again by the next interceptor / filter
                resettableRequest.resetInputStream();
                return requestBody.substring(0, Math.min(1024, requestBody.length())); // only log the first 1kB
            } else {
                return "[unknown]";
            }
        }
    }
}
