package com.atlassian.oai.validator.interaction.request;

import com.atlassian.oai.validator.model.Body;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.RequestBody;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.atlassian.oai.validator.report.ValidationReport.empty;
import static com.atlassian.oai.validator.util.ContentTypeUtils.findMostSpecificMatch;
import static com.atlassian.oai.validator.util.ContentTypeUtils.isFormDataContentType;
import static com.atlassian.oai.validator.util.ContentTypeUtils.isJsonContentType;
import static com.atlassian.oai.validator.util.HttpParsingUtils.parseUrlEncodedFormDataBodyAsJsonNode;
import static java.lang.Boolean.TRUE;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Validation for a request body.
 * <p>
 * The schema to validate is selected based on the content-type header of the incoming request.
 */
class RequestBodyValidator {

    private static final Logger log = getLogger(RequestBodyValidator.class);

    private final MessageResolver messages;

    private final SchemaValidator schemaValidator;

    RequestBodyValidator(@Nullable final MessageResolver messages, final SchemaValidator schemaValidator) {
        this.schemaValidator = requireNonNull(schemaValidator, "A schema validator is required");
        this.messages = messages == null ? new MessageResolver() : messages;
    }

    @Nonnull
    ValidationReport validateRequestBody(final Request request,
                                         @Nullable final RequestBody apiRequestBodyDefinition) {

        final Optional<Body> requestBody = request.getRequestBody();
        final boolean hasBody = requestBody.map(Body::hasBody).orElse(false);

        if (apiRequestBodyDefinition == null) {
            // A request body exists, but no request body is defined in the spec
            if (hasBody) {
                return ValidationReport.singleton(
                        messages.get("validation.request.body.unexpected")
                );
            }

            // No request body exists, and no request body is defined in the spec. Nothing to do.
            return empty();
        }

        ValidationReport.MessageContext context = ValidationReport.MessageContext.create()
                .withApiRequestBodyDefinition(apiRequestBodyDefinition)
                .build();

        if (!hasBody) {
            // No request body, but is required in the spec
            if (TRUE.equals(apiRequestBodyDefinition.getRequired())) {
                return ValidationReport.singleton(
                        messages.get("validation.request.body.missing")
                ).withAdditionalContext(context);
            }

            // No request body, and isn't required. Nothing to do.
            return empty();
        }

        final Optional<Pair<String, MediaType>> maybeApiMediaTypeForRequest =
                findApiMediaTypeForRequest(request, apiRequestBodyDefinition);

        // No matching media type found. Validation of mismatched content-type is handled elsewhere. Nothing to do.
        if (!maybeApiMediaTypeForRequest.isPresent()) {
            return empty();
        }

        context = ValidationReport.MessageContext.from(context)
                .withMatchedApiContentType(maybeApiMediaTypeForRequest.get().getLeft())
                .build();

        if (isJsonContentType(request)) {
            return schemaValidator
                    .validate(() -> requestBody.get().toJsonNode(),
                            maybeApiMediaTypeForRequest.get().getRight().getSchema(),
                            "request.body")
                    .withAdditionalContext(context);
        }

        if (isFormDataContentType(request)) {
            return schemaValidator
                    .validate(() -> parseUrlEncodedFormDataBodyAsJsonNode(requestBody.get().toString(StandardCharsets.UTF_8)),
                            maybeApiMediaTypeForRequest.get().getRight().getSchema(),
                            "request.body")
                    .withAdditionalContext(context);
        }

        // TODO: Validate multi-part form data

        log.info("Validation of '{}' not supported. Request body not validated.", maybeApiMediaTypeForRequest.get().getLeft());
        return empty();
    }

    private Optional<Pair<String, MediaType>> findApiMediaTypeForRequest(final Request request,
                                                                         @Nullable final RequestBody apiRequestBodyDefinition) {
        return Optional.ofNullable(apiRequestBodyDefinition)
                .map(RequestBody::getContent)
                .flatMap(content ->
                        findMostSpecificMatch(request, content.keySet())
                                .map(mostSpecificMatch -> Pair.of(mostSpecificMatch, content.get(mostSpecificMatch)))
                );
    }
}
