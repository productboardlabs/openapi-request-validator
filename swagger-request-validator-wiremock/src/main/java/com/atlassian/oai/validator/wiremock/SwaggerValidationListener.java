package com.atlassian.oai.validator.wiremock;

import com.atlassian.oai.validator.report.SimpleValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.RequestListener;
import com.github.tomakehurst.wiremock.http.Response;

/**
 * @deprecated Replaced with {@link OpenApiValidationListener}
 */
@Deprecated
public class SwaggerValidationListener implements RequestListener {

    private final OpenApiValidationListener delegate;

    public SwaggerValidationListener(final String specUrlOrPayload) {
        delegate = new OpenApiValidationListener(specUrlOrPayload);
    }

    @Override
    public void requestReceived(final Request request, final Response response) {
        delegate.requestReceived(request, response);
    }

    public ValidationReport getReport() {
        return delegate.getReport();
    }

    public void reset() {
        delegate.reset();
    }

    public void assertValidationPassed() {
        try {
            delegate.assertValidationPassed();
        } catch (final OpenApiValidationListener.OpenApiValidationException e) {
            throw new SwaggerValidationException(e.getValidationReport());
        }
    }

    /**
     * @deprecated Replaced with {@link OpenApiValidationListener.OpenApiValidationException}
     */
    @Deprecated
    public static class SwaggerValidationException extends RuntimeException {

        private final ValidationReport report;

        public SwaggerValidationException(final ValidationReport report) {
            super(SimpleValidationReportFormat.getInstance().apply(report));
            this.report = report;
        }

        public ValidationReport getValidationReport() {
            return report;
        }
    }
}
