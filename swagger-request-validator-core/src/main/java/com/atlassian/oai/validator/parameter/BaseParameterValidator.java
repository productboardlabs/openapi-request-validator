package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.atlassian.oai.validator.report.ValidationReport.MessageContext;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;

import static java.lang.Boolean.TRUE;
import static java.lang.Math.abs;
import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

abstract class BaseParameterValidator implements ParameterValidator {

    private static final Logger log = getLogger(BaseParameterValidator.class);
    
    protected final MessageResolver messages;

    BaseParameterValidator(@Nonnull final MessageResolver messages) {
        this.messages = requireNonNull(messages, "A message resolver is required");
    }

    @Override
    public boolean supports(@Nullable final Parameter p) {
        return p != null &&
                p.getSchema() != null &&
                supportedParameterType().equalsIgnoreCase(p.getSchema().getType());
    }

    @Override
    @Nonnull
    public ValidationReport validate(@Nullable final String value, @Nullable final Parameter parameter) {
        if (!supports(parameter)) {
            return ValidationReport.empty();
        }

        final MessageContext context = MessageContext.create().withParameter(parameter).build();

        if (TRUE.equals(parameter.getRequired()) && (value == null || value.trim().isEmpty())) {
            return ValidationReport.singleton(
                    messages.get("validation.request.parameter.missing", parameter.getName())
            ).withAdditionalContext(context);
        }

        if (value == null || value.trim().isEmpty()) {
            return ValidationReport.empty();
        }

        if (!matchesEnumIfDefined(value, parameter)) {
            return ValidationReport.singleton(
                    messages.get("validation.request.parameter.enum.invalid",
                            value, parameter.getName(), parameter.getSchema().getEnum())
            ).withAdditionalContext(context);
        }

        return doValidate(value, parameter).withAdditionalContext(context);
    }

    @SuppressWarnings("unchecked")
    private boolean matchesEnumIfDefined(final String value, final Parameter parameter) {
        if (parameter.getSchema().getEnum() == null || parameter.getSchema().getEnum().isEmpty()) {
            return true;
        }

        final Object castValue = castToSchemaType(value, parameter.getSchema());
        return parameter.getSchema().getEnum().stream().anyMatch(v -> {
            if (castValue instanceof BigDecimal) {
                // Number formats get cast into BigDecimal - need to do an epsilon equality test
                return abs(((BigDecimal) castValue).doubleValue() - ((BigDecimal) v).doubleValue()) < 0.00001;
            }
            return castValue.equals(v);
        });
    }

    private Object castToSchemaType(final String value, final Schema schema) {
        // This is a bit gross, but easier than re-defining all the casting logic that exists in the schema types...
        try {
            final Method castMethod = schema.getClass().getDeclaredMethod("cast", Object.class);
            castMethod.setAccessible(true);
            return castMethod.invoke(schema, value);
        } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.debug("Unable to cast value for enum check.", e);
            return value;
        }
    }

    /**
     * Perform type-specific validations and return a validation report with accumulated errors
     *
     * @param value     The value being validated
     * @param parameter The parameter the value is being validated against
     */
    protected abstract ValidationReport doValidate(@Nonnull String value, @Nonnull Parameter parameter);
}
