package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.NormalisedPath;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

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
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.regex.Pattern.compile;

@VisibleForTesting
class ApiBasedNormalisedPath implements NormalisedPath {

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
