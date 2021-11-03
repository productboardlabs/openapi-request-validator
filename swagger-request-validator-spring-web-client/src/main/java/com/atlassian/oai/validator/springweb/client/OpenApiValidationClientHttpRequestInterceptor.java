package com.atlassian.oai.validator.springweb.client;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.JsonValidationReportFormat;
import com.atlassian.oai.validator.report.ValidationReport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.atlassian.oai.validator.util.StringUtils.requireNonEmpty;
import static java.util.Objects.requireNonNull;

/**
 * A {@link ClientHttpRequestInterceptor} that performs OpenAPI / Swagger validation on a request/response interaction.
 * <p>
 * To use, simply add it as a interceptor to your {@link org.springframework.web.client.RestTemplate}
 * <pre>
 *     private final OpenApiValidationClientHttpRequestInterceptor validationInterceptor = new OpenApiValidationClientHttpRequestInterceptor(SPEC_URL);
 *     ...
 *     RestTemplate restTemplate = new RestTemplate();
 *     restTemplate.setInterceptors(Collections.singletonList(validationInterceptor));
 * </pre>
 * <p>
 * If validation fails, a {@link OpenApiValidationException} will be thrown describing the validation failure.
 * @since 2.1
 */
public class OpenApiValidationClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    private final OpenApiInteractionValidator validator;

    public OpenApiValidationClientHttpRequestInterceptor(final String specUrlOrDefinition) {
        requireNonEmpty(specUrlOrDefinition, "A spec is required");

        validator = OpenApiInteractionValidator.createFor(specUrlOrDefinition).build();
    }

    public OpenApiValidationClientHttpRequestInterceptor(final OpenApiInteractionValidator validator) {
        requireNonNull(validator, "A validator is required");

        this.validator = validator;
    }

    @Override
    public ClientHttpResponse intercept(final HttpRequest request, final byte[] body, final ClientHttpRequestExecution execution) throws IOException {
        final ClientHttpResponse response = new BufferingClientHttpResponse(execution.execute(request, body));
        final ValidationReport validationReport =
                validator.validate(fromHttpRequest(request, body), fromClientHttpResponse(response));

        if (validationReport.hasErrors()) {
            throw new OpenApiValidationException(validationReport);
        }
        return response;
    }

    @Nonnull
    private static Request fromHttpRequest(@Nonnull final HttpRequest originalRequest, @Nullable final byte[] body) {
        requireNonNull(originalRequest, "An original request is required");
        final UriComponents uriComponents = UriComponentsBuilder.fromUri(originalRequest.getURI()).build();

        final SimpleRequest.Builder builder =
                new SimpleRequest.Builder(fromHttpMethod(originalRequest.getMethod()), uriComponents.getPath())
                        .withBody(body);
        originalRequest.getHeaders().forEach(builder::withHeader);
        uriComponents.getQueryParams().forEach(builder::withQueryParam);

        return builder.build();
    }

    @Nonnull
    private static Response fromClientHttpResponse(@Nonnull final ClientHttpResponse originalResponse) throws IOException {
        requireNonNull(originalResponse, "An original response is required");

        final SimpleResponse.Builder builder = new SimpleResponse.Builder(originalResponse.getStatusCode().value())
                .withBody(originalResponse.getBody());
        originalResponse.getHeaders().forEach(builder::withHeader);

        return builder.build();
    }

    private static Request.Method fromHttpMethod(final HttpMethod method) {
        return Request.Method.valueOf(method.name());
    }

    /**
     * A {@link RestClientException} which indicates that the request or response does
     * not conform to the swagger spec
     *
     * @since 2.1
     */
    public static class OpenApiValidationException extends RestClientException {
        private final ValidationReport report;

        public OpenApiValidationException(@Nonnull final ValidationReport report) {
            super(JsonValidationReportFormat.getInstance().apply(requireNonNull(report, "ValidationReport is required")));
            this.report = report;
        }

        /**
         * @return The validation report that generating this exception
         */
        public ValidationReport getValidationReport() {
            return report;
        }
    }

    /**
     * An implementation of {@link ClientHttpResponse} which buffers the body for reuse.
     * <p>
     * This is similar to {@code org.springframework.http.client.BufferingClientHttpRequestWrapper} but
     * isn't package-private and doesn't require the consumer to configure a custom request factory
     */
    private static class BufferingClientHttpResponse extends AbstractClientHttpResponse {

        private final ClientHttpResponse delegate;
        @Nullable
        private byte[] body;

        private BufferingClientHttpResponse(final ClientHttpResponse delegate) {
            this.delegate = requireNonNull(delegate, "delegate ClientHttpResponse is required");
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return delegate.getRawStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public void close() {

        }

        @Override
        public InputStream getBody() throws IOException {
            if (body == null) {
                try (InputStream in = delegate.getBody()) {
                    body = StreamUtils.copyToByteArray(in);
                }
            }
            return new ByteArrayInputStream(body);
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }
    }

}
