package com.atlassian.oai.validator.schema;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.networknt.schema.ValidationMessage;

import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Convert a {@link com.networknt.schema.ValidationMessage} into a
 * {@link com.atlassian.oai.validator.report.ValidationReport}
 */
public class ValidationMessageConverter {

    private final MessageResolver messages;

    public ValidationMessageConverter(final MessageResolver messages) {
        this.messages = messages;
    }

    ValidationReport toValidationReport(final Collection<ValidationMessage> validationMessages,
                                        final String keyPrefix) {
        final List<ValidationReport.Message> messages = validationMessages.stream()
                .map(m -> toMessage(m, keyPrefix))
                .collect(toList());

        return ValidationReport.from(messages);
    }

    ValidationReport.Message toMessage(final ValidationMessage validationMessage,
                                       final String keyPrefix) {
        final String keyword = validationMessage.getMessageKey();

        return messages.create(
                "validation." + keyPrefix + ".schema." + keyword,
                validationMessage.getError()
        ).withAdditionalContext(
                ValidationReport.MessageContext.create()
                        .withPointers(
                                validationMessage.getInstanceLocation().toString(),
                                validationMessage.getSchemaLocation().toString()
                        )
                        .build()
        );
    }
}
