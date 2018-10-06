package com.atlassian.oai.validator;

import com.atlassian.oai.validator.interaction.ApiOperationResolver;
import com.atlassian.oai.validator.interaction.request.RequestValidator;
import com.atlassian.oai.validator.interaction.response.ResponseValidator;
import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.ApiOperationMatch;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.MessageContext;
import com.atlassian.oai.validator.schema.SchemaValidator;
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Validates a HTTP interaction (request/response pair) with a Swagger v2 / OpenAPI v3 specification.
 * <p>
 * Validation errors are provided in a @{@link ValidationReport} that can be used to inspect the failures.
 * <p>
 * New instances should be created via the {@link OpenApiInteractionValidator#createFor(String)} method.
 *
 * @see #createFor(String)
 */
public class OpenApiInteractionValidator {

    private final MessageResolver messages;

    private final ApiOperationResolver apiOperationResolver;
    private final RequestValidator requestValidator;
    private final ResponseValidator responseValidator;
    private final ValidationErrorsWhitelist whitelist;

    /**
     * Create a new instance using the OpenAPI / Swagger specification at the given location OR an actual specification payload.
     * <p>
     * Supports both Swagger v2 and OpenAPI v3 specifications, in both JSON and YAML formats.
     * <p>
     * The URL can be an absolute HTTP/HTTPS URL, a File URL or a classpath location (without the classpath: scheme).
     * <p>
     * For example:
     * <pre>
     *     // Create from a publicly hosted HTTP location
     *     .createFor("http://api.myservice.com/swagger.json")
     *
     *     // Create from a file on the local filesystem
     *     .createFor("file://Users/myuser/tmp/api.yaml");
     *
     *     // Create from a classpath resource in the /api package
     *     .createFor("/api/swagger.json");
     *
     *     // Create from an OpenAPI / Swagger payload
     *     .createFor("{\"swagger\": \"2.0\", ...}")
     * </pre>
     *
     * @param specUrlOrPayload The location of the OpenAPI / Swagger specification to use in the validator,
     * or the inline specification to use.
     *
     * @return A new builder instance to use for creating configuring {@link OpenApiInteractionValidator} instances.
     */
    public static Builder createFor(@Nonnull final String specUrlOrPayload) {
        return new Builder().withApiSpecification(specUrlOrPayload);
    }

    /**
     * Construct a new validator for the given specification.
     *
     * @param specUrlOrPayload The location of the OpenAPI / Swagger specification to use in the validator,
     * or the inline specification to use.
     * @param basePathOverride (Optional) override for the base path defined in the Swagger specification.
     * @param messages The message resolver to use for resolving validation messages.
     * @param whitelist The validation errors whitelist.
     */
    private OpenApiInteractionValidator(@Nonnull final String specUrlOrPayload,
                                        @Nullable final String basePathOverride,
                                        @Nonnull final MessageResolver messages,
                                        @Nonnull final ValidationErrorsWhitelist whitelist) {
        this(specUrlOrPayload, basePathOverride, messages, whitelist, null);
    }

    /**
     * Construct a new validator for the specification at the given URL with authentication data.
     *
     * @param specUrlOrPayload The location of the OpenAPI / Swagger specification to use in the validator,
     * or the inline specification to use.
     * @param basePathOverride (Optional) override for the base path defined in the specification.
     * @param messages The message resolver to use for resolving validation messages.
     * @param whitelist The validation errors whitelist.
     * @param authData (Optional) A List of authentication data to add to spec retrieval request.
     *
     * @throws IllegalArgumentException if the provided <code>specUrlOrPayload</code> is empty
     * @throws ApiLoadException if there was a problem loading the API spec
     */
    private OpenApiInteractionValidator(@Nonnull final String specUrlOrPayload,
                                        @Nullable final String basePathOverride,
                                        @Nonnull final MessageResolver messages,
                                        @Nonnull final ValidationErrorsWhitelist whitelist,
                                        @Nullable final List<AuthorizationValue> authData) {
        if (isBlank(specUrlOrPayload)) {
            throw new IllegalArgumentException("A specification URL or payload is required");
        }

        final OpenAPI api = loadApi(specUrlOrPayload, authData);

        this.messages = messages;
        apiOperationResolver = new ApiOperationResolver(api, basePathOverride);
        final SchemaValidator schemaValidator = new SchemaValidator(api, messages);
        requestValidator = new RequestValidator(schemaValidator, messages, api);
        responseValidator = new ResponseValidator(schemaValidator, messages, api);
        this.whitelist = whitelist;
    }

    @Nonnull
    private OpenAPI loadApi(@Nonnull final String specUrlOrPayload,
                            @Nullable final List<AuthorizationValue> authData) {

        final OpenAPIParser openAPIParser = new OpenAPIParser();
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(false);

        SwaggerParseResult parseResult;
        try {
            // Try to load as a URL first, then as a content string if that fails
            parseResult = openAPIParser.readLocation(specUrlOrPayload, authData, parseOptions);
            if (parseResult == null || parseResult.getOpenAPI() == null) {
                parseResult = openAPIParser.readContents(specUrlOrPayload, authData, parseOptions);
            }
        } catch (final Exception e) {
            throw new ApiLoadException(specUrlOrPayload, e);
        }
        if (parseResult == null || parseResult.getOpenAPI() == null ||
                (parseResult.getMessages() != null && !parseResult.getMessages().isEmpty())) {
            throw new ApiLoadException(specUrlOrPayload, parseResult);
        }

        return parseResult.getOpenAPI();
    }

    /**
     * Validate the given request/response against the API.
     * <p>
     * See class docs for more information on the validation performed.
     *
     * @param request The request to validate (required)
     * @param response The response to validate (required)
     *
     * @return The outcome of the validation
     */
    @Nonnull
    public ValidationReport validate(@Nonnull final Request request, @Nonnull final Response response) {
        requireNonNull(request, "A request is required");
        requireNonNull(response, "A response is required");

        //CHECKSTYLE:OFF Indentation
        return validateOnApiOperation(
                request.getPath(),
                request.getMethod(),
                apiOperation ->
                        requestValidator.validateRequest(request, apiOperation)
                                .merge(responseValidator.validateResponse(response, apiOperation)),
                (apiOperation, report) -> withWhitelistApplied(report, apiOperation, request, response));
        //CHECKSTYLE:ON Indentation
    }

    /**
     * Validate the given request against the API.
     * <p>
     * See class docs for more information on the validation performed.
     *
     * @param request The request to validate (required)
     *
     * @return The outcome of the request validation
     */
    @Nonnull
    public ValidationReport validateRequest(@Nonnull final Request request) {
        requireNonNull(request, "A request is required");

        //CHECKSTYLE:OFF Indentation
        return validateOnApiOperation(
                request.getPath(),
                request.getMethod(),
                apiOperation -> requestValidator.validateRequest(request, apiOperation),
                (apiOperation, report) -> withWhitelistApplied(report, apiOperation, request, null));
        //CHECKSTYLE:ON Indentation
    }

    /**
     * Validate the given response against the API.
     * <p>
     * See class docs for more information on the validation performed.
     *
     * @param path The request path (required)
     * @param method The request method (required)
     * @param response The response to validate (required)
     *
     * @return The outcome of the response validation
     */
    @Nonnull
    public ValidationReport validateResponse(@Nonnull final String path, @Nonnull final Request.Method method,
                                             @Nonnull final Response response) {
        requireNonNull(path, "A path is required");
        requireNonNull(method, "A method is required");
        requireNonNull(response, "A response is required");

        //CHECKSTYLE:OFF Indentation
        return validateOnApiOperation(
                path,
                method,
                apiOperation -> responseValidator.validateResponse(response, apiOperation),
                (apiOperation, report) -> withWhitelistApplied(report, apiOperation, null, response));
        //CHECKSTYLE:ON Indentation
    }

    private ValidationReport validateOnApiOperation(@Nonnull final String path,
                                                    @Nonnull final Request.Method method,
                                                    @Nonnull final Function<ApiOperation, ValidationReport> validationFunction,
                                                    @Nonnull final BiFunction<ApiOperation, ValidationReport, ValidationReport> whitelistingFunction) {

        final MessageContext context = MessageContext.create()
                .withRequestPath(path)
                .withRequestMethod(method)
                .build();

        final ApiOperationMatch apiOperationMatch = apiOperationResolver.findApiOperation(path, method);
        if (!apiOperationMatch.isPathFound()) {
            return whitelistingFunction.apply(null, ValidationReport.singleton(
                    messages.get("validation.request.path.missing", path)).withAdditionalContext(context)
            );
        }

        if (!apiOperationMatch.isOperationAllowed()) {
            return whitelistingFunction.apply(null, ValidationReport.singleton(
                    messages.get("validation.request.operation.notAllowed", method, path)).withAdditionalContext(context)
            );
        }

        final ApiOperation apiOperation = apiOperationMatch.getApiOperation();
        return validationFunction
                .andThen(report -> whitelistingFunction.apply(apiOperation, report))
                .apply(apiOperation)
                .withAdditionalContext(context);
    }

    private ValidationReport withWhitelistApplied(final ValidationReport report,
                                                  @Nullable final ApiOperation operation,
                                                  @Nullable final Request request,
                                                  @Nullable final Response response) {
        return ValidationReport.from(
                report.getMessages().stream()
                        .map(message -> whitelist
                                .whitelistedBy(message, operation, request, response)
                                .map(rule -> message
                                        .withLevel(ValidationReport.Level.IGNORE)
                                        .withAdditionalContext(
                                                MessageContext.create()
                                                        .withAppliedWhitelistRule(rule)
                                                        .build()
                                        )
                                )
                                .orElse(message))
                        .collect(Collectors.toList()));
    }

    /**
     * A builder used to createFor configured instances of the {@link OpenApiInteractionValidator}.
     */
    public static class Builder {
        private String specUrlOrPayload = "";
        private String basePathOverride;
        private LevelResolver levelResolver = LevelResolver.defaultResolver();
        private List<AuthorizationValue> authData;
        private ValidationErrorsWhitelist whitelist = ValidationErrorsWhitelist.create();

        /**
         * The location of the OpenAPI / Swagger specification to use in the validator, or the inline specification to use.
         * <p>
         * Supports both Swagger v2 and OpenAPI v3 specifications, in both JSON and YAML formats.
         * <p>
         * The URL can be an absolute HTTP/HTTPS URL, a File URL or a classpath location (without the classpath: scheme).
         * <p>
         * For example:
         * <pre>
         *     // Create from a publicly hosted HTTP location
         *     .withSwaggerJsonUrl("http://api.myservice.com/swagger.json")
         *
         *     // Create from a file on the local filesystem
         *     .withSwaggerJsonUrl("file://Users/myuser/tmp/api.yaml");
         *
         *     // Create from a classpath resource in the /api package
         *     .withSwaggerJsonUrl("/api/swagger.json");
         * </pre>
         *
         * @param specUrlOrPayload The OpenAPI / Swagger specification to use in the validator.
         *
         * @return this builder instance.
         *
         * @deprecated use {@link #withApiSpecification(String)}. This method will be removed in a future release.
         */
        @Deprecated
        public Builder withSwaggerJsonUrl(final String specUrlOrPayload) {
            return withApiSpecification(specUrlOrPayload);
        }

        /**
         * The location of the OpenAPI / Swagger specification to use in the validator, or the inline specification to use.
         * <p>
         * Supports both Swagger v2 and OpenAPI v3 specifications, in both JSON and YAML formats.
         * <p>
         * The URL can be an absolute HTTP/HTTPS URL, a File URL or a classpath location (without the classpath: scheme).
         * <p>
         * For example:
         * <pre>
         *     // Create from a publicly hosted HTTP location
         *     .withSwaggerJsonUrl("http://api.myservice.com/swagger.json")
         *
         *     // Create from a file on the local filesystem
         *     .withSwaggerJsonUrl("file://Users/myuser/tmp/api.yaml");
         *
         *     // Create from a classpath resource in the /api package
         *     .withSwaggerJsonUrl("/api/swagger.json");
         * </pre>
         *
         * @param specUrlOrPayload The OpenAPI / Swagger specification to use in the validator.
         *
         * @return this builder instance.
         */
        public Builder withApiSpecification(final String specUrlOrPayload) {
            this.specUrlOrPayload = specUrlOrPayload;
            return this;
        }

        /**
         * An optional basepath override to override the one defined in the OpenAPI / Swagger spec.
         * <p>
         * This can be useful if e.g. your Swagger specification has been created for a public URL but you are validating
         * requests against an internal URL where the URL paths differ.
         *
         * @param basePathOverride An optional basepath override to override the one defined in the spec.
         *
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
         *
         * @return this builder instance.
         */
        public Builder withLevelResolver(final LevelResolver levelResolver) {
            this.levelResolver = levelResolver;
            return this;
        }

        /**
         * A whitelist for error messages. Whitelisted error messages will still be returned, but their level will be
         * changed to IGNORE and additional information about whitelisting will be added.
         *
         * @param whitelist The whitelist to use.
         *
         * @return this builder instance
         */
        public Builder withWhitelist(final ValidationErrorsWhitelist whitelist) {
            this.whitelist = whitelist;
            return this;
        }

        /**
         * An optional key value header to add to the OpenAPI / Swagger spec retrieval request.
         * <p>
         * This is necessary if e.g. your specification is retrieved from a remote host and the path to retrieve is secured by an api key in the request header.
         *
         * @param key A key name to add as request header key.
         * @param value (Optional) A value to add as request header value for the given key.
         *
         * @return this builder instance.
         */
        public Builder withAuthHeaderData(final String key,
                                          final String value) {
            requireNonNull(key, "A key for the auth header is required");

            authData = singletonList(new AuthorizationValue(key, value, "header"));
            return this;
        }

        /**
         * Build a configured {@link OpenApiInteractionValidator} instance with the values collected in this builder.
         *
         * @return The configured {@link OpenApiInteractionValidator} instance.
         *
         * @throws IllegalArgumentException if the provided <code>specUrlOrPayload</code> is empty
         * @throws ApiLoadException if there was a problem loading the API spec
         */
        public OpenApiInteractionValidator build() {
            return new OpenApiInteractionValidator(specUrlOrPayload, basePathOverride, new MessageResolver(levelResolver), whitelist, authData);
        }
    }

    /**
     * An exception thrown when the {@link OpenApiInteractionValidator} is unable to load a given API spec
     */
    public static class ApiLoadException extends IllegalArgumentException {

        private final String specUrlOrPayload;
        private final List<String> parseMessages;

        ApiLoadException(final String specUrlOrPayload,
                         @Nullable final SwaggerParseResult parseResult) {
            super("Unable to load API spec from provided URL or payload");
            this.specUrlOrPayload = specUrlOrPayload;
            if (parseResult != null) {
                parseMessages = defaultIfNull(parseResult.getMessages(), emptyList());
            } else {
                parseMessages = emptyList();
            }
        }

        ApiLoadException(final String specUrlOrPayload,
                         final Throwable cause) {
            super("Unable to load API spec from provided URL or payload", cause);
            this.specUrlOrPayload = specUrlOrPayload;
            parseMessages = emptyList();
        }

        @Override
        public String getMessage() {
            if (parseMessages.isEmpty()) {
                return super.getMessage();
            }

            return super.getMessage() + ":\n\t- " + parseMessages.stream().collect(joining("\n\t- "));
        }

        public List<String> getParseMessages() {
            return parseMessages;
        }

        public String getSpecUrlOrPayload() {
            return specUrlOrPayload;
        }
    }
}
