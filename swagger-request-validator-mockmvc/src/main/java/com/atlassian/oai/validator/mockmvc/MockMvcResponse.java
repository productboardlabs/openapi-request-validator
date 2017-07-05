package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleResponse;
import org.slf4j.Logger;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

public class MockMvcResponse implements Response {

    private static final Logger LOGGER = getLogger(MockMvcResponse.class);

    private final Response delegate;

    /**
     * @deprecated Use: {@link MockMvcRequest#of(MockHttpServletRequest)}
     */
    @Deprecated
    public MockMvcResponse(@Nonnull final MockHttpServletResponse originalResponse) {
        this.delegate = MockMvcResponse.of(originalResponse);
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
     * Builds a {@link Response} for the Swagger validator out of the
     * original {@link MockHttpServletResponse}.
     *
     * @param originalResponse the original {@link MockHttpServletResponse}
     */
    @Nonnull
    public static Response of(@Nonnull final MockHttpServletResponse originalResponse) {
        requireNonNull(originalResponse, "An original response is required");
        final SimpleResponse.Builder builder = new SimpleResponse.Builder(originalResponse.getStatus())
                .withBody(getBody(originalResponse));
        originalResponse.getHeaderNames()
                .forEach(header -> builder.withHeader(header, originalResponse.getHeaders(header)));
        return builder.build();
    }

    private static String getBody(@Nonnull final MockHttpServletResponse originalResponse) {
        try {
            if (originalResponse.getContentAsByteArray().length > 0) {
                return originalResponse.getContentAsString();
            }
        } catch (final UnsupportedEncodingException e) {
            LOGGER.warn("Can't read request body.", e);
        }
        return null;
    }
}
