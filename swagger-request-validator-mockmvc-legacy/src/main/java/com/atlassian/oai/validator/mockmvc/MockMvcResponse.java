package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class MockMvcResponse implements Response {

    private final Response delegate;

    /**
     * @deprecated Use: {@link MockMvcRequest#of(MockHttpServletRequest)}
     */
    @Deprecated
    public MockMvcResponse(@Nonnull final MockHttpServletResponse originalResponse) {
        delegate = MockMvcResponse.of(originalResponse);
    }

    @Override
    public int getStatus() {
        return delegate.getStatus();
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return delegate.getBody();
    }

    @Nonnull
    @Override
    public Collection<String> getHeaderValues(final String name) {
        return delegate.getHeaderValues(name);
    }

    /**
     * Builds a {@link Response} for the OpenAPI validator out of the
     * original {@link MockHttpServletResponse}.
     *
     * @param originalResponse the original {@link MockHttpServletResponse}
     */
    @Nonnull
    public static Response of(@Nonnull final MockHttpServletResponse originalResponse) {
        requireNonNull(originalResponse, "An original response is required");
        final SimpleResponse.Builder builder = new SimpleResponse.Builder(originalResponse.getStatus())
                .withBody(originalResponse.getContentAsByteArray());
        originalResponse.getHeaderNames()
                .forEach(header -> builder.withHeader(header, originalResponse.getHeaders(header)));
        return builder.build();
    }
}
