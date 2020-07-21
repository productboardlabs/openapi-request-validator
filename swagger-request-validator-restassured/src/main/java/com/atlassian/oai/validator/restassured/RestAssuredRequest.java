package com.atlassian.oai.validator.restassured;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import io.restassured.specification.FilterableRequestSpecification;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.atlassian.oai.validator.util.ContentTypeUtils.getCharsetFromContentType;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

public class RestAssuredRequest implements Request {

    private static final Logger log = getLogger(RestAssuredRequest.class);

    private final Request delegate;

    /**
     * @deprecated Use: {@link RestAssuredRequest#of(FilterableRequestSpecification)}
     */
    @Deprecated
    public RestAssuredRequest(@Nonnull final FilterableRequestSpecification originalRequest) {
        delegate = RestAssuredRequest.of(originalRequest);
    }

    @Nonnull
    @Override
    public String getPath() {
        return delegate.getPath();
    }

    @Nonnull
    @Override
    public Request.Method getMethod() {
        return delegate.getMethod();
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return delegate.getBody();
    }

    @Nonnull
    @Override
    public Collection<String> getQueryParameters() {
        return delegate.getQueryParameters();
    }

    @Nonnull
    @Override
    public Collection<String> getQueryParameterValues(final String name) {
        return delegate.getQueryParameterValues(name);
    }

    @Nonnull
    @Override
    public Map<String, Collection<String>> getHeaders() {
        return delegate.getHeaders();
    }

    @Nonnull
    @Override
    public Collection<String> getHeaderValues(final String name) {
        return delegate.getHeaderValues(name);
    }

    /**
     * Builds a {@link Request} for the OpenAPI validator out of the
     * original {@link FilterableRequestSpecification}.
     *
     * @param originalRequest the original {@link FilterableRequestSpecification}
     */
    @Nonnull
    public static Request of(@Nonnull final FilterableRequestSpecification originalRequest) {
        requireNonNull(originalRequest, "An original request is required");
        final SimpleRequest.Builder builder = new SimpleRequest.Builder(originalRequest.getMethod(), originalRequest.getDerivedPath());
        builder.withBody(serializeBody(originalRequest));
        if (originalRequest.getHeaders() != null) {
            originalRequest.getHeaders().forEach(header -> builder.withHeader(header.getName(), header.getValue()));
        }
        if (originalRequest.getContentType() != null) {
            builder.withContentType(originalRequest.getContentType());
        }
        // the query params seems wrongly typed - they can contain either a list of strings or a string
        new HashMap<String, Object>(originalRequest.getQueryParams())
                .forEach((key, value) -> {
                    if (value instanceof List) {
                        builder.withQueryParam(key, (List) value);
                    } else if (value instanceof String) {
                        builder.withQueryParam(key, (String) value);
                    }
                });
        if ("GET".equalsIgnoreCase(originalRequest.getMethod())) {
            originalRequest.getRequestParams().forEach(builder::withQueryParam);
        }
        return builder.build();
    }

    private static String serializeBody(final FilterableRequestSpecification originalRequest) {
        final Object body = originalRequest.getBody();
        if (body instanceof String) {
            return (String) body;
        }
        if (body instanceof byte[]) {
            final Charset charset = getCharsetFromContentType(originalRequest.getContentType()).orElse(Charset.defaultCharset());
            return new String((byte[]) body, charset);
        }
        // TODO: Add support for other body types (e.g. InputStream)
        log.warn("Only String and byte[] bodies supported. No request body will be used in validation.");
        return null;
    }
}
