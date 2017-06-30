package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.model.SimpleResponse;
import org.slf4j.Logger;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

public class MockMvcResponse {

    private static final Logger LOGGER = getLogger(MockMvcResponse.class);

    private MockMvcResponse() {
    }

    /**
     * Builds a {@link Response} for the Swagger validator out of the
     * original {@link MockHttpServletResponse}.
     *
     * @param originalResponse the original {@link MockHttpServletResponse}
     */
    @Nonnull
    static Response of(@Nonnull final MockHttpServletResponse originalResponse) {
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
