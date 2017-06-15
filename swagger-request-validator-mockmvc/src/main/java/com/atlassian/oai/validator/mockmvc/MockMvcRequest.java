package com.atlassian.oai.validator.mockmvc;

import com.atlassian.oai.validator.model.Request;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * An adapter for using spring-test {@link MockHttpServletRequest} with the Swagger Request Validator.
 */
public class MockMvcRequest implements Request {
    private final MockHttpServletRequest internalRequest;

    public MockMvcRequest(@Nonnull final MockHttpServletRequest internalRequest) {
        this.internalRequest = requireNonNull(internalRequest, "A request is required");
    }

    @Nonnull @Override
    public String getPath() {
        return internalRequest.getPathInfo();
    }

    @Nonnull @Override
    public Request.Method getMethod() {
        return Request.Method.valueOf(internalRequest.getMethod().toUpperCase());
    }

    @Nonnull @Override
    public Optional<String> getBody() {

        try (BufferedReader reader = internalRequest.getReader()) {
            final StringBuilder builder = new StringBuilder();
            String aux;
            int lineCount = 0;
            while ((aux = reader.readLine()) != null) {
                builder.append(aux);
                lineCount++;
            }
            if (lineCount > 0) {
                return Optional.of(builder.toString());
            } else {
                return Optional.empty();
            }
        } catch (final IOException e) {
            return Optional.empty();
        }
    }

    @Nonnull @Override
    public Collection<String> getQueryParameters() {
        return Collections.list(internalRequest.getParameterNames());
    }

    @Nonnull @Override
    public Collection<String> getQueryParameterValues(final String name) {
        final TreeMap<String, String[]> caseInsensitiveParameterMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitiveParameterMap.putAll(internalRequest.getParameterMap());
        if (caseInsensitiveParameterMap.containsKey(name)) {
            return Arrays.asList(caseInsensitiveParameterMap.get(name));
        }
        return emptyList();
    }

    @Nonnull @Override
    public Map<String, Collection<String>> getHeaders() {
        final Map<String, Collection<String>> headers = new HashMap<>();
        final Enumeration<String> headerNames = internalRequest.getHeaderNames();
        for (; headerNames.hasMoreElements();) {
            final String headerName = headerNames.nextElement();
            headers.put(headerName, Collections.list(internalRequest.getHeaders(headerName)));
        }
        return headers;
    }

    @Nonnull @Override
    public Collection<String> getHeaderValues(final String name) {
        return Collections.list(internalRequest.getHeaders(name));
    }

}
