package com.atlassian.oai.validator;

import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.parameter.ParameterValidators;
import com.atlassian.oai.validator.report.MutableValidationReport;
import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;

import javax.annotation.Nonnull;
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
    private final SwaggerSchemaValidator schemaValidator;


    public SwaggerRequestResponseValidator(final String swaggerJsonUrl, final String basePathOverride) {
        final SwaggerDeserializationResult swaggerParseResult = new SwaggerParser().readWithInfo(swaggerJsonUrl, null, true);
        this.api = swaggerParseResult.getSwagger();
        if (api == null) {
            throw new IllegalArgumentException(
                    format("Unable to load API descriptor from %s:\n\t%s",
                            swaggerJsonUrl, swaggerParseResult.getMessages().toString().replace("\n", "\n\t")));
        }
        this.basePathOverride = Optional.ofNullable(basePathOverride);
        this.schemaValidator = new SwaggerSchemaValidator(this.api);
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
    public ValidationReport validate(@Nonnull final Request request, @Nonnull final Response response) {
        requireNonNull(request, "A request is required");
        requireNonNull(response, "A response is required");

        final MutableValidationReport validationReport = new MutableValidationReport();

        final NormalisedPath requestPath = new NormalisedPath(request.getPath());

        final Optional<NormalisedPath> maybeApiPath = findMatchingApiPath(requestPath);
        if (!maybeApiPath.isPresent()) {
            validationReport.addError("No API path found that matches request " + request.getPath());
            return validationReport;
        }

        final NormalisedPath apiPathString = maybeApiPath.get();
        final Path apiPath = api.getPath(apiPathString.original());

        final HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod().name());
        final Operation operation = apiPath.getOperationMap().get(httpMethod);
        if (operation == null) {
            validationReport.addError(format("%s operation not allowed on path '%s'",
                    request.getMethod(), apiPathString.original()));
            return validationReport;
        }

        final ApiOperation apiOperation = new ApiOperation(apiPathString, apiPath, httpMethod, operation);

        return validationReport
                .merge(validateRequest(apiOperation, request))
                .merge(validateResponse(apiOperation, response));
    }

    private ValidationReport validateRequest(final ApiOperation apiOperation, final Request request) {

        final NormalisedPath requestPath = new NormalisedPath(request.getPath());
        return ValidationReport.empty()
                .merge(validateRequestParameters(apiOperation, requestPath))
                .merge(validateRequestBody(apiOperation, request.getBody()));
    }

    private ValidationReport validateResponse(final ApiOperation apiOperation, final Response response) {

        io.swagger.models.Response apiResponse = apiOperation.getOperation().getResponses().get(Integer.toString(response.getStatus()));
        if (apiResponse == null) {
            apiResponse = apiOperation.getOperation().getResponses().get("default"); // try the default response
        }

        final MutableValidationReport validationReport = new MutableValidationReport();
        if (apiResponse == null) {
            validationReport.addError(format("Response status %d not defined for path '%s'",
                    response.getStatus(), apiOperation.getPathString().original()));
            return validationReport;
        }

        if (apiResponse.getSchema() == null) {
            return validationReport;
        }

        if (!response.getBody().isPresent()) {
            validationReport.addError(format("%s on path '%s' defines a response schema but no response body found",
                            apiOperation.getMethod(), apiOperation.getPathString().original()));
            return validationReport;
        }

        return validationReport.merge(schemaValidator.validate(response.getBody().get(), apiResponse.getSchema()));
    }

    private ValidationReport validateRequestBody(final ApiOperation apiOperation, final Optional<String> requestBody) {
        final Optional<Parameter> bodyParameter = apiOperation.getOperation().getParameters()
                .stream().filter(p -> p.getIn().equalsIgnoreCase("body")).findFirst();

        final MutableValidationReport validationReport = new MutableValidationReport();
        if (requestBody.isPresent() && !bodyParameter.isPresent()) {
            validationReport.addError(format("No request body is expected for %s on path '%s'",
                    apiOperation.getMethod(), apiOperation.getPathString().original()));
            return validationReport;
        }

        if (!bodyParameter.isPresent()) {
            return validationReport;
        }

        if (!requestBody.isPresent()) {
            if (bodyParameter.get().getRequired()) {
                validationReport.addError(format("%s on path '%s' requires a request body. None found.",
                        apiOperation.getMethod(), apiOperation.getPathString().original()));
            }
            return validationReport;
        }

        return validationReport
                .merge(schemaValidator.validate(requestBody.get(), ((BodyParameter)bodyParameter.get()).getSchema()));
    }

    private ValidationReport validateRequestParameters(final ApiOperation apiOperation, final NormalisedPath requestPath) {

        ValidationReport validationReport = ValidationReport.empty();
        for (int i = 0; i < apiOperation.getPathString().parts().size(); i++) {
            final String part = apiOperation.getPathString().part(i);
            if (!isPathParameter(part)) {
                continue;
            }

            final String paramName = getParameterName(part);
            final String paramValue = requestPath.part(i);

            final Optional<Parameter> parameter = apiOperation.getOperation().getParameters()
                    .stream()
                    .filter(p ->
                            p.getIn().equalsIgnoreCase("PATH") && p.getName().equalsIgnoreCase(paramName))
                    .findFirst();

            if (parameter.isPresent()) {
                validationReport = validationReport.merge(ParameterValidators.validate(paramValue, parameter.get()));
            }
        }
        return validationReport;
    }

    private Optional<NormalisedPath> findMatchingApiPath(final NormalisedPath requestPath) {
        return api.getPaths().keySet()
                .stream()
                .map(NormalisedPath::new)
                .filter(p -> pathMatches(requestPath, p))
                .findFirst();
    }

    private boolean pathMatches(final NormalisedPath requestPath, final NormalisedPath apiPath) {
        if (requestPath.parts().size() != apiPath.parts().size()) {
            return false;
        }
        for (int i = 0; i < requestPath.parts().size(); i++) {
            if (requestPath.part(i).equalsIgnoreCase(apiPath.part(i)) ||
                    isPathParameter(apiPath.part(i))) {
                continue;
            }
            return false;
        }
        return true;
    }

    private boolean isPathParameter(final String s) {
        return s.startsWith("{") && s.endsWith("}");
    }

    private String getParameterName(final String pathParam) {
        return pathParam.substring(1, pathParam.length() - 1);
    }

    private class NormalisedPath {
        private final List<String> pathParts;
        private final String original;
        private final String normalised;

        NormalisedPath(final String path) {
            this.original = path;
            this.normalised = normalise(path);
            this.pathParts = unmodifiableList(asList(normalised.split("/")));
        }

        List<String> parts() {
            return pathParts;
        }

        String part(int index) {
            return pathParts.get(index);
        }

        String original() {
            return original;
        }

        String normalised() {
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

    private class ApiOperation {
        private final NormalisedPath pathString;
        private final Path pathObject;
        private final HttpMethod method;
        private final Operation operation;

        ApiOperation(NormalisedPath pathString, Path pathObject, HttpMethod method, Operation operation) {
            this.pathString = pathString;
            this.pathObject = pathObject;
            this.method = method;
            this.operation = operation;
        }

        NormalisedPath getPathString() {
            return pathString;
        }

        Path getPathObject() {
            return pathObject;
        }

        HttpMethod getMethod() {
            return method;
        }

        Operation getOperation() {
            return operation;
        }
    }

}
