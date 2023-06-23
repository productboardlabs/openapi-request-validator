package com.atlassian.oai.validator.interaction.request;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Headers;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.MessageContext;
import com.atlassian.oai.validator.schema.SchemaValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Joiner;
import com.google.common.net.MediaType;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.Parameter.StyleEnum;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.atlassian.oai.validator.report.ValidationReport.MessageContext.Location.REQUEST;
import static com.atlassian.oai.validator.report.ValidationReport.empty;
import static com.atlassian.oai.validator.util.HttpAcceptUtils.splitAcceptHeader;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Validate a request against a given API operation.
 */
public class RequestValidator {

    private static final Logger log = getLogger(RequestValidator.class);

    private final MessageResolver messages;
    private final Components components;

    private final ParameterValidator parameterValidator;
    private final SecurityValidator securityValidator;
    private final RequestBodyValidator requestBodyValidator;
    private final List<CustomRequestValidator> customRequestValidators;

    /**
     * Construct a new request validator with the given schema validator.
     *
     * @param schemaValidator The schema validator to use when validating request bodies
     * @param messages The message resolver to use
     * @param api The OpenAPI spec to validate against
     * @param customRequestValidators The list of custom validators to run
     */
    public RequestValidator(final SchemaValidator schemaValidator,
                            final MessageResolver messages,
                            final OpenAPI api,
                            final List<CustomRequestValidator> customRequestValidators) {
        this.messages = requireNonNull(messages, "A message resolver is required");
        this.components = defaultIfNull(api.getComponents(), new Components());

        this.customRequestValidators = customRequestValidators;

        parameterValidator = new ParameterValidator(schemaValidator, messages);
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
                .merge(validateExplodedQueryParameters(request, apiOperation))
                .merge(validateDeepObjectQueryParameters(request, apiOperation))
                .merge(validateUnexpectedQueryParameters(request, apiOperation))
                .merge(validateCookieParameters(request, apiOperation))
                .merge(validateCustom(request, apiOperation))
                .withAdditionalContext(context);
    }

    @Nonnull
    private ValidationReport validateContentType(final Request request,
                                                 final ApiOperation apiOperation) {
        return validateMediaTypes(request,
                Headers.CONTENT_TYPE,
                getConsumes(apiOperation),
                "validation.request.contentType.invalid",
                "validation.request.contentType.notAllowed",
                // For content types we expect the wildcards to appear in the spec and concrete types to appear on the request
                (specType, contentType) -> contentType.withoutParameters().is(specType.withoutParameters()));
    }

    @Nonnull
    private ValidationReport validateAccepts(final Request request,
                                             final ApiOperation apiOperation) {
        return validateMediaTypes(request,
                Headers.ACCEPT,
                getProduces(apiOperation),
                "validation.request.accept.invalid",
                "validation.request.accept.notAllowed",
                // For accept types we expect the wildcards to appear in the accept header and concrete types to appear in the spec
                (specType, acceptType) -> specType.withoutParameters().is(acceptType.withoutParameters()));
    }

    @Nonnull
    private ValidationReport validateMediaTypes(final Request request,
                                                final String headerName,
                                                final Collection<String> specMediaTypes,
                                                final String invalidTypeKey,
                                                final String notAllowedKey,
                                                final BiPredicate<MediaType, MediaType> typeComparer) {

        // Handle the case where multiple media types are supplied in a single header
        final Collection<String> requestHeaderValues = request.getHeaderValues(headerName)
                .stream()
                .flatMap(v -> splitAcceptHeader(v).stream())
                .collect(toList());

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

        if (specMediaTypes
                .stream()
                .allMatch("*/*"::equals)) {
            return empty();
        }

        return specMediaTypes
                .stream()
                .map(MediaType::parse)
                .filter(specType -> requestMediaTypes.stream().anyMatch(requestType -> typeComparer.test(specType, requestType)))
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
                .map(p -> parameterValidator.validate(paramValue.orElse(null), p))
                .orElse(empty());
    }

    @Nonnull
    private ValidationReport validateQueryParameters(final Request request,
                                                     final ApiOperation apiOperation) {

        return defaultIfNull(apiOperation.getOperation().getParameters(), Collections.<Parameter>emptyList())
                .stream()
                .filter(p -> isQueryParam(p) && !isDeepObjectParam(p) && !isExplodedParamWithProperties(p))
                .map(p -> validateParameter(
                        apiOperation,
                        p,
                        request.getQueryParameterValues(p.getName()),
                        "validation.request.parameter.query.missing"))
                .reduce(empty(), ValidationReport::merge);
    }

    @Nonnull
    private ValidationReport validateExplodedQueryParameters(final Request request,
                                                             final ApiOperation apiOperation) {

        return defaultIfNull(apiOperation.getOperation().getParameters(), Collections.<Parameter>emptyList())
                .stream()
                .filter(p -> isQueryParam(p) && !isDeepObjectParam(p) && isExplodedParamWithProperties(p))
                .flatMap(p -> {
                    final Map<String, Schema<?>> properties = p.getSchema().getProperties();
                    final List<QueryParameter> explodedQueryParameters = properties.entrySet()
                            .stream()
                            .map(e -> {
                                final Schema<?> schema = e.getValue();
                                final QueryParameter parameter = new QueryParameter();
                                parameter.set$ref(schema.get$ref());
                                parameter.name(e.getKey());
                                parameter.setDescription(schema.getDescription());
                                parameter.setRequired(isRequired(p, e.getKey()));
                                parameter.setSchema(schema);
                                parameter.setIn(p.getIn());
                                parameter.setExample(schema.getExample());
                                parameter.setDeprecated(schema.getDeprecated());
                                parameter.setStyle(p.getStyle());
                                parameter.setExplode("array".equals(schema.getType()));
                                parameter.setExtensions(schema.getExtensions());

                                return parameter;
                            })
                            .collect(toList());

                    if (!TRUE.equals(p.getRequired()) && isNoExplodedQueryParameterProvided(request, explodedQueryParameters)) {
                        return Stream.empty();
                    }

                    return explodedQueryParameters.stream();
                })
                .map(p -> validateParameter(
                        apiOperation,
                        p,
                        request.getQueryParameterValues(p.getName()),
                        "validation.request.parameter.query.missing"))
                .reduce(empty(), ValidationReport::merge);
    }

    private boolean isNoExplodedQueryParameterProvided(final Request request, final List<QueryParameter> explodedQueryParameters) {
        return explodedQueryParameters.stream()
                .allMatch(queryParameter -> request.getQueryParameterValues(queryParameter.getName()).isEmpty());
    }

    private boolean isRequired(final Parameter parameter, final String propertyName) {
        return Optional.ofNullable(parameter.getSchema())
                .map(Schema::getRequired)
                .map(required -> required.contains(propertyName))
                .orElse(false);
    }

    @Nonnull
    private ValidationReport validateDeepObjectQueryParameters(final Request request,
                                                               final ApiOperation apiOperation) {
        return defaultIfNull(apiOperation.getOperation().getParameters(), Collections.<Parameter>emptyList())
                .stream()
                .filter(p -> isQueryParam(p) && isDeepObjectParam(p))
                .map(p -> validateDeepObjectQueryParameter(request, apiOperation, p))
                .reduce(empty(), ValidationReport::merge);
    }

    @Nonnull
    private ValidationReport validateDeepObjectQueryParameter(final Request request,
                                                              final ApiOperation apiOperation,
                                                              final Parameter parameter) {
        final String queryParam = parameter.getName();
        final Pattern fieldPattern = Pattern.compile(String.format("%s\\[(\\S*)\\]", queryParam));
        final Map<String, String> deepObject = new HashMap<>();

        request.getQueryParameters()
                .stream()
                .map(qp -> fieldPattern.matcher(qp))
                .filter(matcher -> matcher.matches())
                .forEach(matcher -> deepObject.putIfAbsent(
                        matcher.group(1),
                        request.getQueryParameterValues(matcher.group(0)).iterator().next()));

        // We need to handle where the parameter is not required, and there aren't any values
        if (deepObject.isEmpty() && !TRUE.equals(parameter.getRequired())) {
            return empty();
        }

        // It's possible that the values cause an error writing to a json string
        final String deepObjectAsJson;
        try {
            deepObjectAsJson = Json.mapper().writeValueAsString(deepObject);
        } catch (final JsonProcessingException e) {
            final ValidationReport.MessageContext context = ValidationReport.MessageContext.create()
                    .withApiOperation(apiOperation)
                    .withParameter(parameter)
                    .build();

            return ValidationReport.singleton(
                    messages.get("validation.request.parameter.query.unexpected", queryParam,
                            apiOperation.getApiPath().original())).withAdditionalContext(context);
        }

        return validateParameter(
                apiOperation, parameter, singletonList(deepObjectAsJson),
                "validation.request.parameter.query.missing"
        );
    }

    @Nonnull
    private ValidationReport validateUnexpectedQueryParameters(final Request request,
                                                               final ApiOperation apiOperation) {

        final Set<String> allowedQueryParams =
                Stream.of(defaultIfNull(apiOperation.getOperation().getParameters(),
                                        Collections.<Parameter>emptyList())
                                        .stream()
                                        .filter(p -> isQueryParam(p) && (isDeepObjectParam(p) || !isExplodedParamWithProperties(p)))
                                        .map(Parameter::getName),
                                (Stream<String>) defaultIfNull(apiOperation.getOperation().getParameters(),
                                        Collections.<Parameter>emptyList())
                                        .stream()
                                        .filter(p -> isQueryParam(p) && !isDeepObjectParam(p) && isExplodedParamWithProperties(p))
                                        .flatMap(p -> p.getSchema().getProperties().keySet().stream()),
                                defaultIfNull(components.getSecuritySchemes(),
                                        Collections.<String, SecurityScheme>emptyMap()).values().stream()
                                        .filter(sc -> sc.getIn() != null && sc.getIn() == SecurityScheme.In.QUERY)
                                        .map(SecurityScheme::getName)
                        ).flatMap(s -> s)
                        .collect(Collectors.toSet());

        return request.getQueryParameters().stream()
                .map(queryParam -> validateUnexpectedQueryParameter(allowedQueryParams, queryParam, apiOperation))
                .reduce(empty(), ValidationReport::merge);
    }

    private Boolean isExplodedParamWithProperties(final Parameter parameter) {
        return parameter.getExplode() != null && parameter.getExplode() && parameter.getSchema() != null
                && parameter.getSchema().getProperties() != null && !parameter.getSchema().getProperties().isEmpty();
    }

    @Nonnull
    private ValidationReport validateUnexpectedQueryParameter(final Set<String> allowedQueryParameters,
                                                              final String queryParam,
                                                              final ApiOperation apiOperation) {

        if (allowedQueryParameters.contains(queryParam)) {
            return empty();
        }

        // Allow through any deepObject formatted parameters - i.e 'filter[name_eq]'
        if (allowedQueryParameters.stream().anyMatch(p -> Pattern.matches(String.format("%s\\[(\\S*)\\]", p), queryParam))) {
            return empty();
        }

        final ValidationReport.MessageContext context =
                ValidationReport.MessageContext.create()
                        .withApiOperation(apiOperation)
                        .withParameter(new Parameter().name(queryParam).in("query"))
                        .build();

        return ValidationReport.singleton(
                messages.get("validation.request.parameter.query.unexpected", queryParam,
                        apiOperation.getApiPath().original())).withAdditionalContext(context);
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
    private ValidationReport validateCookieParameters(final Request request,
                                                      final ApiOperation apiOperation) {
        final Map<String, Collection<String>> cookieParams = getCookieParameterValues(request);
        return defaultIfNull(apiOperation.getOperation().getParameters(), Collections.<Parameter>emptyList())
                .stream()
                .filter(RequestValidator::isCookieParam)
                .map(p -> validateParameter(
                        apiOperation, p,
                        defaultIfNull(cookieParams.get(p.getName()), Collections.<String>emptyList()),
                        "validation.request.parameter.cookie.missing")
                )
                .reduce(empty(), ValidationReport::merge);
    }

    private Map<String, Collection<String>> getCookieParameterValues(final Request request) {
        final Map<String, Collection<String>> paramsMap = new HashMap<>();
        // SimpleRequest will split the header value with ',' by default, so here we join
        // the split values to get back original header value string
        final Collection<String> cookieValues = request.getHeaderValues("Cookie");
        if (!cookieValues.isEmpty()) {
            final String cookieValuesStr = Joiner.on(",").join(cookieValues);
            // cookie list are separated by a semicolon and a space ('; ')
            final String[] cookieValuesArray = cookieValuesStr.split("; ");
            for (final String cookieVal : cookieValuesArray) {
                // look for the first '='
                final int index = cookieVal.indexOf('=');
                if (index > 0) {
                    final String name = cookieVal.substring(0, index);
                    // skip '='
                    final String value = cookieVal.substring(index + 1);
                    paramsMap.putIfAbsent(name, new ArrayList<>());
                    paramsMap.get(name).add(value);
                }
            }
        }
        return paramsMap;
    }

    @Nonnull
    private ValidationReport validateParameter(final ApiOperation apiOperation,
                                               final Parameter parameter,
                                               final Collection<String> parameterValues,
                                               final String missingKey) {

        final ValidationReport.MessageContext context =
                ValidationReport.MessageContext.create().withParameter(parameter).build();

        if (parameterValues.isEmpty() && TRUE.equals(parameter.getRequired())) {
            return ValidationReport.singleton(
                    messages.get(missingKey, parameter.getName(), apiOperation.getApiPath().original())
            ).withAdditionalContext(context);
        }

        if (parameterValues.size() > 1) {
            return parameterValidator.validate(parameterValues, parameter);
        }

        return parameterValues
                .stream()
                .map(v -> parameterValidator.validate(v, parameter))
                .reduce(empty(), ValidationReport::merge);
    }

    @Nonnull
    private ValidationReport validateCustom(final Request request,
                                            final ApiOperation apiOperation) {
        return customRequestValidators
                .stream()
                .map(customValidator -> customValidator.validate(request, apiOperation))
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

    private static boolean isCookieParam(final Parameter p) {
        return isParam(p, "cookie");
    }

    private static boolean isDeepObjectParam(final Parameter p) {
        return p != null && p.getStyle() != null && p.getStyle().equals(StyleEnum.DEEPOBJECT);
    }

    private static boolean isParam(final Parameter p, final String type) {
        return p != null && p.getIn() != null && p.getIn().equalsIgnoreCase(type);
    }
}
