package com.atlassian.oai.validator.security;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import static com.atlassian.oai.validator.report.ValidationReport.empty;
import static org.slf4j.LoggerFactory.getLogger;

public class SecurityValidator {

    private static final Logger log = getLogger(SecurityValidator.class);

    private static final String HTTP_AUTH_HEADER = "Authorization";

    private static final String MISSING_SECURITY_PARAMETER_KEY = "validation.request.security.missing";
    private static final String INVALID_SECURITY_PARAMETER_KEY = "validation.request.security.invalid";

    private final MessageResolver messages;
    private final OpenAPI oaiDefinition;

    public SecurityValidator(final MessageResolver messages, final OpenAPI oaiDefinition) {
        this.messages = messages;
        this.oaiDefinition = oaiDefinition;
    }

    @Nonnull
    public ValidationReport validateSecurity(final Request request,
                                             final ApiOperation apiOperation) {
        final List<SecurityRequirement> securityRequired = apiOperation.getOperation().getSecurity();

        if (null != securityRequired && !securityRequired.isEmpty()) {
            boolean foundSecurity = false;
            ValidationReport report = empty();
            for (final Map.Entry<String, SecurityScheme> s : oaiDefinition.getComponents().getSecuritySchemes().entrySet()) {
                final Map<String, SecurityScheme> filtered = new HashMap<>();
                securityRequired.stream().filter(item -> item.containsKey(s.getKey())).forEach(item -> filtered.put(s.getKey(), s.getValue()));

                if (!filtered.isEmpty()) {
                    final Set<String> missingDefinitions = new TreeSet<>();
                    final ValidationReport subReport = filtered.entrySet().stream().map(e -> {
                        final ValidationReport validationReport = validateSingleSecurityParameter(request, e.getValue());

                        if (validationReport.getMessages().stream().filter(m -> MISSING_SECURITY_PARAMETER_KEY.equals(m.getKey())).count() > 0) {
                            missingDefinitions.add(e.getKey());
                        }

                        return validationReport;
                    }).reduce(empty(), ValidationReport::merge);

                    if (missingDefinitions.isEmpty()) {
                        foundSecurity = true;
                        report = report.merge(subReport);
                    } else {
                        // do not append subReport if security definition of 's' is missing/incomplete
                        log.debug("Security definition not found for {}", s.getKey());
                    }
                }
            }

            if (!foundSecurity) {
                // none of security headers was found
                return ValidationReport.singleton(messages.get(MISSING_SECURITY_PARAMETER_KEY, request.getMethod(), request.getPath()));
            }

            return report;
        }
        
        return empty();
    }

    @Nonnull
    private ValidationReport validateSingleSecurityParameter(final Request request,
                                                             final SecurityScheme securityScheme) {
        switch (securityScheme.getType()) {
            case APIKEY:
                switch (securityScheme.getIn()) {
                    case HEADER:
                        return checkApiKeyAuthorizationByHeader(request, securityScheme);
                    case QUERY:
                        return checkApiKeyAuthorizationByQueryParameter(request, securityScheme);
                    default:
                        return empty();
                }
            case HTTP:
                return checkBasicAuthorization(request, securityScheme);
            default:
                return empty();
        }
    }

    @Nonnull
    private ValidationReport checkBasicAuthorization(final Request request,
                                                     final SecurityScheme securityScheme) {

        if (!request.getHeaderValue(HTTP_AUTH_HEADER).isPresent()) {
            return ValidationReport.singleton(messages.get(MISSING_SECURITY_PARAMETER_KEY, request.getMethod(), request.getPath()));
        } else if (!request.getHeaderValue(HTTP_AUTH_HEADER).get().startsWith("Basic ")) {
            // Authorization HTTP header found but not a Basic authentication token
            return ValidationReport.singleton(messages.get(INVALID_SECURITY_PARAMETER_KEY, request.getMethod(), request.getPath()));
        }
        // HTTP basic authentication header found, additional checks can be placed here
        return empty();
    }

    @Nonnull
    private ValidationReport checkApiKeyAuthorizationByQueryParameter(final Request request,
                                                                      final SecurityScheme securityScheme) {
        final Optional<String> authQueryParam = request.getQueryParameterValues(securityScheme.getName()).stream().findFirst();
        if (!authQueryParam.isPresent()) {
            return ValidationReport.singleton(messages.get(MISSING_SECURITY_PARAMETER_KEY, request.getMethod(), request.getPath()));
        }
        // API key query parameter found, additional checks can be placed here
        return empty();
    }

    @Nonnull
    private ValidationReport checkApiKeyAuthorizationByHeader(final Request request,
                                                              final SecurityScheme securityScheme) {

        if (!request.getHeaderValue(securityScheme.getName()).isPresent()) {
            return ValidationReport.singleton(messages.get(MISSING_SECURITY_PARAMETER_KEY, request.getMethod(), request.getPath()));
        }
        // API key header found, additional checks can be placed here
        return empty();
    }

}
