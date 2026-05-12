package com.atlassian.oai.validator.interaction.request;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.atlassian.oai.validator.model.Headers.AUTHORIZATION;
import static com.atlassian.oai.validator.report.ValidationReport.empty;
import static com.atlassian.oai.validator.report.ValidationReport.singleton;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Validates security parameters on a request against the API definition.
 */
class SecurityValidator {

    private static final Logger log = getLogger(SecurityValidator.class);

    private static final String MISSING_SECURITY_PARAMETER_KEY = "validation.request.security.missing";
    private static final String INVALID_SECURITY_PARAMETER_KEY = "validation.request.security.invalid";

    private final MessageResolver messages;
    private final OpenAPI api;

    SecurityValidator(final MessageResolver messages, final OpenAPI api) {
        this.messages = messages;
        this.api = api;
    }

    @Nonnull
    public ValidationReport validateSecurity(final Request request,
                                             final ApiOperation apiOperation) {
        final List<SecurityRequirement> securityRequired = apiOperation.getOperation().getSecurity();

        if (securityRequired == null || securityRequired.isEmpty()) {
            return empty();
        }

        if (api.getComponents() == null || api.getComponents().getSecuritySchemes() == null) {
            log.warn("Operation '{} {}' defines a 'security' block but no 'securitySchemes' are defined",
                    apiOperation.getMethod().name(),
                    apiOperation.getApiPath().normalised());
            return empty();
        }

        // Each 'SecurityRequirement' in the list is an 'OR' - at least one needs to pass
        // The map within each 'SecurityRequirement' is an 'AND' - all must pass
        // See https://swagger.io/docs/specification/authentication/#multiple

        final List<ValidationReport> reports = securityRequired.stream()
                .map(requirement -> validateSecurityRequirement(request, requirement))
                .collect(toList());

        if (atLeastOneRequirementFulfilled(reports)) {
            return empty();
        }

        if (allSecurityRequirementsMissing(reports)) {
            return missingSecurityParameter(request);
        }

        return findMostFulfilledRequirement(reports).orElse(combineAllReports(reports));
    }

    @Nonnull
    private ValidationReport validateSecurityRequirement(final Request request, final SecurityRequirement requirement) {
        return requirement.keySet().stream()
                .map(schemeName -> {
                    final SecurityScheme scheme = api.getComponents().getSecuritySchemes().get(schemeName);
                    if (scheme == null) {
                        log.warn("Security scheme definition not found for {}", schemeName);
                    }
                    return scheme;
                })
                .filter(Objects::nonNull)
                .map(scheme -> validateSecurityScheme(request, scheme))
                .reduce(empty(), ValidationReport::merge);
    }

    @Nonnull
    private ValidationReport validateSecurityScheme(final Request request,
                                                    final SecurityScheme securityScheme) {
        switch (securityScheme.getType()) {
            case APIKEY:
                switch (securityScheme.getIn()) {
                    case HEADER:
                        return validateApiKeyAuthByHeader(request, securityScheme);
                    case QUERY:
                        return validateApiKeyAuthByQueryParameter(request, securityScheme);
                    case COOKIE:
                        return validateApiKeyAuthByCookie(request, securityScheme);
                    default:
                        return empty();
                }
            case HTTP:
                return validateHttpAuthorization(request, securityScheme);
            default:
                log.info("Security scheme '{}' not currently supported", securityScheme.getType());
                return empty();
        }
    }

    @Nonnull
    private ValidationReport validateHttpAuthorization(final Request request,
                                                       final SecurityScheme securityScheme) {
        if ("BASIC".equalsIgnoreCase(securityScheme.getScheme())) {
            return validateBasicAuthHeader(request);
        }

        if ("BEARER".equalsIgnoreCase(securityScheme.getScheme())) {
            return validateBearerAuthHeader(request);
        }

        return empty();
    }

    private ValidationReport validateBasicAuthHeader(final Request request) {
        return request
                .getHeaderValue(AUTHORIZATION)
                .map(header -> header.startsWith("Basic ") ? empty() : invalidSecurityParameter(request))
                .orElse(missingSecurityParameter(request));
    }

    private ValidationReport validateBearerAuthHeader(final Request request) {
        return request
                .getHeaderValue(AUTHORIZATION)
                .map(header -> header.startsWith("Bearer ") ? empty() : invalidSecurityParameter(request))
                .orElse(missingSecurityParameter(request));
    }

    @Nonnull
    private ValidationReport validateApiKeyAuthByQueryParameter(final Request request,
                                                                final SecurityScheme securityScheme) {
        final Optional<String> authQueryParam = request.getQueryParameterValues(securityScheme.getName()).stream().findFirst();
        if (!authQueryParam.isPresent()) {
            return missingSecurityParameter(request);
        }
        return empty();
    }

    @Nonnull
    private ValidationReport validateApiKeyAuthByHeader(final Request request,
                                                        final SecurityScheme securityScheme) {
        final Optional<String> headerValue = request.getHeaderValue(securityScheme.getName());
        if (!headerValue.isPresent() || headerValue.get().isEmpty()) {
            return missingSecurityParameter(request);
        }
        return empty();
    }

    @Nonnull
    private ValidationReport validateApiKeyAuthByCookie(final Request request,
                                                        final SecurityScheme securityScheme) {
        // See https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Cookie
        final Optional<String> cookieHeader = request.getHeaderValue("Cookie");
        final boolean cookieExists = cookieHeader
                .map(c -> c.split("; "))
                .map(cookies -> stream(cookies)
                        .filter(cookie -> cookie.toLowerCase().startsWith(securityScheme.getName().toLowerCase()))
                        .anyMatch(cookie -> !isBlank(substringAfter(cookie, "=")))
                )
                .orElse(false);
        if (!cookieExists) {
            return missingSecurityParameter(request);
        }
        return empty();
    }

    @Nonnull
    private ValidationReport missingSecurityParameter(final Request request) {
        return singleton(messages.get(MISSING_SECURITY_PARAMETER_KEY, request.getMethod(), request.getPath()));
    }

    @Nonnull
    private ValidationReport invalidSecurityParameter(final Request request) {
        return singleton(messages.get(INVALID_SECURITY_PARAMETER_KEY, request.getMethod(), request.getPath()));
    }

    @Nonnull
    private Optional<ValidationReport> findMostFulfilledRequirement(final List<ValidationReport> reports) {
        // Try to find a report that does not include a 'missing' param
        // Assume this means that all params were supplied, but that at least one was invalid in some way
        // Assume this was the requirement that was being attempted
        return reports.stream()
                .filter(r -> r.getMessages().stream().noneMatch(m -> MISSING_SECURITY_PARAMETER_KEY.equals(m.getKey())))
                .findFirst();
    }

    private boolean atLeastOneRequirementFulfilled(final List<ValidationReport> reports) {
        return reports.stream().anyMatch(r -> !r.hasErrors());
    }

    private boolean allSecurityRequirementsMissing(final List<ValidationReport> reports) {
        return reports.stream()
                .allMatch(r -> r.getMessages().stream().anyMatch(m -> MISSING_SECURITY_PARAMETER_KEY.equals(m.getKey())));
    }

    @Nonnull
    private ValidationReport combineAllReports(final List<ValidationReport> reports) {
        return reports.stream().reduce(empty(), ValidationReport::merge);
    }

}
