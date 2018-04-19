package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.MessageContext;
import com.atlassian.oai.validator.schema.SchemaValidator;
import com.google.common.net.MediaType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.atlassian.oai.validator.report.ValidationReport.MessageContext.Location.RESPONSE;
import static com.atlassian.oai.validator.report.ValidationReport.empty;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Validate a response against an API operation
 */
public class ResponseValidator {

    private static final Logger log = getLogger(ResponseValidator.class);

    private final SchemaValidator schemaValidator;
    private final MessageResolver messages;
    private final OpenAPI oaiDefinition;

    /**
     * Construct a new response validator with the given schema validator.
     *
     * @param schemaValidator The schema validator to use when validating response bodies
     * @param messages The message resolver to use
     * @param oaiDefinition The OpenAPI spec to validate against
     */
    public ResponseValidator(final SchemaValidator schemaValidator,
                             final MessageResolver messages,
                             final OpenAPI oaiDefinition) {
        this.schemaValidator = requireNonNull(schemaValidator, "A schema validator is required");
        this.messages = requireNonNull(messages, "A message resolver is required");
        this.oaiDefinition = requireNonNull(oaiDefinition, "An OAI definition is required");
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
        // TODO
//        if (apiResponse.getContent().getSchema() == null) {
//            return ValidationReport.empty();
//        }
//
//        if (!response.getBody().isPresent() || response.getBody().get().isEmpty()) {
//            return ValidationReport.singleton(
//                    messages.get("validation.response.body.missing",
//                            apiOperation.getMethod(), apiOperation.getApiPath().original())
//            );
//        }
//
//        if (hasContentType(response) && !isJsonContentType(response)) {
//            log.debug("Non-JSON response body found. No validation will be applied.");
//            return empty();
//        }
//
//        return schemaValidator.validate(response.getBody().get(), apiResponse.getSchema());
        return empty();
    }

    @Nonnull
    private ValidationReport validateContentType(final Response response,
                                                 final ApiOperation apiOperation) {

        final Optional<String> requestHeader = response.getHeaderValue("Content-Type");
        if (!requestHeader.isPresent()) {
            return ValidationReport.empty();
        }

        final MediaType requestMediaType;
        try {
            requestMediaType = MediaType.parse(requestHeader.get());
        } catch (final IllegalArgumentException e) {
            return ValidationReport.singleton(messages.get("validation.response.contentType.invalid", requestHeader.get()));
        }

        final Collection<String> produces = getProduces(apiOperation);
        if (produces.isEmpty()) {
            return ValidationReport.empty();
        }

        final boolean contentTypeMatchesProduces = produces.stream()
                        .map(MediaType::parse)
                        .anyMatch(m -> m.withoutParameters().is(requestMediaType.withoutParameters()));
        if (!contentTypeMatchesProduces) {
            return ValidationReport.singleton(messages.get("validation.response.contentType.notAllowed", requestHeader.get(), produces));
        }

        return ValidationReport.empty();
    }

    @Nonnull
    private Collection<String> getProduces(final ApiOperation apiOperation) {
        return apiOperation.getOperation()
                .getResponses()
                .values()
                .stream()
                .flatMap(apiResponse -> apiResponse.getContent().keySet().stream())
                .collect(Collectors.toSet());
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

        if (propertyValues.isEmpty() && apiHeader.getRequired()) {
            return ValidationReport.singleton(
                    messages.get("validation.response.header.missing", headerName, apiOperation.getApiPath().original())
            );
        }

        return propertyValues
                .stream()
                .map(v -> schemaValidator.validate(v, apiHeader.getSchema()))
                .reduce(ValidationReport.empty(), ValidationReport::merge);
    }
}
