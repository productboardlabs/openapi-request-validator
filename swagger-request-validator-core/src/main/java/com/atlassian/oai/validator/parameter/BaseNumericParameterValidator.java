package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;
import io.swagger.models.parameters.SerializableParameter;

import javax.annotation.Nonnull;

import java.math.BigDecimal;

import static com.google.common.base.MoreObjects.firstNonNull;

public abstract class BaseNumericParameterValidator extends BaseParameterValidator {

    public BaseNumericParameterValidator(@Nonnull final MessageResolver messages) {
        super(messages);
    }

    @Override
    protected void doValidate(@Nonnull final String value,
                              @Nonnull final SerializableParameter parameter,
                              @Nonnull final MutableValidationReport validationReport) {

        try {
            final double doubleValue = getNumericValue(value, parameter).doubleValue();
            validateMinimum(parameter, validationReport, doubleValue);
            validateMaximum(parameter, validationReport, doubleValue);
            validateMultipleOf(parameter, validationReport, doubleValue);
        } catch (final NumberFormatException e) {
            failFormatValidation(value, parameter, parameter.getFormat(), validationReport);
        }
    }

    private void failFormatValidation(
        final String value,
        final SerializableParameter parameter,
        final String format,
        final MutableValidationReport report) {
        report.add(messages.get("validation.request.parameter.invalidFormat",
            value, parameter.getName(), supportedParameterType(), format)
        );

    }

    private void validateMultipleOf(final SerializableParameter parameter,
                                    final MutableValidationReport report,
                                    final Double value) {

        final Number multipleOf = parameter.getMultipleOf();
        final Double doubleMultipleOf = multipleOf != null ? multipleOf.doubleValue() : null;
        if (doubleMultipleOf != null && (value % doubleMultipleOf != 0d)) {
            report.add(messages.get("validation.request.parameter.number.multipleOf",
                value, parameter.getName(), multipleOf)
            );
        }
    }

    private void validateMinimum(final SerializableParameter parameter,
                                 final MutableValidationReport report,
                                 final Double value) {
        final BigDecimal minimum = parameter.getMinimum();
        final boolean exclusiveMinimum = firstNonNull(parameter.isExclusiveMinimum(), false);

        if (parameter.getMinimum() != null) {
            if (exclusiveMinimum && value <= minimum.doubleValue()) {
                report.add(messages.get("validation.request.parameter.number.belowExclusiveMin",
                    value, parameter.getName(), minimum)
                );
            } else if (!exclusiveMinimum && value < minimum.doubleValue()) {
                report.add(messages.get("validation.request.parameter.number.belowMin",
                    value, parameter.getName(), minimum)
                );
            }
        }
    }

    private void validateMaximum(final SerializableParameter parameter,
                                 final MutableValidationReport report,
                                 final Double value) {
        final BigDecimal maximum = parameter.getMaximum();
        final boolean exclusiveMaximum = firstNonNull(parameter.isExclusiveMaximum(), false);

        if (parameter.getMaximum() != null) {
            if (exclusiveMaximum && value >= maximum.doubleValue()) {
                report.add(messages.get("validation.request.parameter.number.aboveExclusiveMax",
                    value, parameter.getName(), maximum)
                );
            } else if (!exclusiveMaximum && value > maximum.doubleValue()) {
                report.add(messages.get("validation.request.parameter.number.aboveMax",
                    value, parameter.getName(), maximum)
                );
            }
        }
    }

    protected abstract Number getNumericValue(String value, SerializableParameter parameter) throws NumberFormatException;
}
