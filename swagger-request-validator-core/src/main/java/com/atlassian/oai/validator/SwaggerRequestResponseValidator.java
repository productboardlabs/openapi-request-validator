package com.atlassian.oai.validator;

import com.atlassian.oai.validator.interaction.RequestValidator;
import com.atlassian.oai.validator.interaction.ResponseValidator;
import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Validates a HTTP request/response pair with a Swagger/OpenAPI specification.
 * <p>
 * Validation errors are provided in a @{@link ValidationReport} that can be used to inspect the failures.
 * <p>
 * New instances should be created via the {@link SwaggerRequestResponseValidator#createFor(String)} method.
 *
 * @see #createFor(String)
 */
public class SwaggerRequestResponseValidator {

    private final Swagger api;
    private final Optional<String> basePathOverride;
    private final MessageResolver messages;

    private final RequestValidator requestValidator;
    private final ResponseValidator responseValidator;

    /**
     * Create a new instance using the Swagger JSON specification at the given location OR actual swagger JSON String payload.
     * <p>
     * The URL can be an absolute HTTP/HTTPS URL, a File URL or a classpath location (without the classpath: scheme).
     * <p>
     * For example:
     * <pre>
     *     // Create from a publicly hosted HTTP location
     *     .createFor("http://api.myservice.com/swagger.json")
     *
     *     // Create from a file on the local filesystem
     *     .createFor("file://Users/myuser/tmp/swagger.json");
     *
     *     // Create from a classpath resource in the /api package
     *     .createFor("/api/swagger.json");
     *
     *     // Create from a swagger JSON payload
     *     .createFor("{\"swagger\": \"2.0\", ...}")
     * </pre>
     *
     * @param swaggerJsonUrlOrPayload The location of the Swagger JSON specification to use in the validator.
     * @return A new builder instance to use for creating configuring {@link SwaggerRequestResponseValidator} instances.
     */
    public static Builder createFor(@Nonnull final String swaggerJsonUrlOrPayload) {
        return new Builder().withSwaggerJsonUrl(swaggerJsonUrlOrPayload);
    }

    /**
     * Construct a new validator for the specification at the given URL.
     *
     * @param swaggerJsonUrlOrPayload   The location of the Swagger JSON specification to use in this validator.
     * @param basePathOverride (Optional) override for the base path defined in the Swagger specification.
     * @param messages         The message resolver to use for resolving validation messages.
     */
    private SwaggerRequestResponseValidator(@Nonnull final String swaggerJsonUrlOrPayload,
                                            @Nullable final String basePathOverride,
                                            @Nonnull final MessageResolver messages) {

        requireNonNull(swaggerJsonUrlOrPayload, "A Swagger JSON URL or payload is required");

        final SwaggerDeserializationResult swaggerParseResult =
                swaggerJsonUrlOrPayload.startsWith("{") ?
                        new SwaggerParser().readWithInfo(swaggerJsonUrlOrPayload) :
                        new SwaggerParser().readWithInfo(swaggerJsonUrlOrPayload, null, true);
        this.api = swaggerParseResult.getSwagger();
        if (api == null) {
            throw new IllegalArgumentException(
                    format("Unable to load API descriptor from provided %s:\n\t%s",
                            swaggerJsonUrlOrPayload, swaggerParseResult.getMessages().toString().replace("\n", "\n\t")));
        }
        this.basePathOverride = Optional.ofNullable(basePathOverride);
        this.messages = messages;
        final SchemaValidator schemaValidator = new SchemaValidator(api, messages);
        this.requestValidator = new RequestValidator(schemaValidator, messages, api);
        this.responseValidator = new ResponseValidator(schemaValidator, messages, api);
    }

    /**
     * Validate the given request/response against the API.
     * <p>
     * See class docs for more information on the validation performed.
     *
     * @param request  The request to validate (required)
     * @param response The response to validate (required)
     * @return The outcome of the validation
     */
    @Nonnull
    public ValidationReport validate(@Nonnull final Request request, @Nonnull final Response response) {
        requireNonNull(request, "A request is required");
        requireNonNull(response, "A response is required");

        return validateOnApiOperation(request.getPath(), request.getMethod(),
            apiOperation -> validateRequest(request, apiOperation)
                    .merge(responseValidator.validateResponse(response, apiOperation))
        );
    }

    /**
     * Validate the given request against the API.
     * <p>
     * See class docs for more information on the validation performed.
     *
     * @param request The request to validate (required)
     * @return The outcome of the request validation
     */
    @Nonnull
    public ValidationReport validateRequest(@Nonnull final Request request) {
        requireNonNull(request, "A request is required");

        return validateOnApiOperation(request.getPath(), request.getMethod(),
            apiOperation -> validateRequest(request, apiOperation)
        );
    }

    /**
     * Validate the given response against the API.
     * <p>
     * See class docs for more information on the validation performed.
     *
     * @param path     The request path (required)
     * @param method   The request method (required)
     * @param response The response to validate (required)
     * @return The outcome of the response validation
     */
    @Nonnull
    public ValidationReport validateResponse(@Nonnull final String path, @Nonnull final Request.Method method,
                                             @Nonnull final Response response) {
        requireNonNull(path, "A path is required");
        requireNonNull(method, "A method is required");
        requireNonNull(response, "A response is required");

        return validateOnApiOperation(path, method,
            apiOperation -> responseValidator.validateResponse(response, apiOperation)
        );
    }

    private ValidationReport validateRequest(@Nonnull final Request request, @Nonnull final ApiOperation apiOperation) {
        final NormalisedPath requestPath = new ApiBasedNormalisedPath(request.getPath());
        return requestValidator.validateRequest(requestPath, request, apiOperation);
    }

    private ValidationReport validateOnApiOperation(@Nonnull final String path, @Nonnull final Request.Method method,
                                                    @Nonnull final Function<ApiOperation, ValidationReport> validationFunction) {
        final NormalisedPath requestPath = new ApiBasedNormalisedPath(path);

        final Map<NormalisedPath, Path> possibleApiPaths = findMatchingApiPaths(requestPath, method);
        if (possibleApiPaths.isEmpty()) {
            return ValidationReport.singleton(
                    messages.get("validation.request.path.missing", path)
            );
        }

        final Map<NormalisedPath, Path> possibleApiPathsForMethod = filterApiPathsByMethod(possibleApiPaths, method);
        if (possibleApiPathsForMethod.isEmpty()) {
            return ValidationReport.singleton(
               messages.get("validation.request.operation.notAllowed", method, path)
            );
        }
        final Map.Entry<NormalisedPath, Path> possiblePath = possibleApiPathsForMethod.entrySet().stream().findFirst().get();
        final String originalApiString = possiblePath.getKey().original();
        final Path apiPath = api.getPath(originalApiString);

        final HttpMethod httpMethod = HttpMethod.valueOf(method.name());
        final Operation operation = apiPath.getOperationMap().get(httpMethod);

        return validationFunction.apply(
                new ApiOperation(possiblePath.getKey(), apiPath, httpMethod, operation)
        );
    }

    @Nonnull
    private Map<NormalisedPath, Path> findMatchingApiPaths(@Nonnull final NormalisedPath requestPath, @Nonnull final Request.Method method) {
        final Map<ApiBasedNormalisedPath, Path> normalizedPathMap = api.getPaths().entrySet().stream()
            .collect(Collectors.toMap(p -> new ApiBasedNormalisedPath(p.getKey()), Map.Entry::getValue));

        return normalizedPathMap.entrySet().stream().filter(p -> pathMatches(requestPath, p.getKey()))
           .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Nonnull
    private Map<NormalisedPath, Path> filterApiPathsByMethod(final Map<NormalisedPath, Path> possibleApiPaths, final Request.Method method) {
        return possibleApiPaths.entrySet().stream().filter(p -> methodMatches(method, p)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean methodMatches(@Nonnull final Request.Method method, final Map.Entry<NormalisedPath, Path> p) {
        final Map<HttpMethod, Operation> operationMap = p.getValue().getOperationMap();
        return operationMap != null && operationMap.containsKey(HttpMethod.valueOf(method.name()));
    }

    private boolean pathMatches(@Nonnull final NormalisedPath requestPath, @Nonnull final NormalisedPath apiPath) {
        if (requestPath.parts().size() != apiPath.parts().size()) {
            return false;
        }
        for (int i = 0; i < requestPath.parts().size(); i++) {
            if (requestPath.part(i).equalsIgnoreCase(apiPath.part(i)) || apiPath.isParam(i)) {
                continue;
            }
            return false;
        }
        return true;
    }

    private class ApiBasedNormalisedPath implements NormalisedPath {
        private final List<String> pathParts;
        private final String original;
        private final String normalised;

        ApiBasedNormalisedPath(@Nonnull final String path) {
            this.original = requireNonNull(path, "A path is required");
            this.normalised = normalise(path);
            this.pathParts = unmodifiableList(asList(normalised.split("/")));
        }

        @Override
        @Nonnull
        public List<String> parts() {
            return pathParts;
        }

        @Override
        @Nonnull
        public String part(final int index) {
            return pathParts.get(index);
        }

        @Override
        public boolean isParam(final int index) {
            final String part = part(index);
            return part.startsWith("{") && part.endsWith("}");
        }

        @Override
        @Nullable
        public String paramName(final int index) {
            if (!isParam(index)) {
                return null;
            }
            final String part = part(index);
            return part.substring(1, part.length() - 1);
        }

        @Override
        @Nonnull
        public String original() {
            return original;
        }

        @Override
        @Nonnull
        public String normalised() {
            return normalised;
        }

        private String normalise(final String requestPath) {
            final String trimmedPath = trimPrefix(requestPath, basePathOverride.orElse(api.getBasePath()));
            if (!trimmedPath.startsWith("/")) {
                return "/" + requestPath;
            }
            return trimmedPath;
        }

        private String trimPrefix(@Nonnull final String requestPath, @Nullable final String prefix) {
            if (prefix == null || !requestPath.startsWith(prefix)) {
                return requestPath;
            }
            return requestPath.substring(prefix.length());
        }
    }

    /**
     * A builder used to createFor configured instances of the {@link SwaggerRequestResponseValidator}.
     */
    public static class Builder {
        private String swaggerJsonUrlOrPayload = "";
        private String basePathOverride;
        private LevelResolver levelResolver = LevelResolver.defaultResolver();

        /**
         * The location of the Swagger JSON specification to use in the validator.
         * <p>
         * The URL can be an absolute HTTP/HTTPS URL, a File URL or a classpath location (without the classpath: scheme).
         * <p>
         * For example:
         * <pre>
         *     // Create from a publicly hosted HTTP location
         *     .withSwaggerJsonUrl("http://api.myservice.com/swagger.json")
         *
         *     // Create from a file on the local filesystem
         *     .withSwaggerJsonUrl("file://Users/myuser/tmp/swagger.json");
         *
         *     // Create from a classpath resource in the /api package
         *     .withSwaggerJsonUrl("/api/swagger.json");
         * </pre>
         *
         * @param swaggerJsonUrlOrPayload The location of the Swagger JSON specification to use in the validator.
         * @return this builder instance.
         */
        public Builder withSwaggerJsonUrl(final String swaggerJsonUrlOrPayload) {
            this.swaggerJsonUrlOrPayload = swaggerJsonUrlOrPayload;
            return this;
        }

        /**
         * An optional basepath override to override the one defined in the Swagger spec.
         * <p>
         * This can be useful if e.g. your Swagger specification has been created for a public URL but you are validating
         * requests against an internal URL where the URL paths differ.
         *
         * @param basePathOverride An optional basepath override to override the one defined in the Swagger spec.
         * @return this builder instance.
         */
        public Builder withBasePathOverride(final String basePathOverride) {
            this.basePathOverride = basePathOverride;
            return this;
        }

        /**
         * The resolver to use for resolving the level of validation messages (ERROR, WARN, IGNORE etc.).
         * <p>
         * This can be used to get fine-grained control over validation behaviour
         * (e.g. what level to emit message at, which validations to ignore etc.).
         * <p>
         * If not provided, a default resolver will be used that resolves all message to ERROR.
         *
         * @param levelResolver The resolver to use for resolving validation message levels.
         * @return this builder instance.
         */
        public Builder withLevelResolver(final LevelResolver levelResolver) {
            this.levelResolver = levelResolver;
            return this;
        }

        /**
         * Build a configured {@link SwaggerRequestResponseValidator} instance with the values collected in this builder.
         *
         * @return The configured {@link SwaggerRequestResponseValidator} instance.
         */
        public SwaggerRequestResponseValidator build() {
            return new SwaggerRequestResponseValidator(swaggerJsonUrlOrPayload, basePathOverride, new MessageResolver(levelResolver));
        }
    }
}
