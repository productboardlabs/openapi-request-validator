package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.ApiOperationMatch;
import com.atlassian.oai.validator.model.ApiPath;
import com.atlassian.oai.validator.model.ApiPathImpl;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.NormalisedPathImpl;
import com.atlassian.oai.validator.model.Request;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Comparator.comparingInt;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import java.util.function.BiPredicate;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Component responsible for matching an incoming request path + method with an operation defined in the OAI spec.
 */
public class ApiOperationResolver {

    private final String apiPrefix;

    private final List<ApiPath> apiPaths;
    private final Map<Integer, List<ApiPath>> apiPathsGroupedByNumberOfParts;
    private final Table<String, HttpMethod, Operation> operations;

    /**
     * A utility for finding the best fitting API path.
     *
     * @param api              the Swagger API definition
     * @param basePathOverride (Optional) override for the base path defined in the Swagger specification.
     */
    public ApiOperationResolver(@Nonnull final Swagger api, @Nullable final String basePathOverride) {
        apiPrefix = ofNullable(basePathOverride).orElse(api.getBasePath());
        final Map<String, Path> apiPathsByURI = ofNullable(api.getPaths()).orElse(emptyMap());

        apiPaths = apiPathsByURI.keySet().stream()
                .map(p -> new ApiPathImpl(p, apiPrefix))
                .collect(toList());
        // normalise all API paths and group them by their number of parts
        apiPathsGroupedByNumberOfParts = apiPaths
                .stream()
                .collect(groupingBy(NormalisedPath::numberOfParts));

        // create a operation mapping for the API path and HTTP method
        operations = HashBasedTable.create();
        apiPathsByURI.forEach((pathKey, apiPath) ->
                apiPath.getOperationMap().forEach((httpMethod, operation) ->
                        operations.put(pathKey, httpMethod, operation))
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
        final NormalisedPath requestPath = new NormalisedPathImpl(path, apiPrefix);
        //When using the standard path matching algorithm we can optimize by only considering paths with a matching number of parts
        final Stream<ApiPath> pathsWithMatchingNumberOfParts = apiPathsGroupedByNumberOfParts.getOrDefault(requestPath.numberOfParts(), emptyList()).stream();
        return findApiOperation(requestPath, method, ApiPath::matches, pathsWithMatchingNumberOfParts);
    }

    /**
     * Tries to find the best fitting API path matching the given path and request method, given a custom path
     * matching method.
     *
     * @param path   the requests path to find in API definition
     * @param method the {@link Request.Method} for the request
     * @param matcher a function that can compare an API path from a specification to a request path to see if they match, e.g. {@code ApiPath::matches}
     * @return a {@link ApiOperationMatch} containing the information if the path is defined, the operation
     * is allowed and having the necessary {@link ApiOperation} if applicable
     */
    @Nonnull
    public ApiOperationMatch findApiOperation(@Nonnull final String path, @Nonnull final Request.Method method,
            @Nonnull final BiPredicate<ApiPath, NormalisedPath> matcher) {
        final NormalisedPath requestPath = new NormalisedPathImpl(path, apiPrefix);
        return findApiOperation(requestPath, method, matcher, apiPaths.stream());
    }

    @Nonnull
    private ApiOperationMatch findApiOperation(@Nonnull final NormalisedPath requestPath, @Nonnull final Request.Method method,
            @Nonnull final BiPredicate<ApiPath, NormalisedPath> matcher, @Nonnull final Stream<ApiPath> paths) {
        // try to find possible matching paths regardless of HTTP method
        final List<ApiPath> matchingPaths = paths
                .filter(apiPath -> matcher.test(apiPath, requestPath))
                .collect(toList());

        if (matchingPaths.isEmpty()) {
            return ApiOperationMatch.MISSING_PATH;
        }

        // try to find the operation which fits the HTTP method,
        // choosing the most 'specific' path match from the candidates
        final HttpMethod httpMethod = HttpMethod.valueOf(method.name());
        final Optional<ApiPath> matchingPathAndOperation = matchingPaths.stream()
                .filter(apiPath -> operations.contains(apiPath.original(), httpMethod))
                .max(comparingInt(ApiOperationResolver::specificityScore));

        return matchingPathAndOperation
                .map(match ->
                        new ApiOperationMatch(new ApiOperation(match, requestPath, httpMethod, operations.get(match.original(), httpMethod))))
                .orElse(ApiOperationMatch.NOT_ALLOWED_OPERATION);
    }

    /**
     * Get the 'specificity' score of the provided API path. This is used when selecting an API operation to validate against -
     * where an incoming request matches multiple paths the 'most specific' one should win.
     * <p>
     * Note: This score is essentially meaningless across different paths - it should only be used to differentiate paths
     * that could be equivalent. For example, '{@code /{id}}' and '{@code /{id}.json}' could both match an incoming request on path
     * '{@code /foo.json}'; in that case we should match on '{@code /{id}.json}' as it is the most 'specific' match.
     *
     * @return a score >= 0 that indicates how 'specific' the path definition is. Higher numbers indicate more specific
     * definitions (e.g. fewer path variables).
     */
    private static int specificityScore(@Nonnull final ApiPath apiPath) {
        // Return the length of the path, with path vars counting as 1.
        return apiPath.normalised().replaceAll("\\{.+?}", "").length();
    }

}
