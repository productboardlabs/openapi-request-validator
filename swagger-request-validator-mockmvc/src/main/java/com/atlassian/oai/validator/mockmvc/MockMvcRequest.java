package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleRequest;
import org.slf4j.Logger;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.list;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

public class MockMvcRequest implements Request {

    private static final Logger LOGGER = getLogger(MockMvcRequest.class);

    private final Request delegate;

    /**
     * @deprecated Use: {@link MockMvcRequest#of(MockHttpServletRequest)}
     */
    @Deprecated
    public MockMvcRequest(@Nonnull final MockHttpServletRequest originalRequest) {
        this.delegate = MockMvcRequest.of(originalRequest);
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
     * Builds a {@link Request} for the swagger validator out of the
     * original {@link MockHttpServletRequest}.
     *
     * @param originalRequest the original {@link MockHttpServletRequest}
     */
    @Nonnull
    public static Request of(@Nonnull final MockHttpServletRequest originalRequest) {
        requireNonNull(originalRequest, "An original request is required");
        final SimpleRequest.Builder builder =
                new SimpleRequest.Builder(originalRequest.getMethod(), originalRequest.getPathInfo())
                        .withBody(getBody(originalRequest));
        list(originalRequest.getHeaderNames())
                .forEach(header -> builder.withHeader(header, list(originalRequest.getHeaders(header))));
        originalRequest.getParameterMap().forEach((key, value) -> builder.withQueryParam(key, value));
        return builder.build();
    }

    private static String getBody(@Nonnull final MockHttpServletRequest mockHttpServletRequest) {
        try (BufferedReader reader = mockHttpServletRequest.getReader()) {
            final StringBuilder builder = new StringBuilder();
            String aux;
            int lineCount = 0;
            while ((aux = reader.readLine()) != null) {
                builder.append(aux);
                lineCount++;
            }
            if (lineCount > 0) {
                return builder.toString();
            }
        } catch (final IOException e) {
            LOGGER.warn("Can't read request body.", e);
        }
        return null;
    }
}
