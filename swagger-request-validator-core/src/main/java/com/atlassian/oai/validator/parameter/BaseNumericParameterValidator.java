package com.atlassian.oai.validator.parameter;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.MutableValidationReport;
import io.swagger.models.parameters.SerializableParameter;

import javax.annotation.Nonnull;
import javax.print.Doc;

import static com.google.common.base.MoreObjects.firstNonNull;

public abstract class BaseNumericParameterValidator extends BaseParameterValidator {

    public BaseNumericParameterValidator(@Nonnull MessageResolver messages) {
        super(messages);
    }

    @Override
    protected void doValidate(@Nonnull String value,
                              @Nonnull SerializableParameter parameter,
                              @Nonnull MutableValidationReport validationReport) {

        try {
            double doubleValue = getNumericValue(value, parameter).doubleValue();
            validateMinimum(parameter, validationReport, doubleValue);
            validateMaximum(parameter, validationReport, doubleValue);
            validateMultipleOf(parameter, validationReport, doubleValue);
        } catch (NumberFormatException e) {
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

    private void validateMultipleOf(SerializableParameter parameter,
                                    MutableValidationReport report,
                                    Double value) {

        Number multipleOf = parameter.getMultipleOf();
        Double doubleMultipleOf = multipleOf != null ?
            multipleOf.doubleValue() :
            null;
        if (doubleMultipleOf != null && (value % doubleMultipleOf != 0d)) {
            report.add(messages.get("validation.request.parameter.number.multipleOf",
                value, parameter.getName(), multipleOf)
            );
        }
    }

    private void validateMinimum(SerializableParameter parameter, MutableValidationReport report, Double value) {
        Double minimum = parameter.getMinimum();
        boolean exclusiveMinimum = firstNonNull(parameter.isExclusiveMinimum(), false);

        if (parameter.getMinimum() != null) {
            if (exclusiveMinimum && value <= minimum) {
                report.add(messages.get("validation.request.parameter.number.belowExclusiveMin",
                    value, parameter.getName(), minimum)
                );
            } else if (!exclusiveMinimum && value < minimum) {
                report.add(messages.get("validation.request.parameter.number.belowMin",
                    value, parameter.getName(), minimum)
                );
            }
        }
    }

    private void validateMaximum(SerializableParameter parameter, MutableValidationReport report, Double value) {
        Double maximum = parameter.getMaximum();
        boolean exclusiveMaximum = firstNonNull(parameter.isExclusiveMaximum(), false);

        if (parameter.getMaximum() != null) {
            if (exclusiveMaximum && value >= maximum) {
                report.add(messages.get("validation.request.parameter.number.aboveExclusiveMax",
                    value, parameter.getName(), maximum)
                );
            } else if (!exclusiveMaximum && value > maximum) {
                report.add(messages.get("validation.request.parameter.number.aboveMax",
                    value, parameter.getName(), maximum)
                );
            }
        }
    }

    protected abstract Number getNumericValue(String value, SerializableParameter parameter) throws NumberFormatException;
}
