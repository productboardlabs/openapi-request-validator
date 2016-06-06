package com.atlassian.oai.validator;

import com.atlassian.oai.validator.interaction.RequestValidator;
import com.atlassian.oai.validator.interaction.ResponseValidator;
import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.MutableValidationReport;
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
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * Validates a HTTP request/response pair with a Swagger/OpenAPI specification.
 * <p>
 * Validation errors are provided in a @{@link ValidationReport} that can be used to inspect the failures.
 * <p>
 * Currently supports the following validation checks:
 * <p>
 * <b>Request</b>:
 * <ul>
 *     <li>Path existence - does the request path exist in the API spec</li>
 *     <li>Path parameter format - do the provided path parameters match the schema specified in the spec</li>
 *     <li>Request body existence - has a body been supplied where needed</li>
 *     <li>Request body schema - does the request body adhere to the schema defined in the spec</li>
 * </ul>
 * <p>
 * <b>Response</b>:
 * <ul>
 *     <li>Status code - does the response status match one defined in the spec</li>
 *     <li>Response body schema - does the response body adhere to the schema defined in the spec</li>
 * </ul>
 *
 */
public class SwaggerRequestResponseValidator {

    private final Swagger api;
    private final Optional<String> basePathOverride;

    private final RequestValidator requestValidator;
    private final ResponseValidator responseValidator;

    /**
     * Construct a new validator for the specification at the given URL.
     * <p>
     * The URL can be an absolute HTTP/HTTPS URL, a File URL or a classpath location (without the classpath: scheme).
     *
     * @param swaggerJsonUrl The location of the Swagger JSON specification to use in this validator
     *
     * @see SwaggerRequestResponseValidator#SwaggerRequestResponseValidator(String, String)
     */
    public SwaggerRequestResponseValidator(@Nonnull final String swaggerJsonUrl) {
        this(swaggerJsonUrl, null);
    }

    /**
     * Construct a new validator for the specification at the given URL.
     * <p>
     * The URL can be an absolute HTTP/HTTPS URL, a File URL or a classpath location (without the classpath: scheme).
     * <p>
     * This constructor also takes an optional basepath override to override the one defined in the Swagger spec.
     * This can be useful if e.g. your Swagger specification has been created for a public URL but you are validating
     * requests against an internal URL where the URL paths differ.
     * <p>
     * Example usage:
     * <pre>
     *     // Create from a publicly hosted HTTP location
     *     new SwaggerRequestResponseValidator("http://api.myservice.com/swagger.json", null);
     *
     *     // Create from a file on the local filesystem
     *     new SwaggerRequestResponseValidator("file://Users/myuser/tmp/swagger.json", null);
     *
     *     // Create from a classpath resource in the /api package
     *     // and override the basepath to "/testapi"
     *     new SwaggerRequestResponseValidator("/api/swagger.json", "/testapi");
     * </pre>
     *
     * @param swaggerJsonUrl The location of the Swagger JSON specification to use in this validator
     * @param basePathOverride (Optional) override for the base path defined in the Swagger specification.
     */
    public SwaggerRequestResponseValidator(@Nonnull final String swaggerJsonUrl,
                                           @Nullable final String basePathOverride) {

        requireNonNull(swaggerJsonUrl, "A Swagger URL is required");

        final SwaggerDeserializationResult swaggerParseResult =
                new SwaggerParser().readWithInfo(swaggerJsonUrl, null, true);
        this.api = swaggerParseResult.getSwagger();
        if (api == null) {
            throw new IllegalArgumentException(
                    format("Unable to load API descriptor from %s:\n\t%s",
                            swaggerJsonUrl, swaggerParseResult.getMessages().toString().replace("\n", "\n\t")));
        }
        this.basePathOverride = Optional.ofNullable(basePathOverride);

        final SchemaValidator schemaValidator = new SchemaValidator(api);
        this.requestValidator = new RequestValidator(schemaValidator);
        this.responseValidator = new ResponseValidator(schemaValidator);
    }

    /**
     * Validate the given request/response against the API.
     *
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

        final MutableValidationReport validationReport = new MutableValidationReport();

        final NormalisedPath requestPath = new ApiBasedNormalisedPath(request.getPath());

        final Optional<NormalisedPath> maybeApiPath = findMatchingApiPath(requestPath);
        if (!maybeApiPath.isPresent()) {
            return validationReport.addError("No API path found that matches request " + request.getPath());
        }

        final NormalisedPath apiPathString = maybeApiPath.get();
        final Path apiPath = api.getPath(apiPathString.original());

        final HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod().name());
        final Operation operation = apiPath.getOperationMap().get(httpMethod);
        if (operation == null) {
            return validationReport.addError(format("%s operation not allowed on path '%s'",
                    request.getMethod(), apiPathString.original()));
        }

        final ApiOperation apiOperation = new ApiOperation(apiPathString, apiPath, httpMethod, operation);

        return validationReport
                .merge(requestValidator.validateRequest(requestPath, request, apiOperation))
                .merge(responseValidator.validateResponse(response, apiOperation));
    }

    @Nonnull
    private Optional<NormalisedPath> findMatchingApiPath(@Nonnull final NormalisedPath requestPath) {
        return api.getPaths().keySet()
                .stream()
                .map(p -> (NormalisedPath) new ApiBasedNormalisedPath(p))
                .filter(p -> pathMatches(requestPath, p))
                .findFirst();
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
        public String part(int index) {
            return pathParts.get(index);
        }

        @Override
        public boolean isParam(int index) {
            final String part = part(index);
            return part.startsWith("{") && part.endsWith("}");
        }

        @Override
        @Nullable
        public String paramName(int index) {
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

        private String normalise(String requestPath) {
            if (basePathOverride.isPresent()) {
                requestPath = requestPath.replace(basePathOverride.get(), "");
            } else if (api.getBasePath() != null) {
                requestPath = requestPath.replace(api.getBasePath(), "");
            }
            if (!requestPath.startsWith("/")) {
                return "/" + requestPath;
            }
            return requestPath;
        }
    }
}
