package com.atlassian.oai.validator.springmvc;

import org.springframework.web.util.WebUtils;

import javax.servlet.http.HttpServletResponse;

class ResponseUtils {
    private ResponseUtils() { }

    /**
     * With async processing at play, the response wrapper does not have to be at the top and we
     * do not want to use multiple ContentCachingResponseWrappers. We have to find our wrapper in the wrapper chain.
     */
    static OpenApiValidationContentCachingResponseWrapper getCachingResponse(final HttpServletResponse responseToUse) {
        return WebUtils.getNativeResponse(responseToUse, OpenApiValidationContentCachingResponseWrapper.class);
    }
}
