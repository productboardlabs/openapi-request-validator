package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @deprecated Replaced with {@link OpenApiValidationService}
 */
@Deprecated
public class SwaggerRequestValidationService {

    private final OpenApiValidationService delegate;

    SwaggerRequestValidationService(final EncodedResource specUrlOrPayload) throws IOException {
        delegate = new OpenApiValidationService(specUrlOrPayload, new UrlPathHelper());
    }

    SwaggerRequestValidationService(final SwaggerRequestResponseValidator validator) {
        delegate = new OpenApiValidationService(validator.getValidator(), new UrlPathHelper());
    }

    public ValidationReport validateRequest(final Request request) {
        return delegate.validateRequest(request);
    }

    public ValidationReport validateResponse(final HttpServletRequest servletRequest,
                                             final Response response) {
        return delegate.validateResponse(servletRequest, response);
    }
}
