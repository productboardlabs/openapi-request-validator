package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.ApiOperationMatch;
import com.atlassian.oai.validator.model.ApiPath;
import com.atlassian.oai.validator.model.ApiPathImpl;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.NormalisedPathImpl;
import com.atlassian.oai.validator.model.Request;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component responsible for matching an incoming request path + method with an operation defined in the OAI spec.
 */
public class ApiOperationResolver {

    private static final Logger log = getLogger(ApiOperationResolver.class);

    private final String apiPrefix;

    private final Map<Integer, List<ApiPath>> apiPathsGroupedByNumberOfParts;
    private final Table<String, PathItem.HttpMethod, Operation> operations;

    /**
     * A utility for finding the best fitting API path.
     *
     * @param api the OpenAPI definition
     * @param basePathOverride (Optional) override for the base path defined in the OpenAPI specification.
     * @param strictPathMatching Enable strict path matching. If enabled, a trailing slash indicates a different path than without.
     */
    public ApiOperationResolver(final OpenAPI api,
                                @Nullable final String basePathOverride,
                                final boolean strictPathMatching) {

        apiPrefix = ofNullable(basePathOverride).orElse(getBasePathFrom(api.getServers()));
        final Paths apiPaths = ofNullable(api.getPaths()).orElse(new Paths());

        // normalise all API paths and group them by their number of parts
        apiPathsGroupedByNumberOfParts = apiPaths.keySet().stream()
                .map(p -> new ApiPathImpl(p, apiPrefix, strictPathMatching))
                .collect(groupingBy(NormalisedPath::numberOfParts));

        // create a operation mapping for the API path and HTTP method
        operations = HashBasedTable.create();
        apiPaths.forEach((pathKey, apiPath) ->
                apiPath.readOperationsMap().forEach((httpMethod, operation) ->
                        operations.put(pathKey, httpMethod, operation))
        );
    }

    /**
     * Tries to find the best fitting API path matching the given path and request method.
     *
     * @param path the requests path to find in API definition
     * @param method the {@link Request.Method} for the request
     *
     * @return a {@link ApiOperationMatch} containing the information if the path is defined, the operation
     * is allowed and having the necessary {@link ApiOperation} if applicable
     */
    @Nonnull
    public ApiOperationMatch findApiOperation(final String path, final Request.Method method) {

        // Try to find possible matching paths regardless of HTTP method
        final NormalisedPath requestPath = new NormalisedPathImpl(path, apiPrefix);
        final List<ApiPath> matchingPaths = apiPathsGroupedByNumberOfParts
                .getOrDefault(requestPath.numberOfParts(), emptyList()).stream()
                .filter(p -> p.matches(requestPath))
                .collect(toList());

        if (matchingPaths.isEmpty()) {
            return ApiOperationMatch.MISSING_PATH;
        }

        // Try to find the operation which fits the HTTP method,
        final PathItem.HttpMethod httpMethod = PathItem.HttpMethod.valueOf(method.name());
        final List<ApiPath> matchingPathAndMethod = matchingPaths.stream()
                .filter(apiPath -> operations.contains(apiPath.original(), httpMethod))
                .collect(toList());

        if (matchingPathAndMethod.isEmpty()) {
            return ApiOperationMatch.NOT_ALLOWED_OPERATION;
        }

        // Now look for exact matches first
        final Optional<ApiPath> exactMatch = matchingPathAndMethod.stream()
                .filter(apiPath -> apiPath.normalised().equalsIgnoreCase(requestPath.normalised()))
                .findFirst();

        if (exactMatch.isPresent()) {
            return new ApiOperationMatch(new ApiOperation(exactMatch.get(), requestPath, httpMethod, operations.get(exactMatch.get().original(), httpMethod)));
        }

        // Finally, use the specificity score to find the most likely match
        final Optional<ApiPath> scoredMatch = matchingPathAndMethod.stream()
                .max(comparingInt(ApiOperationResolver::specificityScore));

        return scoredMatch
                .map(match -> new ApiOperationMatch(new ApiOperation(match, requestPath, httpMethod, operations.get(match.original(), httpMethod))))
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
    private static int specificityScore(final ApiPath apiPath) {
        // Return the length of the path, with path vars counting as 1.
        return apiPath.normalised().replaceAll("\\{.+?}", "").length();
    }

    /**
     * Determine the 'base path' of the given API.
     * <p>
     * Returns the base path of the first server definition in the spec.
     *
     * @param servers The OpenAPI servers definition to get the base path from
     *
     * @return the base path of the first server definition found in the spec.
     */
    @VisibleForTesting
    @Nonnull
    static String getBasePathFrom(@Nullable final List<Server> servers) {
        if (servers == null) {
            return "/";
        }
        return servers.stream()
                .filter(Objects::nonNull)
                .map(ApiOperationResolver::substituteUrlVariables)
                .map(ApiOperationResolver::gePathFrom)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("/");
    }

    private static String gePathFrom(final String serverUrl) {
        try {
            return new URI(serverUrl).getPath();
        } catch (final URISyntaxException e) {
            log.debug("Server URL {} not a valid URI", serverUrl);
            return serverUrl;
        }
    }

    private static String substituteUrlVariables(final Server server) {
        String result = server.getUrl();
        if (result == null) {
            return "/";
        }
        if (server.getVariables() == null) {
            return result;
        }
        for (final String varName : server.getVariables().keySet()) {
            final String value = defaultIfBlank(server.getVariables().get(varName).getDefault(), "");
            result = result.replace(format("{%s}", varName), value);
        }
        return result;
    }

}
