package com.atlassian.oai.validator;

import com.atlassian.oai.validator.interaction.ApiOperationResolver;
import com.atlassian.oai.validator.interaction.request.CustomRequestValidator;
import com.atlassian.oai.validator.interaction.request.RequestValidator;
import com.atlassian.oai.validator.interaction.response.CustomResponseValidator;
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
import com.atlassian.oai.validator.schema.SwaggerV20Library;
import com.atlassian.oai.validator.schema.ValidationConfiguration;
import com.atlassian.oai.validator.util.OpenApiLoader;
import com.atlassian.oai.validator.whitelist.ValidationErrorsWhitelist;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.atlassian.oai.validator.util.StringUtils.requireNonEmpty;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

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
     * <b>Note:</b> This method may log exceptions during normal operation. To avoid this, consider using
     * {@link #createForInlineApiSpecification(String)} or {@link #createForSpecificationUrl(String)} instead.
     * This method may be deprecated in the future.
     *
     * @param specUrlOrPayload The location of the OpenAPI / Swagger specification to use in the validator,
     * or the inline specification to use.
     *
     * @return A new builder instance to use for creating and configuring {@link OpenApiInteractionValidator} instances.
     *
     * @see #createForInlineApiSpecification(String)
     * @see #createForSpecificationUrl(String)
     */
    public static Builder createFor(@Nonnull final String specUrlOrPayload) {
        return new Builder().withApiSpecification(specUrlOrPayload);
    }

    /**
     * Create a new instance using given the OpenAPI / Swagger specification.
     * <p>
     * Supports both Swagger v2 and OpenAPI v3 specifications, in both JSON and YAML formats.
     *
     * @param specAsString The OpenAPI / Swagger specification to use in the validator
     *
     * @return A new builder instance to use for creating and configuring {@link OpenApiInteractionValidator} instances.
     */
    public static Builder createForInlineApiSpecification(@Nonnull final String specAsString) {
        return new Builder().withInlineApiSpecification(specAsString);
    }

    /**
     * Create a new instance using the OpenAPI / Swagger specification at the given location.
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
     * @param specUrl The location of the OpenAPI / Swagger specification to use in the validator
     *
     * @return A new builder instance to use for creating and configuring {@link OpenApiInteractionValidator} instances.
     */
    public static Builder createForSpecificationUrl(@Nonnull final String specUrl) {
        return new Builder().withApiSpecificationUrl(specUrl);
    }

    /**
     * Create a new instance using a parsed API specification.
     *
     * @param api The API specification to use for validation
     *
     * @return A new builder instance to use for creating and configuring {@link OpenApiInteractionValidator} instances.
     */
    public static Builder createFor(@Nonnull final OpenAPI api) {
        return new Builder().withApi(api);
    }

    private OpenApiInteractionValidator(@Nonnull final OpenAPI api,
                                        @Nullable final String basePathOverride,
                                        @Nonnull final MessageResolver messages,
                                        @Nonnull final ValidationErrorsWhitelist whitelist,
                                        @Nonnull final Supplier<JsonSchemaFactory> schemaFactorySupplier,
                                        @Nonnull final List<CustomRequestValidator> customRequestValidators,
                                        @Nonnull final List<CustomResponseValidator> customResponseValidators,
                                        @Nonnull final ValidationConfiguration validationConfiguration,
                                        final boolean strictOperationPathMatching) {
        this.messages = messages;
        apiOperationResolver = new ApiOperationResolver(api, basePathOverride, strictOperationPathMatching);
        final SchemaValidator schemaValidator = new SchemaValidator(api, messages, schemaFactorySupplier, validationConfiguration);
        requestValidator = new RequestValidator(schemaValidator, messages, api, customRequestValidators);
        responseValidator = new ResponseValidator(schemaValidator, messages, api, customResponseValidators);
        this.whitelist = whitelist;
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
     * Holds the source location for an API specification.
     * <p>
     * May be a spec URL (remote HTTP/HTTPS, classpath or file) or an inline specification (JSON or YAML).
     */
    public static class SpecSource {

        private enum Type {
            INLINE_SPEC,
            SPEC_URL,
            UNKNOWN
        }

        public static SpecSource unknown(final String specUrlOrPayload) {
            return new SpecSource(specUrlOrPayload, Type.UNKNOWN);
        }

        public static SpecSource inline(final String inlineApiSpec) {
            return new SpecSource(inlineApiSpec, Type.INLINE_SPEC);
        }

        public static SpecSource specUrl(final String specUrl) {
            return new SpecSource(specUrl, Type.SPEC_URL);
        }

        @Nullable
        private final String value;

        @Nonnull
        private final Type type;

        private SpecSource(final String value,
                           final Type type) {
            this.value = value;
            this.type = type;
        }

        public boolean isInlineSpecification() {
            return type == Type.INLINE_SPEC;
        }

        public boolean isSpecUrl() {
            return type == Type.SPEC_URL;
        }

        @Nullable
        public String getValue() {
            return value;
        }
    }

    /**
     * A builder used to createFor configured instances of the {@link OpenApiInteractionValidator}.
     */
    public static class Builder {
        private SpecSource specSource;
        private String basePathOverride;
        private LevelResolver levelResolver = LevelResolver.defaultResolver();
        private final List<AuthorizationValue> authData = new ArrayList<>();
        private ParseOptions parseOptions = defaultParseOptions();
        private ValidationErrorsWhitelist whitelist = ValidationErrorsWhitelist.create();
        private final List<CustomRequestValidator> customRequestValidators = new ArrayList<>();
        private final List<CustomResponseValidator> customResponseValidators = new ArrayList<>();
        private OpenAPI api;
        private Supplier<JsonSchemaFactory> schemaFactorySupplier = SwaggerV20Library::schemaFactory;

        private ValidationConfiguration validationConfiguration = new ValidationConfiguration();
        private boolean strictOperationPathMatching = false;

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
         *
         * @deprecated Use {@link #withInlineApiSpecification(String)} or {@link #withApiSpecificationUrl(String)}
         */
        @Deprecated
        public Builder withApiSpecification(final String specUrlOrPayload) {
            requireNonEmpty(specUrlOrPayload, "A specification URL or payload is required");
            this.specSource = SpecSource.unknown(specUrlOrPayload);
            return this;
        }

        /**
         * The inline API specification to use.
         * <p>
         * Supports both Swagger v2 and OpenAPI v3 specifications, in both JSON and YAML formats.
         *
         * @param inlineSpecPayload The OpenAPI / Swagger specification to use in the validator.
         *
         * @return this builder instance.
         */
        public Builder withInlineApiSpecification(final String inlineSpecPayload) {
            requireNonEmpty(inlineSpecPayload, "A specification payload is required");
            this.specSource = SpecSource.inline(inlineSpecPayload);
            return this;
        }

        /**
         * The location of the OpenAPI / Swagger specification to use in the validator.
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
         * @param specUrl The OpenAPI / Swagger specification to use in the validator.
         *
         * @return this builder instance.
         */
        public Builder withApiSpecificationUrl(final String specUrl) {
            requireNonEmpty(specUrl, "A specification URL is required");
            this.specSource = SpecSource.specUrl(specUrl);
            return this;
        }

        public Builder withApi(final OpenAPI api) {
            requireNonNull(api, "An API is required");
            this.api = api;
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
         * This is necessary if e.g. your specification is retrieved from a remote host and the path to retrieve is
         * secured by an api key in the request header.
         *
         * @param key A key name to add as request header key.
         * @param value (Optional) A value to add as request header value for the given key.
         *
         * @return this builder instance.
         */
        public Builder withAuthHeaderData(final String key,
                                          final String value) {
            requireNonNull(key, "A key for the auth header is required");

            authData.add(new AuthorizationValue(key, value, "header"));
            return this;
        }

        /**
         * An optional custom request validation step.
         * Possible usages include validation of vendor specific extensions.
         *
         * @param validator The validator to apply
         *
         * @return this builder instance
         */
        public Builder withCustomRequestValidation(final CustomRequestValidator validator) {
            requireNonNull(validator, "A validator is required");
            customRequestValidators.add(validator);
            return this;
        }

        /**
         * An optional custom response validation step.
         * Possible usages include validation of vendor specific extensions.
         *
         * @param validator The validator to apply
         *
         * @return this builder instance
         */
        public Builder withCustomResponseValidation(final CustomResponseValidator validator) {
            requireNonNull(validator, "A validator is required");
            customResponseValidators.add(validator);
            return this;
        }

        /**
         * Sets the {@code resolve} and {@code resolveFully} flags on the {@link ParseOptions} supplied to the underlying {@link io.swagger.parser.OpenAPIParser}.
         * Useful if you need to control whether the parser resolves refs prior to validation. Default is {@code true}.
         * <p>
         * If additional parse options are needed use {@link #withParseOptions(ParseOptions)} to supply a fully-constructed
         * instance.
         *
         * @return this builder instance
         *
         * @see #withParseOptions(ParseOptions)
         */
        public Builder withResolveRefs(final boolean resolveRefs) {
            parseOptions.setResolve(resolveRefs);
            parseOptions.setResolveFully(resolveRefs);
            return this;
        }

        /**
         * Sets the {@code resolveCombinators} flag on the {@link ParseOptions} supplied to the underlying {@link io.swagger.parser.OpenAPIParser}.
         * Useful when using {@code allOf} composition to avoid the problems with {@code additionalProperties}.
         * <p>
         * If additional parse options are needed use {@link #withParseOptions(ParseOptions)} to supply a fully-constructed
         * instance.
         *
         * @return this builder instance
         *
         * @see #withParseOptions(ParseOptions)
         */
        public Builder withResolveCombinators(final boolean resolveCombinators) {
            parseOptions.setResolveCombinators(resolveCombinators);
            return this;
        }

        /**
         * Optionally supply parse options to control the behavior of the underlying {@link io.swagger.parser.OpenAPIParser} parser.
         * <p>
         * Sensible defaults are provided, but this can be useful if you need more fine-grained control over the behavior.
         * One example use case is to resolve schema combinators ({@code allOf} etc) to flatten the schema prior to validation
         * to avoid some of the problems with {@code allOf} and {@code additionalProperties}
         * <p>
         * The defaults used are:
         * <ul>
         *     <li>{@code resolve = true}</li>
         *     <li>{@code resolveFully = true}</li>
         *     <li>{@code resolveCombinators = false}</li>
         *     <li>{@code flatten = false}</li>
         * </ul>
         *
         * @param parseOptions Parse options to replace the defaults
         *
         * @return this builder instance
         */
        public Builder withParseOptions(final ParseOptions parseOptions) {
            requireNonNull(parseOptions, "Parse options are required");
            this.parseOptions = parseOptions;
            return this;
        }

        /**
         * Optionally supply a function that returns a {@link com.github.fge.jsonschema.main.JsonSchemaFactory} to use.
         * <p>
         * Defaults to {@link SwaggerV20Library}'s `schemaFactory`, but this can be useful if you have additional
         * extensions to add to {@link com.github.fge.jsonschema.library.Library}.
         *
         * @param schemaFactorySupplier A supplier function that returns a JsonSchemaFactory.
         *
         * @return this builder instance
         */
        public Builder withSchemaFactorySupplier(final Supplier<JsonSchemaFactory> schemaFactorySupplier) {
            requireNonNull(schemaFactorySupplier, "JsonSchemaFactory supplier is required");
            this.schemaFactorySupplier = schemaFactorySupplier;
            return this;
        }

        /**
         * Optionally supply a configuration to configure the following aspects of validation:
         * <ul>
         *     <li>The cache size of {@link com.github.fge.jsonschema.main.JsonSchema} in {@link com.atlassian.oai.validator.schema.SchemaValidator} </li>
         * </ul>
         * @param validationConfiguration The configuration for OpenApi validation.
         * @return this builder instance
         */
        public Builder withSchemaValidationConfiguration(final ValidationConfiguration validationConfiguration) {
            requireNonNull(validationConfiguration, "ValidationConfiguration is required");
            this.validationConfiguration = validationConfiguration;
            return this;
        }

        /**
         * Optionally enable strict operation path matching. If enabled, a trailing slash indicates a different path than without. Defaults to false.
         * @return this builder instance
         */
        public Builder withStrictOperationPathMatching() {
            this.strictOperationPathMatching = true;
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
            if (api == null) {
                this.api = new OpenApiLoader().loadApi(specSource, authData, parseOptions);
            }
            return new OpenApiInteractionValidator(
                    api,
                    basePathOverride,
                    new MessageResolver(levelResolver),
                    whitelist,
                    schemaFactorySupplier,
                    customRequestValidators,
                    customResponseValidators,
                    validationConfiguration,
                    strictOperationPathMatching);
        }

        private static ParseOptions defaultParseOptions() {
            final ParseOptions parseOptions = new ParseOptions();
            parseOptions.setResolve(true);
            parseOptions.setResolveFully(true);
            parseOptions.setResolveCombinators(false);
            return parseOptions;
        }
    }

    /**
     * An exception thrown when the {@link OpenApiInteractionValidator} is unable to load a given API spec
     */
    public static class ApiLoadException extends IllegalArgumentException {

        private final String specUrlOrPayload;
        private final List<String> parseMessages;

        public ApiLoadException(final String specUrlOrPayload,
                                @Nullable final SwaggerParseResult parseResult) {
            super("Unable to load API spec from provided URL or payload");
            this.specUrlOrPayload = specUrlOrPayload;
            if (parseResult != null) {
                parseMessages = defaultIfNull(parseResult.getMessages(), emptyList());
            } else {
                parseMessages = emptyList();
            }
        }

        public ApiLoadException(final String specUrlOrPayload,
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

            return super.getMessage() + ":\n\t- " + String.join("\n\t- ", parseMessages);
        }

        public List<String> getParseMessages() {
            return parseMessages;
        }

        public String getSpecUrlOrPayload() {
            return specUrlOrPayload;
        }
    }
}
