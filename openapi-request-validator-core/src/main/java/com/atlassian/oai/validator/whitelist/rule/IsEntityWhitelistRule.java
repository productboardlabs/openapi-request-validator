package com.atlassian.oai.validator.whitelist.rule;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport.Message;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;

import javax.annotation.Nullable;
import java.util.Objects;

class IsEntityWhitelistRule implements RequestOrResponseWhitelistRule {
    private final String entityName;

    @Override
    public boolean matches(final Message message, final ApiOperation operation, final Request request) {
        if (operation == null || operation.getOperation().getRequestBody() == null) {
            return false;
        }

        final RequestBody apiRequestBody = operation.getOperation().getRequestBody();

        // TODO: This should really respect the content-type of the response to filter schemas
        return apiRequestBody.getContent().values().stream()
                .map(MediaType::getSchema)
                .filter(Objects::nonNull)
                .map(Schema::get$ref)
                .filter(Objects::nonNull)
                .anyMatch($ref -> $ref.endsWith("/" + entityName));
    }

    @Override
    public boolean matches(final Message message, final ApiOperation operation, final Response response) {
        if (operation == null || operation.getOperation().getResponses() == null) {
            return false;
        }

        final ApiResponse apiResponse = getApiResponse(response, operation);
        if (apiResponse == null || apiResponse.getContent() == null) {
            return false;
        }

        // TODO: This should really respect the content-type of the response to filter schemas
        return apiResponse.getContent().values().stream()
                .map(MediaType::getSchema)
                .filter(Objects::nonNull)
                .map(Schema::get$ref)
                .filter(Objects::nonNull)
                .anyMatch($ref -> $ref.endsWith("/" + entityName));
    }

    @Override
    public String toString() {
        return "Is entity: " + entityName;
    }

    public IsEntityWhitelistRule(final String entityName) {
        this.entityName = Objects.requireNonNull(entityName);
    }

    public String getEntityName() {
        return entityName;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final IsEntityWhitelistRule that = (IsEntityWhitelistRule) o;

        return Objects.equals(getEntityName(), that.getEntityName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getEntityName());
    }

    // TODO: This logic is duplicated. Should move somewhere.
    @Nullable
    private ApiResponse getApiResponse(final Response response,
                                       final ApiOperation apiOperation) {
        final ApiResponse apiResponse =
                apiOperation.getOperation().getResponses().get(Integer.toString(response.getStatus()));
        if (apiResponse == null) {
            return apiOperation.getOperation().getResponses().getDefault();
        }
        return apiResponse;
    }
}
