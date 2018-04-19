package com.atlassian.oai.validator.whitelist.rule;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Response;
import com.atlassian.oai.validator.report.ValidationReport.Message;
import io.swagger.models.RefModel;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.Collections;
import java.util.Objects;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

class IsEntityWhitelistRule implements RequestOrResponseWhitelistRule {
    private final String entityName;

    @Override
    public boolean matches(final Message message, final ApiOperation operation, final Request request) {
        return operation != null &&
                defaultIfNull(operation.getOperation().getParameters(), Collections.<Parameter>emptyList())
                        .stream()
                        .filter(BodyParameter.class::isInstance)
                        .map(BodyParameter.class::cast)
                        .map(BodyParameter::getSchema)
                        .filter(RefModel.class::isInstance)
                        .map(RefModel.class::cast)
                        .anyMatch(refModel -> entityName.equalsIgnoreCase(refModel.getSimpleRef()));
    }

    @Override
    public boolean matches(final Message message, final ApiOperation operation, final Response response) {
        // TODO
//        return operation != null && operation.getOperation().getResponses().entrySet()
//                .stream()
//                .filter(entry -> entry.getKey().equals(String.valueOf(response.getStatus())))
//                .map(Map.Entry::getValue)
//                .map(io.swagger.models.Response::getSchema)
//                .filter(RefProperty.class::isInstance)
//                .map(RefProperty.class::cast)
//                .anyMatch(ref -> entityName.equals(ref.getSimpleRef()));
        return false;
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
}
