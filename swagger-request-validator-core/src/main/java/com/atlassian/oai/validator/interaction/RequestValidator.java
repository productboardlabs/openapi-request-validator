package com.atlassian.oai.validator.interaction;

import com.atlassian.oai.validator.model.ApiOperation;
import com.atlassian.oai.validator.model.NormalisedPath;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.parameter.ParameterValidators;
import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.schema.SchemaValidator;
import com.google.common.net.MediaType;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;

import javax.annotation.Nonnull;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Validate a request against a given API operation.
 */
public class RequestValidator {

    private final SchemaValidator schemaValidator;
    private final ParameterValidators parameterValidators;
    private final MessageResolver messages;

    /**
     * Construct a new request validator with the given schema validator.
     *
     * @param schemaValidator The schema validator to use when validating request bodies
     * @param messages The message resolver to use
     */
    public RequestValidator(@Nonnull final SchemaValidator schemaValidator, @Nonnull final MessageResolver messages) {
        this.schemaValidator = requireNonNull(schemaValidator, "A schema validator is required");
        this.parameterValidators = new ParameterValidators(schemaValidator, messages);
        this.messages = requireNonNull(messages, "A message resolver is required");
    }

    /**
     * Validate the request against the given API operation
     *
     * @param requestPath The normalised path the request is on
     * @param request The request to validate
     * @param apiOperation The operation to validate the request against
     *
     * @return A validation report containing validation errors
     */
    @Nonnull
    public ValidationReport validateRequest(@Nonnull final NormalisedPath requestPath,
                                            @Nonnull final Request request,
                                            @Nonnull final ApiOperation apiOperation) {
        requireNonNull(requestPath, "A request path is required");
        requireNonNull(request, "A request is required");
        requireNonNull(apiOperation, "An API operation is required");

        return validatePathParameters(requestPath, apiOperation)
                .merge(validateRequestBody(request.getBody(), apiOperation))
                .merge(validateQueryParameters(request, apiOperation));
    }

    @Nonnull
    private ValidationReport validateRequestBody(@Nonnull final Optional<String> requestBody,
                                                 @Nonnull final ApiOperation apiOperation) {

        if (isFormData(requestBody, apiOperation)) {
            return validateForm(requestBody, apiOperation);
        } else {
            return validateBody(requestBody, apiOperation);
        }
    }


    @Nonnull
    private ValidationReport validateForm(@Nonnull Optional<String> requestBody, @Nonnull ApiOperation apiOperation) {
        HashMap<String, List<String>> paramAndValues = parseFormData(requestBody.get());
        List<ValidationReport> reports = new ArrayList<>();
        for (Parameter parameter : apiOperation.getOperation().getParameters()) {
            List<String> parameterValues = paramAndValues.getOrDefault(parameter.getName(), Collections.singletonList(null));
            reports.addAll(parameterValues.stream().map(value -> parameterValidators.validate(value, parameter)).collect(Collectors.toList()));
        }
        return reports.stream().reduce(ValidationReport.empty(), ValidationReport::merge);
    }

    @Nonnull
    private ValidationReport validateBody(@Nonnull Optional<String> requestBody, @Nonnull ApiOperation apiOperation) {
        final Optional<Parameter> bodyParameter = apiOperation.getOperation().getParameters()
                .stream().filter(p -> p.getIn().equalsIgnoreCase("body")).findFirst();

        if (requestBody.isPresent() && !requestBody.get().isEmpty() && !bodyParameter.isPresent()) {
            return ValidationReport.singleton(
                    messages.get("validation.request.body.unexpected",
                        apiOperation.getMethod(), apiOperation.getPathString().original())
            );
        }

        if (!bodyParameter.isPresent()) {
            return ValidationReport.empty();
        }

        if (!requestBody.isPresent() || requestBody.get().isEmpty()) {
            if (bodyParameter.get().getRequired()) {
                return ValidationReport.singleton(
                        messages.get("validation.request.body.missing",
                            apiOperation.getMethod(), apiOperation.getPathString().original())
                );
            }
            return ValidationReport.empty();
        }

        return schemaValidator.validate(requestBody.get(), ((BodyParameter)bodyParameter.get()).getSchema());
    }

    @Nonnull
    private ValidationReport validatePathParameters(@Nonnull final NormalisedPath requestPath,
                                                    @Nonnull final ApiOperation apiOperation) {

        ValidationReport validationReport = ValidationReport.empty();
        for (int i = 0; i < apiOperation.getPathString().parts().size(); i++) {
            if (!apiOperation.getPathString().isParam(i)) {
                continue;
            }

            final String paramName = apiOperation.getPathString().paramName(i);
            final String paramValue = requestPath.part(i);

            final Optional<Parameter> parameter = apiOperation.getOperation().getParameters()
                    .stream()
                    .filter(p -> p.getIn().equalsIgnoreCase("PATH"))
                    .filter(p -> p.getName().equalsIgnoreCase(paramName))
                    .findFirst();

            if (parameter.isPresent()) {
                validationReport = validationReport.merge(parameterValidators.validate(paramValue, parameter.get()));
            }
        }
        return validationReport;
    }

    private ValidationReport validateQueryParameters(@Nonnull final Request request,
                                                     @Nonnull final ApiOperation apiOperation) {
        return apiOperation
                .getOperation()
                .getParameters()
                .stream()
                .filter(p -> p.getIn().equalsIgnoreCase("QUERY"))
                .map(p -> validateQueryParameter(request, apiOperation, p))
                .reduce(ValidationReport.empty(), ValidationReport::merge);
    }

    private ValidationReport validateQueryParameter(@Nonnull final Request request,
                                                    @Nonnull final ApiOperation apiOperation,
                                                    @Nonnull final Parameter queryParameter) {

        final Collection<String> queryParameterValues = request.getQueryParameterValues(queryParameter.getName());

        if (queryParameterValues.isEmpty() && queryParameter.getRequired()) {
            return ValidationReport.singleton(
                    messages.get("validation.request.parameter.query.missing",
                            queryParameter.getName(), apiOperation.getPathString().original())
            );
        }

        return queryParameterValues
                .stream()
                .map((v) -> parameterValidators.validate(v, queryParameter))
                .reduce(ValidationReport.empty(), ValidationReport::merge);
    }

    @Nonnull
    private boolean isFormData(@Nonnull Optional<String> requestBody, @Nonnull ApiOperation apiOperation) {
        List<String> consumes = apiOperation.getOperation().getConsumes();
        return null != consumes && !consumes.isEmpty() &&
                consumes.stream().anyMatch(p -> p.equals(MediaType.FORM_DATA.toString()))
                && requestBody.isPresent();
    }

    @Nonnull
    private HashMap<String, List<String>> parseFormData(String formData) {
        HashMap<String, List<String>> params = new HashMap<>();
        String[] pairs = formData.split("&");
        try {
            for (String pair : pairs) {
                String[] fields = pair.split("=");
                if (fields.length > 1) {
                    String name = URLDecoder.decode(fields[0], "UTF-8");
                    String value = URLDecoder.decode(fields[1], "UTF-8");
                    params.merge(name, Collections.singletonList(value), (oldValue, newValue) -> {
                        oldValue.addAll(newValue);
                        return oldValue;
                    });
                }
            }
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        return params;
    }

}
