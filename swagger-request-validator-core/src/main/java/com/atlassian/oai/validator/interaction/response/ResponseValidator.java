package com.atlassian.oai.validator.interaction.response;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.MessageContext;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.atlassian.oai.validator.report.ValidationReport.MessageContext.Location.RESPONSE;
import static com.atlassian.oai.validator.report.ValidationReport.empty;
import static com.atlassian.oai.validator.util.ContentTypeUtils.findMostSpecificMatch;
import static com.atlassian.oai.validator.util.ContentTypeUtils.isFormDataContentType;
import static com.atlassian.oai.validator.util.ContentTypeUtils.isJsonContentType;
import static com.atlassian.oai.validator.util.HttpParsingUtils.parseUrlEncodedFormDataBodyAsJson;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Validate a response against an API operation
 */
public class ResponseValidator {

    private static final Logger log = getLogger(ResponseValidator.class);

    private final SchemaValidator schemaValidator;
    private final MessageResolver messages;
    private final OpenAPI api;
    private final List<CustomResponseValidator> customResponseValidators;

    /**
     * Construct a new response validator with the given schema validator.
     *
     * @param schemaValidator The schema validator to use when validating response bodies
     * @param messages The message resolver to use
     * @param api The OpenAPI spec to validate against
     * @param customResponseValidators The list of custom validators to run
     */
    public ResponseValidator(final SchemaValidator schemaValidator,
                             final MessageResolver messages,
                             final OpenAPI api,
                             final List<CustomResponseValidator> customResponseValidators) {
        this.schemaValidator = requireNonNull(schemaValidator, "A schema validator is required");
        this.messages = requireNonNull(messages, "A message resolver is required");
        this.api = requireNonNull(api, "An OAI definition is required");
        this.customResponseValidators = customResponseValidators;
    }

    /**
     * Validate the given response against the API operation.
     *
     * @param response The response to validate
     * @param apiOperation The API operation to validate the response against
     *
     * @return A validation report containing validation errors
     */
    @Nonnull
    public ValidationReport validateResponse(final Response response, final ApiOperation apiOperation) {
        requireNonNull(response, "A response is required");
        requireNonNull(apiOperation, "An API operation is required");

        final ApiResponse apiResponse = getApiResponse(response, apiOperation);

        final MessageContext.Builder contextBuilder = MessageContext.create()
                .in(RESPONSE)
                .withApiOperation(apiOperation);

        if (apiResponse == null) {
            return ValidationReport.singleton(
                    messages.get("validation.response.status.unknown",
                            response.getStatus(), apiOperation.getApiPath().original())
            ).withAdditionalContext(contextBuilder.build());
        }

        return validateResponseBody(response, apiResponse, apiOperation)
                .merge(validateContentType(response, apiOperation))
                .merge(validateHeaders(response, apiResponse, apiOperation))
                .merge(validateCustom(response, apiOperation))
                .withAdditionalContext(
                        contextBuilder
                                .withResponseStatus(response.getStatus())
                                .withApiResponseDefinition(apiResponse)
                                .build()
                );
    }

    @Nullable
    private ApiResponse getApiResponse(final Response response,
                                       final ApiOperation apiOperation) {
        final ApiResponse apiResponse =
                apiOperation.getOperation().getResponses().get(Integer.toString(response.getStatus()));
        if (apiResponse == null) {
            return apiOperation.getOperation().getResponses().get("default"); // try the default response
        }
        return apiResponse;
    }

    @Nonnull
    private ValidationReport validateResponseBody(final Response response,
                                                  final ApiResponse apiResponse,
                                                  final ApiOperation apiOperation) {
        if (apiResponse.getContent() == null) {
            return ValidationReport.empty();
        }

        final Optional<String> mostSpecificMatch = findMostSpecificMatch(response, apiResponse.getContent().keySet());

        if (!mostSpecificMatch.isPresent()) {
            // Validation of invalid content type is handled in content type validation
            return ValidationReport.empty();
        }

        final MediaType apiMediaType = apiResponse.getContent().get(mostSpecificMatch.get());
        if (apiMediaType.getSchema() == null) {
            return ValidationReport.empty();
        }

        final Optional<String> responseBody = response.getBody();

        if (!responseBody.isPresent() || responseBody.get().isEmpty()) {
            return ValidationReport.singleton(
                    messages.get("validation.response.body.missing",
                            apiOperation.getMethod(), apiOperation.getApiPath().original())
            );
        }

        if (isJsonContentType(response)) {
            return schemaValidator.validate(responseBody.get(), apiMediaType.getSchema(), "response.body");
        }

        if (isFormDataContentType(response)) {
            final String bodyAsJson = parseUrlEncodedFormDataBodyAsJson(responseBody.get());
            return schemaValidator.validate(bodyAsJson, apiMediaType.getSchema(), "response.body");
        }

        if (response.getContentType().isPresent()) {
            log.info("Validation of '{}' not supported. Response body not validated.", response.getContentType().get());
        }

        return empty();
    }

    @Nonnull
    private ValidationReport validateContentType(final Response response,
                                                 final ApiOperation apiOperation) {

        final Optional<String> requestHeader = response.getContentType();
        if (!requestHeader.isPresent()) {
            return ValidationReport.empty();
        }

        final com.google.common.net.MediaType requestMediaType;
        try {
            requestMediaType = com.google.common.net.MediaType.parse(requestHeader.get());
        } catch (final IllegalArgumentException e) {
            return ValidationReport.singleton(messages.get(
                    "validation.response.contentType.invalid", requestHeader.get())
            );
        }

        final Collection<String> responseMediaTypes = getResponseMediaTypes(response, apiOperation);
        if (responseMediaTypes.isEmpty()) {
            return ValidationReport.empty();
        }

        if (responseMediaTypes
                .stream()
                .allMatch("*/*"::equals)) {
            return empty();
        }

        final boolean contentTypeMatchesProduces = responseMediaTypes.stream()
                .map(com.google.common.net.MediaType::parse)
                .anyMatch(m -> m.withoutParameters().is(requestMediaType.withoutParameters()));

        if (!contentTypeMatchesProduces) {
            return ValidationReport.singleton(
                    messages.get("validation.response.contentType.notAllowed", requestHeader.get(), responseMediaTypes)
            );
        }

        return ValidationReport.empty();
    }

    @Nonnull
    private Collection<String> getResponseMediaTypes(final Response response,
                                                     final ApiOperation apiOperation) {
        final ApiResponse apiResponse = getApiResponse(response, apiOperation);
        if (apiResponse == null) {
            return emptyList();
        }
        return defaultIfNull(apiResponse.getContent(), new Content()).keySet();
    }

    @Nonnull
    private ValidationReport validateHeaders(final Response response,
                                             final ApiResponse apiResponse,
                                             final ApiOperation apiOperation) {

        final Map<String, Header> apiHeaders = apiResponse.getHeaders();
        if (apiHeaders == null || apiHeaders.isEmpty()) {
            return ValidationReport.empty();
        }

        return apiHeaders.entrySet()
                .stream()
                .map(h -> validateHeader(apiOperation, h.getKey(), h.getValue(), response.getHeaderValues(h.getKey())))
                .reduce(ValidationReport.empty(), ValidationReport::merge);

    }

    @Nonnull
    private ValidationReport validateHeader(final ApiOperation apiOperation,
                                            final String headerName,
                                            final Header apiHeader,
                                            final Collection<String> propertyValues) {

        if (propertyValues.isEmpty() && TRUE.equals(apiHeader.getRequired())) {
            return ValidationReport.singleton(
                    messages.get("validation.response.header.missing", headerName, apiOperation.getApiPath().original())
            );
        }

        return propertyValues
                .stream()
                .map(v -> schemaValidator.validate(v, apiHeader.getSchema(), "response.header"))
                .reduce(ValidationReport.empty(), ValidationReport::merge);
    }

    @Nonnull
    private ValidationReport validateCustom(final Response response,
                                            final ApiOperation apiOperation) {
        return customResponseValidators
                .stream()
                .map(customResponseValidator -> customResponseValidator.validate(response, apiOperation))
                .reduce(ValidationReport.empty(), ValidationReport::merge);
    }
}
