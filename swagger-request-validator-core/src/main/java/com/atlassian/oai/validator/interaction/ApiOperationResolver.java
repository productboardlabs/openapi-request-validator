package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.ApiOperationMatch;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.Request;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Table;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * Resolver responsible for matching an incoming request path + method with an operation defined in the OAI spec.
 * <p>
 */
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
        this.apiPrefix = ofNullable(basePathOverride).orElse(api.getBasePath());
        final Map<String, Path> apiPaths = ofNullable(api.getPaths()).orElse(emptyMap());

        // normalise all API paths and group them by their number of parts
        this.apiPathsGroupedByNumberOfParts = apiPaths.keySet().stream()
                .map(p -> new ApiBasedNormalisedPath(p, apiPrefix))
                .collect(groupingBy(NormalisedPath::numberOfParts));

        // create a operation mapping for the API path and HTTP method
        this.operations = HashBasedTable.create();
        apiPaths.forEach((pathKey, apiPath) ->
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

        // try to find possible matching paths regardless of HTTP method
        final NormalisedPath requestPath = new ApiBasedNormalisedPath(path, apiPrefix);
        final List<NormalisedPath> possibleMatches = apiPathsGroupedByNumberOfParts
                .getOrDefault(requestPath.numberOfParts(), emptyList()).stream()
                .filter(p -> pathMatches(requestPath, p))
                .collect(toList());

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
            if (requestPath.part(i).equalsIgnoreCase(apiPath.part(i)) || apiPath.hasParams(i)) {
                continue;
            }
            return false;
        }
        return true;
    }

    @VisibleForTesting
    static class ApiBasedNormalisedPath implements NormalisedPath {

        private static final String PARAM_REGEX = "\\{(.*?)}";
        private static final Pattern PARAM_PATTERN = compile(PARAM_REGEX);

        private static final char PARAM_START = '{';
        private static final char PARAM_END = '}';

        private final List<String> pathParts;
        private final String original;
        private final String normalised;
        private final String apiPrefix;

        public ApiBasedNormalisedPath(@Nonnull final String path, @Nullable final String apiPrefix) {
            this.original = requireNonNull(path, "A path is required");
            this.apiPrefix = apiPrefix;
            this.normalised = normalise(path);

            // We have normalized to start with a leading "/"; this will result in an empty path element
            this.pathParts = unmodifiableList(asList(normalised.substring(1).split("/")));
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
        public boolean hasParams(final int index) {
            final String part = part(index);
            return PARAM_PATTERN.matcher(part).find();
        }

        @Override
        public List<String> paramNames(final int index) {
            final String part = part(index);
            final Matcher matcher = PARAM_PATTERN.matcher(part);
            final List<String> result = new ArrayList<>();
            while (matcher.find()) {
                result.add(matcher.group(1));
            }
            return result;
        }

        @Override
        public Map<String, Optional<String>> paramValues(final int index, final String requestPathPart) {
            final List<String> paramNames = paramNames(index);
            if (paramNames.isEmpty()) {
                return emptyMap();
            }

            final String template = part(index);

            // This is a shortcut for the most common case where a single path param occupies the whole path part
            // e.g. /foo/{param}/bar
            // Avoids the need to scan strings etc.
            if (paramNames.size() == 1
                    && template.indexOf(PARAM_START) == 0
                    && template.indexOf(PARAM_END) == template.length() - 1) {
                return ImmutableMap.of(paramNames.get(0), of(requestPathPart));
            }

            // Using a scanning approach rather than regexes etc. because we want to get any matches
            // and then fill remaining with empties so we can validate on them later.
            // This is harder to do with regexes...

            final Map<String, Optional<String>> result = new HashMap<>();

            int templateScanner = 0;
            int requestScanner = 0;
            int paramValueStart;
            int paramIndex = 0;

            while (templateScanner < template.length() && requestScanner < requestPathPart.length()) {
                if (template.charAt(templateScanner) == PARAM_START) {
                    paramValueStart = requestScanner;

                    // Scan ahead in the template and find the terminal character
                    while (templateScanner < template.length() && template.charAt(templateScanner) != PARAM_END) {
                        templateScanner++;
                    }
                    if (templateScanner == template.length() || template.charAt(templateScanner) != PARAM_END) {
                        // We must have reached the end without finding a close char
                        break;
                    }
                    if (templateScanner == template.length() - 1) {
                        // Close char is the last char - value goes to end of string
                        result.put(paramNames.get(paramIndex++), Optional.of(requestPathPart.substring(paramValueStart)));
                        break;
                    }

                    final char terminal = template.charAt(++templateScanner);

                    // Scan ahead in the request to find the terminal char
                    while (requestScanner < requestPathPart.length() && requestPathPart.charAt(requestScanner) != terminal) {
                        requestScanner++;
                    }
                    if (requestPathPart.charAt(requestScanner) == terminal) {
                        // Found the terminal - construct the param value
                        result.put(paramNames.get(paramIndex++), Optional.of(requestPathPart.substring(paramValueStart, requestScanner)));
                    } else {
                        // Must have reached the end without finding a terminal - no match
                        break;
                    }
                } else {
                    if (template.charAt(templateScanner) != requestPathPart.charAt(requestScanner)) {
                        // Templates differ - no match
                        break;
                    }
                    templateScanner++;
                    requestScanner++;
                }
            }
            while (paramIndex < paramNames.size()) {
                result.put(paramNames.get(paramIndex++), empty());
            }
            return result;
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
