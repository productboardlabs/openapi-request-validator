package com.atlassian.oai.validator.example.async;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;

@SpringBootApplication
public class RestServiceApplication {

    public static void main(final String[] args) {
        SpringApplication.run(RestServiceApplication.class, args);
    }

    @Bean
    public Filter wrapperFilter() {
        return new WrapperFilter();
    }

    /**
     * Simulates Spring security which wraps response every time FilterChainProxy is called.
     */
    private static class WrapperFilter implements Filter, Ordered {

        @Override
        public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain filterChain) throws IOException, ServletException {
            filterChain.doFilter(request, new HttpServletResponseWrapper((HttpServletResponse) response));
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public void init(final FilterConfig filterConfig) {
        }

        @Override
        public void destroy() {
        }
    }
}
