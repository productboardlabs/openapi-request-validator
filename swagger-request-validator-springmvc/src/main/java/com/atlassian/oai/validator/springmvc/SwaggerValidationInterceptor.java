package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @deprecated Replaced with {@link  OpenApiValidationInterceptor}
 */
@Deprecated
public class SwaggerValidationInterceptor extends HandlerInterceptorAdapter {

    private final OpenApiValidationInterceptor delegate;

    public SwaggerValidationInterceptor(final EncodedResource specUrlOrPayload) throws IOException {
        delegate = new OpenApiValidationInterceptor(specUrlOrPayload);
    }

    public SwaggerValidationInterceptor(final SwaggerRequestResponseValidator validator) {
        delegate = new OpenApiValidationInterceptor(validator.getValidator());
    }

    SwaggerValidationInterceptor(final OpenApiValidationService validationService) {
        delegate = new OpenApiValidationInterceptor(validationService);
    }

    @Override
    public boolean preHandle(final HttpServletRequest servletRequest,
                             final HttpServletResponse servletResponse,
                             final Object handler) throws Exception {
        return delegate.preHandle(servletRequest, servletResponse, handler);
    }

    @Override
    public void postHandle(final HttpServletRequest servletRequest,
                           final HttpServletResponse servletResponse,
                           final Object handler,
                           final ModelAndView modelAndView) throws Exception {
        delegate.postHandle(servletRequest, servletResponse, handler, modelAndView);
    }
}
