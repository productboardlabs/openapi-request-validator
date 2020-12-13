package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.list;
import static java.util.Objects.requireNonNull;

public class MockMvcRequest implements Request {

    private final Request delegate;

    /**
     * @deprecated Use: {@link MockMvcRequest#of(MockHttpServletRequest)}
     */
    @Deprecated
    public MockMvcRequest(@Nonnull final MockHttpServletRequest originalRequest) {
        delegate = MockMvcRequest.of(originalRequest);
    }

    @Nonnull
    @Override
    public String getPath() {
        return delegate.getPath();
    }

    @Nonnull
    @Override
    public Method getMethod() {
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
     * original {@link MockHttpServletRequest}.
     *
     * @param originalRequest the original {@link MockHttpServletRequest}
     */
    @Nonnull
    public static Request of(@Nonnull final MockHttpServletRequest originalRequest) {
        requireNonNull(originalRequest, "An original request is required");
        final SimpleRequest.Builder builder =
                new SimpleRequest.Builder(originalRequest.getMethod(), originalRequest.getPathInfo())
                        .withBody(originalRequest.getInputStream());
        list(originalRequest.getHeaderNames())
                .forEach(header -> builder.withHeader(header, list(originalRequest.getHeaders(header))));
        originalRequest.getParameterMap().forEach((key, value) -> builder.withQueryParam(key, value));
        return builder.build();
    }
}
