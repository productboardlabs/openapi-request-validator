package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.ApiOperationMatch;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.Request;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

public class ApiOperationResolver {

    private final String apiPrefix;

    private final Map<Integer, List<NormalisedPath>> apiPathsGroupedByNumberOfParts;
    private final Table<String, HttpMethod, Operation> operations;

    /**
     * A utility for finding the best fitting API path.
     *
     * @param api              the Swagger API definition
     * @param basePathOverride (Optional) override for the base path defined in the Swagger specification.
     */
    public ApiOperationResolver(@Nonnull final Swagger api, @Nullable final String basePathOverride) {
        this.apiPrefix = Optional.ofNullable(basePathOverride).orElse(api.getBasePath());
        final Map<String, Path> apiPaths = Optional.ofNullable(api.getPaths())
            .orElse(Collections.emptyMap());

        // normalise all API paths and group them by their number of parts
        this.apiPathsGroupedByNumberOfParts = apiPaths.keySet().stream()
            .map(p -> new ApiBasedNormalisedPath(p, apiPrefix))
            .collect(Collectors.groupingBy(p -> p.numberOfParts()));

        // create a operation mapping for the API path and HTTP method
        this.operations = HashBasedTable.create();
        apiPaths.forEach((apiPath, apiPathsPath) ->
            apiPathsPath.getOperationMap().forEach((httpMethod, operation) ->
                operations.put(apiPath, httpMethod, operation))
        );
    }

    /**
     * Tries to find the best fitting API path matching the given path and request method.
     *
     * @param path   the requests path to find in API definition
     * @param method the {@link Request.Method} for the request
     * @return a {@link ApiOperationMatch} containing the information if the path is defined, the operation
     * is allowed and having the necessary {@link ApiOperation} if applicable
     */
    @Nonnull
    public ApiOperationMatch findApiOperation(@Nonnull final String path, @Nonnull final Request.Method method) {

        // try to find possible matching paths regardless of HTTP method
        final NormalisedPath requestPath = new ApiBasedNormalisedPath(path, apiPrefix);
        final List<NormalisedPath> possibleMatches = apiPathsGroupedByNumberOfParts
            .getOrDefault(requestPath.numberOfParts(), emptyList()).stream()
            .filter(p -> pathMatches(requestPath, p))
            .collect(Collectors.toList());

        if (possibleMatches.isEmpty()) {
            return ApiOperationMatch.MISSING_PATH;
        }

        // try to find the operation which fits the HTTP method
        final HttpMethod httpMethod = HttpMethod.valueOf(method.name());
        final Optional<NormalisedPath> pathOpt = possibleMatches.stream()
            .filter(apiPath -> operations.contains(apiPath.original(), httpMethod))
            .findFirst(); // if exists there can only be one path matching the path and method - overlapping paths+methods are not allowed

        return pathOpt
            .map(apiPath -> new ApiOperationMatch(new ApiOperation(apiPath, requestPath, httpMethod,
                operations.get(apiPath.original(), httpMethod))))
            .orElse(ApiOperationMatch.NOT_ALLOWED_OPERATION);
    }

    private static boolean pathMatches(@Nonnull final NormalisedPath requestPath,
                                       @Nonnull final NormalisedPath apiPath) {
        if (requestPath.numberOfParts() != apiPath.numberOfParts()) {
            return false;
        }
        for (int i = 0; i < requestPath.numberOfParts(); i++) {
            if (requestPath.part(i).equalsIgnoreCase(apiPath.part(i)) || apiPath.isParam(i)) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static class ApiBasedNormalisedPath implements NormalisedPath {

        private final List<String> pathParts;
        private final String original;
        private final String normalised;
        private final String apiPrefix;

        public ApiBasedNormalisedPath(@Nonnull final String path, @Nullable final String apiPrefix) {
            this.original = requireNonNull(path, "A path is required");
            this.apiPrefix = apiPrefix;
            this.normalised = normalise(path);
            this.pathParts = unmodifiableList(asList(normalised.split("/")));
        }

        @Override
        public int numberOfParts() {
            return pathParts.size();
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
            final String trimmedPath = trimPrefix(requestPath);
            if (!trimmedPath.startsWith("/")) {
                return "/" + requestPath;
            }
            return trimmedPath;
        }

        private String trimPrefix(@Nonnull final String requestPath) {
            if (apiPrefix == null || !requestPath.startsWith(apiPrefix)) {
                return requestPath;
            }
            return requestPath.substring(apiPrefix.length());
        }
    }
}
