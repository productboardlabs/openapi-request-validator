package com.atlassian.oai.validator.model;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.apache.http.client.utils.DateUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.atlassian.oai.validator.model.Headers.ACCEPT;
import static com.atlassian.oai.validator.model.Headers.AUTHORIZATION;
import static com.atlassian.oai.validator.model.Headers.CONTENT_TYPE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

/**
 * Simple immutable {@link Request} implementation.
 * <p>
 * New instances should be constructed with a {@link Builder}.
 */
public class SimpleRequest implements Request {

    private final Method method;
    private final String path;
    private final Map<String, Collection<String>> headers;
    private final Map<String, Collection<String>> queryParams;
    private final Optional<String> requestBody;

    private SimpleRequest(@Nonnull final Method method,
                          @Nonnull final String path,
                          @Nonnull final Map<String, Collection<String>> headers,
                          @Nonnull final Map<String, Collection<String>> queryParams,
                          @Nullable final String body) {
        this.method = requireNonNull(method, "A method is required");
        this.path = requireNonNull(path, "A request path is required");
        this.queryParams = requireNonNull(queryParams);
        this.headers = requireNonNull(headers);
        requestBody = Optional.ofNullable(body);
    }

    @Nonnull
    @Override
    public String getPath() {
        return path;
    }

    @Nonnull
    @Override
    public Method getMethod() {
        return method;
    }

    @Nonnull
    @Override
    public Optional<String> getBody() {
        return requestBody;
    }

    @Override
    @Nonnull
    public Collection<String> getQueryParameterValues(final String name) {
        return getFromMapOrEmptyList(queryParams, name);
    }

    @Override
    @Nonnull
    public Collection<String> getQueryParameters() {
        return Collections.unmodifiableCollection(queryParams.keySet());
    }

    @Nonnull
    @Override
    public Map<String, Collection<String>> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    @Nonnull
    @Override
    public Collection<String> getHeaderValues(final String name) {
        return getFromMapOrEmptyList(headers, name);
    }

    static Collection<String> getFromMapOrEmptyList(final Map<String, Collection<String>> map, final String name) {
        if (name == null || !map.containsKey(name)) {
            return emptyList();
        }

        return map.get(name).stream().filter(Objects::nonNull)
                .collect(collectingAndThen(toList(), Collections::unmodifiableList));
    }

    /**
     * A builder for constructing new {@link SimpleRequest} instances.
     */
    public static class Builder {

        private final Method method;
        private final String path;
        private final Multimap<String, String> headers;
        private final Multimap<String, String> queryParams;
        private String body;

        /**
         * A convenience method for creating a {@link SimpleRequest.Builder} with
         * HTTP method GET and the given path.
         *
         * @param path the requests path
         * @return a prepared {@link SimpleRequest.Builder}
         */
        public static Builder get(final String path) {
            return new Builder(Method.GET, path);
        }

        /**
         * A convenience method for creating a {@link SimpleRequest.Builder} with
         * HTTP method PUT and the given path.
         *
         * @param path the requests path
         * @return a prepared {@link SimpleRequest.Builder}
         */
        public static Builder put(final String path) {
            return new Builder(Method.PUT, path);
        }

        /**
         * A convenience method for creating a {@link SimpleRequest.Builder} with
         * HTTP method POST and the given path.
         *
         * @param path the requests path
         * @return a prepared {@link SimpleRequest.Builder}
         */
        public static Builder post(final String path) {
            return new Builder(Method.POST, path);
        }

        /**
         * A convenience method for creating a {@link SimpleRequest.Builder} with
         * HTTP method DELETE and the given path.
         *
         * @param path the requests path
         * @return a prepared {@link SimpleRequest.Builder}
         */
        public static Builder delete(final String path) {
            return new Builder(Method.DELETE, path);
        }

        /**
         * A convenience method for creating a {@link SimpleRequest.Builder} with
         * HTTP method PATCH and the given path.
         *
         * @param path the requests path
         * @return a prepared {@link SimpleRequest.Builder}
         */
        public static Builder patch(final String path) {
            return new Builder(Method.PATCH, path);
        }

        /**
         * A convenience method for creating a {@link SimpleRequest.Builder} with
         * HTTP method HEAD and the given path.
         *
         * @param path the requests path
         * @return a prepared {@link SimpleRequest.Builder}
         */
        public static Builder head(final String path) {
            return new Builder(Method.HEAD, path);
        }

        /**
         * A convenience method for creating a {@link SimpleRequest.Builder} with
         * HTTP method OPTIONS and the given path.
         *
         * @param path the requests path
         * @return a prepared {@link SimpleRequest.Builder}
         */
        public static Builder options(final String path) {
            return new Builder(Method.OPTIONS, path);
        }

        /**
         * A convenience method for creating a {@link SimpleRequest.Builder} with
         * HTTP method TRACE and the given path.
         *
         * @param path the requests path
         * @return a prepared {@link SimpleRequest.Builder}
         */
        public static Builder trace(final String path) {
            return new Builder(Method.TRACE, path);
        }

        /**
         * Creates a {@link SimpleRequest.Builder} with the given HTTP method and path.
         *
         * @param method the HTTP method
         * @param path   the requests path
         */
        public Builder(final String method, final String path) {
            this(method, path, false);
        }

        /**
         * Creates a {@link SimpleRequest.Builder} with the given HTTP {@link Request.Method} and path.
         *
         * @param method the HTTP method
         * @param path   the requests path
         */
        public Builder(final Method method, final String path) {
            this(method, path, false);
        }

        /**
         * Creates a {@link SimpleRequest.Builder} with the given HTTP method and path including
         * the specification if the query parameters are handled case sensitive or not.
         *
         * @param method                       the HTTP method
         * @param path                         the requests path
         * @param queryParametersCaseSensitive flag if the query parameters are handled case sensitive or not
         */
        public Builder(final String method, final String path, final boolean queryParametersCaseSensitive) {
            this(Method.valueOf(requireNonNull(method, "A method is required").toUpperCase()),
                    path, queryParametersCaseSensitive);
        }

        /**
         * Creates a {@link SimpleRequest.Builder} with the given HTTP {@link Request.Method} and path including
         * the specification if the query parameters are handled case sensitive or not.
         *
         * @param method                       the HTTP method
         * @param path                         the requests path
         * @param queryParametersCaseSensitive flag if the query parameters are handled case sensitive or not
         */
        public Builder(final Method method, final String path, final boolean queryParametersCaseSensitive) {
            this.method = requireNonNull(method, "A method is required");
            this.path = requireNonNull(path, "A path is required");

            headers = multimapBuilder(false /* header are always case insensitive */);
            queryParams = multimapBuilder(queryParametersCaseSensitive);
        }

        /**
         * Adds a request body to this builder.
         *
         * @param body the request body
         * @return this builder
         */
        public Builder withBody(final String body) {
            this.body = body;
            return this;
        }

        /**
         * Adds a request header to this builder. If there was already a header with this
         * name the values will be added.
         * <p>
         * Headers are treated case insensitive.
         *
         * @param name   the header name
         * @param values the values for this header
         * @return this builder
         */
        public Builder withHeader(final String name, final List<String> values) {
            // available but not set headers are considered as empty
            putValuesToMapOrDefault(headers, name, values, "", true);
            return this;
        }

        /**
         * Adds a request header to this builder. If there was already a header with this
         * name the values will be added.
         * <p>
         * Headers are treated case insensitive.
         *
         * @param name   the header name
         * @param values the values for this header
         * @return this builder
         */
        public Builder withHeader(final String name, final String... values) {
            return withHeader(name, asList(values));
        }

        /**
         * Sets the content type header on this builder.
         * <p>
         * Equivalent to: <pre>withHeader("Content-Type", contentType);</pre>
         *
         * @param contentType The content type to set
         *
         * @return this builder
         */
        public Builder withContentType(final String contentType) {
            return withHeader(CONTENT_TYPE, contentType);
        }

        /**
         * Sets the accept header on this builder.
         * <p>
         * Equivalent to: <pre>withHeader("Accept", contentType);</pre>
         *
         * @param accept The accept type(s) to set
         *
         * @return this builder
         */
        public Builder withAccept(final String... accept) {
            return withHeader(ACCEPT, accept);
        }

        /**
         * Sets the authorization header on this builder.
         * <p>
         * Equivalent to: <pre>withHeader("Authorization", contentType);</pre>
         *
         * @param auth The authorization header to set
         *
         * @return this builder
         */
        public Builder withAuthorization(final String auth) {
            return withHeader(AUTHORIZATION, auth);
        }

        /**
         * Adds a query parameter to this request builder. If there was already a query
         * parameter with this name the values will be added.
         * <p>
         * The case sensitivity can be set by this builder's
         * {@linkplain SimpleRequest.Builder#Builder(Method, String, boolean)} constructor.
         *
         * @param name   the header name
         * @param values the values for this header
         * @return this builder
         */
        public Builder withQueryParam(final String name, final List<String> values) {
            // available but not set query parameters are considered as available but with no value
            putValuesToMapOrDefault(queryParams, name, values, null, false);
            return this;
        }

        /**
         * Adds a query parameter to this request builder. If there was already a query
         * parameter with this name the values will be added.
         * <p>
         * The case sensitivity can be set by this builder's
         * {@linkplain SimpleRequest.Builder#Builder(String, String, boolean)} constructor.
         *
         * @param name   the header name
         * @param values the values for this header
         * @return this builder
         */
        public Builder withQueryParam(final String name, final String... values) {
            return withQueryParam(name, asList(values));
        }

        /**
         * Builds a {@link SimpleRequest} out of this builder.
         *
         * @return the build {@link SimpleRequest}
         */
        public SimpleRequest build() {
            return new SimpleRequest(method, path, headers.asMap(), queryParams.asMap(), body);
        }

        static Multimap<String, String> multimapBuilder(final boolean caseSensitive) {
            return caseSensitive ? MultimapBuilder.hashKeys().arrayListValues().build() :
                    MultimapBuilder.treeKeys(String.CASE_INSENSITIVE_ORDER).arrayListValues().build();
        }

        static void putValuesToMapOrDefault(final Multimap<String, String> map, final String name,
                                            final List<String> values, final String defaultIfNotSet,
                                            final boolean splitValues) {
            if (values == null || values.isEmpty()) {
                map.putAll(name, splitValues ? splitHeaderValue(defaultIfNotSet) : Collections.singleton(null));
            } else {
                values.forEach(value -> map.putAll(name, splitValues ? splitHeaderValue(value) : Collections.singleton(value)));
            }
        }

        static Collection<String> splitHeaderValue(final String value) {
            System.out.println(value);
            //Do not split a date in RFC 1123 format
            if (value == null) {
                return Collections.singleton(null);
            }
            try {
                LocalDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
                return Arrays.asList(value);
            } catch (DateTimeParseException dtpe) {
                String[] split = value.split("\\s*,\\s*");

                //check if multiple dates
                List<String> dates = new ArrayList<>();
                if (split.length % 2 == 0) {
                    for (int i = 0; i < split.length; i = i + 2) {
                        try {
                            String possibleDate = split[i] + ", " + split[i + 1];
                            LocalDateTime.parse(split[i] + ", " + split[i + 1], DateTimeFormatter.RFC_1123_DATE_TIME);
                            dates.add(possibleDate);
                        } catch (DateTimeParseException dtpe2) {
                            break;
                        }
                    }
                    return dates;
                }
                return Arrays.asList(split);
            }
        }
    }
}
