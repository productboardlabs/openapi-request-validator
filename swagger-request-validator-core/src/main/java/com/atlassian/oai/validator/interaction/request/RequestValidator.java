package com.atlassian.oai.validator.interaction.request;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Headers;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.parameter.ParameterValidators;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.MessageContext;
import com.atlassian.oai.validator.schema.SchemaValidator;
import com.google.common.net.MediaType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.atlassian.oai.validator.report.ValidationReport.MessageContext.Location.REQUEST;
import static com.atlassian.oai.validator.report.ValidationReport.empty;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Validate a request against a given API operation.
 */
public class RequestValidator {

    private static final Logger log = getLogger(RequestValidator.class);

    private final MessageResolver messages;

    private final ParameterValidators parameterValidators;
    private final SecurityValidator securityValidator;
    private final RequestBodyValidator requestBodyValidator;

    /**
     * Construct a new request validator with the given schema validator.
     *
     * @param schemaValidator The schema validator to use when validating request bodies
     * @param messages The message resolver to use
     * @param api The OpenAPI spec to validate against
     */
    public RequestValidator(final SchemaValidator schemaValidator,
                            final MessageResolver messages,
                            final OpenAPI api) {
        this.messages = requireNonNull(messages, "A message resolver is required");

        parameterValidators = new ParameterValidators(schemaValidator, messages);
        securityValidator = new SecurityValidator(messages, api);
        requestBodyValidator = new RequestBodyValidator(messages, schemaValidator);
    }

    /**
     * Validate the request against the given API operation
     *
     * @param request The request to validate
     * @param apiOperation The operation to validate the request against
     *
     * @return A validation report containing validation errors
     */
    @Nonnull
    public ValidationReport validateRequest(final Request request,
                                            final ApiOperation apiOperation) {
        requireNonNull(request, "A request is required");
        requireNonNull(apiOperation, "An API operation is required");

        final MessageContext context = MessageContext.create()
                .in(REQUEST)
                .withApiOperation(apiOperation)
                .withRequestPath(apiOperation.getRequestPath().original())
                .withRequestMethod(request.getMethod())
                .build();

        return securityValidator.validateSecurity(request, apiOperation)
                .merge(validateContentType(request, apiOperation))
                .merge(validateAccepts(request, apiOperation))
                .merge(validateHeaders(request, apiOperation))
                .merge(validatePathParameters(apiOperation))
                .merge(requestBodyValidator.validateRequestBody(request, apiOperation.getOperation().getRequestBody()))
                .merge(validateQueryParameters(request, apiOperation))
                .withAdditionalContext(context);
    }

    @Nonnull
    private ValidationReport validateContentType(final Request request,
                                                 final ApiOperation apiOperation) {
        return validateMediaTypes(request,
                Headers.CONTENT_TYPE,
                getConsumes(apiOperation),
                "validation.request.contentType.invalid",
                "validation.request.contentType.notAllowed");
    }

    @Nonnull
    private ValidationReport validateAccepts(final Request request,
                                             final ApiOperation apiOperation) {
        return validateMediaTypes(request,
                Headers.ACCEPT,
                getProduces(apiOperation),
                "validation.request.accept.invalid",
                "validation.request.accept.notAllowed");
    }

    @Nonnull
    private ValidationReport validateMediaTypes(final Request request,
                                                final String headerName,
                                                final Collection<String> specMediaTypes,
                                                final String invalidTypeKey,
                                                final String notAllowedKey) {

        final Collection<String> requestHeaderValues = request.getHeaderValues(headerName);
        if (requestHeaderValues.isEmpty()) {
            return empty();
        }

        final List<MediaType> requestMediaTypes = new ArrayList<>();
        for (final String requestHeaderValue : requestHeaderValues) {
            try {
                requestMediaTypes.add(MediaType.parse(requestHeaderValue));
            } catch (final IllegalArgumentException e) {
                return ValidationReport.singleton(messages.get(invalidTypeKey, requestHeaderValue));
            }
        }

        if (specMediaTypes.isEmpty()) {
            return empty();
        }

        return specMediaTypes
                .stream()
                .map(MediaType::parse)
                .filter(specType ->
                        requestMediaTypes.stream()
                                .anyMatch(requestType ->
                                        specType.withoutParameters().is(requestType.withoutParameters())
                                )
                )
                .findFirst()
                .map(m -> empty())
                .orElse(ValidationReport.singleton(messages.get(notAllowedKey, requestHeaderValues, specMediaTypes)));
    }

    @Nonnull
    private Collection<String> getConsumes(final ApiOperation apiOperation) {
        if (apiOperation.getOperation().getRequestBody() == null) {
            return emptyList();
        }
        return defaultIfNull(apiOperation.getOperation().getRequestBody().getContent().keySet(), emptySet());
    }

    @Nonnull
    private Collection<String> getProduces(final ApiOperation apiOperation) {
        return apiOperation.getOperation()
                .getResponses()
                .values()
                .stream()
                .filter(apiResponse -> apiResponse.getContent() != null)
                .flatMap(apiResponse -> apiResponse.getContent().keySet().stream())
                .collect(Collectors.toSet());
    }

    @Nonnull
    private ValidationReport validatePathParameters(final ApiOperation apiOperation) {

        ValidationReport validationReport = empty();
        final NormalisedPath requestPath = apiOperation.getRequestPath();
        for (int i = 0; i < apiOperation.getApiPath().numberOfParts(); i++) {
            if (!apiOperation.getApiPath().hasParams(i)) {
                continue;
            }

            final ValidationReport pathPartValidation = apiOperation
                    .getApiPath()
                    .paramValues(i, requestPath.part(i))
                    .entrySet()
                    .stream()
                    .map(param -> validatePathParameter(apiOperation, param.getKey(), param.getValue()))
                    .reduce(empty(), ValidationReport::merge);

            validationReport = validationReport.merge(pathPartValidation);
        }
        return validationReport;
    }

    @Nonnull
    private ValidationReport validatePathParameter(final ApiOperation apiOperation,
                                                   final String paramName,
                                                   final Optional<String> paramValue) {
        return defaultIfNull(apiOperation.getOperation().getParameters(), Collections.<Parameter>emptyList())
                .stream()
                .filter(RequestValidator::isPathParam)
                .filter(p -> p.getName().equalsIgnoreCase(paramName))
                .findFirst()
                .map(p -> parameterValidators.validate(paramValue.orElse(null), p))
                .orElse(empty());
    }

    @Nonnull
    private ValidationReport validateQueryParameters(final Request request,
                                                     final ApiOperation apiOperation) {
        return defaultIfNull(apiOperation.getOperation().getParameters(), Collections.<Parameter>emptyList())
                .stream()
                .filter(RequestValidator::isQueryParam)
                .map(p -> validateParameter(
                        apiOperation, p,
                        request.getQueryParameterValues(p.getName()),
                        "validation.request.parameter.query.missing")
                )
                .reduce(empty(), ValidationReport::merge);
    }

    @Nonnull
    private ValidationReport validateHeaders(final Request request,
                                             final ApiOperation apiOperation) {
        return defaultIfNull(apiOperation.getOperation().getParameters(), Collections.<Parameter>emptyList())
                .stream()
                .filter(RequestValidator::isHeaderParam)
                .map(p -> validateParameter(
                        apiOperation, p,
                        request.getHeaderValues(p.getName()),
                        "validation.request.parameter.header.missing")
                )
                .reduce(empty(), ValidationReport::merge);
    }

    @Nonnull
    private ValidationReport validateParameter(final ApiOperation apiOperation,
                                               final Parameter parameter,
                                               final Collection<String> parameterValues,
                                               final String missingKey) {

        if (parameterValues.isEmpty() && TRUE.equals(parameter.getRequired())) {
            return ValidationReport.singleton(
                    messages.get(missingKey, parameter.getName(), apiOperation.getApiPath().original())
            );
        }

        return parameterValues
                .stream()
                .map(v -> parameterValidators.validate(v, parameter))
                .reduce(empty(), ValidationReport::merge);
    }

    private static boolean isPathParam(final Parameter p) {
        return isParam(p, "path");
    }

    private static boolean isQueryParam(final Parameter p) {
        return isParam(p, "query");
    }

    private static boolean isHeaderParam(final Parameter p) {
        return isParam(p, "header");
    }

    private static boolean isParam(final Parameter p, final String type) {
        return p != null && p.getIn() != null && p.getIn().equalsIgnoreCase(type);
    }
}
