package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.model.Response;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * An adapter for using spring-test {@link MockHttpServletResponse} with the Swagger Request Validator.
 */
public class MockMvcResponse implements Response {

    private final MockHttpServletResponse internalResponse;

    public MockMvcResponse(@Nonnull final MockHttpServletResponse internalResponse) {
        this.internalResponse = requireNonNull(internalResponse, "A response is required");
    }

    @Override
    public int getStatus() {
        return internalResponse.getStatus();
    }

    @Nonnull @Override
    public Optional<String> getBody() {
        try {
            return (internalResponse.getContentAsByteArray().length > 0) ? Optional.of(internalResponse.getContentAsString()) : Optional.empty();
        } catch (final UnsupportedEncodingException e) {
            return Optional.empty();
        }
    }

    @Nonnull @Override
    public Collection<String> getHeaderValues(final String name) {
        return internalResponse.getHeaders(name);
    }
}
