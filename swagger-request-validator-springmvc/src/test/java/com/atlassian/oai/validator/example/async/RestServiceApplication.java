package com.atlassian.oai.validator.example.async;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
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
