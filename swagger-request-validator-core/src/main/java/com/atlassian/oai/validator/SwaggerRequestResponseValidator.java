package com.atlassian.oai.validator;

import au.com.dius.pact.model.OptionalBody;
import au.com.dius.pact.model.Request;
import au.com.dius.pact.model.Response;
import com.atlassian.oai.validator.parameter.ParameterValidators;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.parser.SwaggerParser;
import io.swagger.parser.util.SwaggerDeserializationResult;

import java.util.List;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

public class SwaggerRequestResponseValidator {

    private final Swagger api;
    private final Optional<String> basePathOverride;
    private final SwaggerSchemaValidator schemaValidator;


    public SwaggerRequestResponseValidator(final String swaggerJsonUrl, final String basePathOverride) {
        final SwaggerDeserializationResult swaggerParseResult = new SwaggerParser().readWithInfo(swaggerJsonUrl, null, true);
        this.api = swaggerParseResult.getSwagger();
        if (api == null) {
            throw new IllegalArgumentException("Unable to load API descriptor from " + swaggerJsonUrl);
        }
        this.basePathOverride = Optional.ofNullable(basePathOverride);
        this.schemaValidator = new SwaggerSchemaValidator(this.api);
    }

    public void validate(final Request request, final Response response) {
        final NormalisedPath requestPath = new NormalisedPath(request.getPath());

        final Optional<NormalisedPath> maybeApiPath = findMatchingApiPath(requestPath);
        if (!maybeApiPath.isPresent()) {
            throw new ValidationException("No API path found that matches request " + request.getPath());
        }

        final NormalisedPath apiPathString = maybeApiPath.get();
        final Path apiPath = api.getPath(apiPathString.original());

        final HttpMethod httpMethod = HttpMethod.valueOf(request.getMethod().toUpperCase());
        final Operation operation = apiPath.getOperationMap().get(httpMethod);
        if (operation == null) {
            throw new ValidationException(format("%s operation not allowed on path '%s'",
                    request.getMethod(), apiPathString.original()));
        }

        final ApiOperation apiOperation = new ApiOperation(apiPathString, apiPath, httpMethod, operation);

        validateRequest(apiOperation, request);
        validateResponse(apiOperation, response);
    }

    private void validateRequest(final ApiOperation apiOperation, final Request request) {

        // Check request parameters
        final NormalisedPath requestPath = new NormalisedPath(request.getPath());
        validateRequestParameters(apiOperation, requestPath);

        // Check request body
        validateRequestBody(apiOperation, request.getBody());
    }

    private void validateResponse(final ApiOperation apiOperation, final Response response) {

        final io.swagger.models.Response apiResponse = apiOperation.getOperation().getResponses().get(Integer.toString(response.getStatus()));
        if (apiResponse == null) {
            apiOperation.getOperation().getResponses().get("default"); // try the default response
        }

        if (apiResponse == null) {
            throw new ValidationException(format("Response status %d not defined for path '%s'",
                    response.getStatus(), apiOperation.getPathString().original()));
        }

        if (apiResponse.getSchema() == null) {
            return;
        }

        if (!response.getBody().isPresent()) {
            throw new ValidationException(
                    format("%s on path '%s' defines a response schema but no response body found",
                            apiOperation.getMethod(), apiOperation.getPathString().original())
            );
        }

        this.schemaValidator.validate(response.getBody().getValue(), apiResponse.getSchema());
    }

    private void validateRequestBody(final ApiOperation apiOperation, final OptionalBody body) {
        final Optional<Parameter> bodyParameter = apiOperation.getOperation().getParameters()
                .stream().filter(p -> p.getIn().equalsIgnoreCase("body")).findFirst();

        if (body.isPresent() && !bodyParameter.isPresent()) {
            throw new ValidationException(format("No request body is expected for %s on path '%s'",
                    apiOperation.getMethod(), apiOperation.getPathString().original()));
        }

        if (!bodyParameter.isPresent()) {
            return;
        }

        this.schemaValidator.validate(body.getValue(), ((BodyParameter)bodyParameter.get()).getSchema());
    }

    private void validateRequestParameters(final ApiOperation apiOperation, final NormalisedPath requestPath) {

        for (int i = 0; i < apiOperation.getPathString().parts().size(); i++) {
            final String part = apiOperation.getPathString().part(i);
            if (!isPathParameter(part)) {
                continue;
            }

            final String paramName = getParameterName(part);
            final String paramValue = requestPath.part(i);

            final Parameter parameter = apiOperation.getOperation().getParameters()
                    .stream()
                    .filter(p ->
                            p.getIn().equalsIgnoreCase("PATH") && p.getName().equalsIgnoreCase(paramName))
                    .findFirst()
                    .orElseThrow(() ->
                            new ValidationException(format("No path parameter %s found in API spec", paramName)));

            ParameterValidators.validate(paramValue, parameter);
        }
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

    static class ValidationException extends RuntimeException {
        ValidationException(String message) {
            super(message);
        }
    }

}
