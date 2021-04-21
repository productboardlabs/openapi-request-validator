package com.atlassian.oai.validator.schema;

import com.atlassian.oai.validator.report.MessageResolver;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.report.ProcessingMessage;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.capitalize;

/**
 * Convert a {@link com.github.fge.jsonschema.core.report.ProcessingMessage} to a
 * {@link com.atlassian.oai.validator.report.ValidationReport}
 */
class ProcessingMessageConverter {
    private final MessageResolver messages;

    public ProcessingMessageConverter(@Nonnull final MessageResolver messages) {
        this.messages = requireNonNull(messages);
    }

    ValidationReport toValidationReport(final ProcessingMessage pm,
                                        final String keywordOverride,
                                        final String keyPrefix) {

        return ValidationReport.singleton(toValidationReportMessage(pm.asJson(), keywordOverride, keyPrefix));
    }

    ValidationReport.Message toValidationReportMessage(final JsonNode pm,
                                                       final String keywordOverride,
                                                       final String keyPrefix) {

        final String validationKeyword = getValidationKeyword(pm, keywordOverride);

        final String instancePointer = getInstancePointer(pm);
        final String schemaPointer = getSchemaPointer(pm);

        final List<String> subReports = new ArrayList<>();
        if (pm.has("reports")) {
            final JsonNode reports = pm.get("reports");
            reports.fields().forEachRemaining(field -> {
                field.getValue().elements().forEachRemaining(report -> {
                    subReports.add(field.getKey() + ": " + capitalize(report.get("message").textValue()));
                });
            });
        }

        final String message = buildMessage(pm, instancePointer);

        final ValidationReport.Message validationReportMessage = messages.create(
                "validation." + keyPrefix + ".schema." + validationKeyword,
                message, subReports.toArray(new String[0]))
                .withAdditionalContext(
                        ValidationReport.MessageContext
                                .create()
                                .withPointers(instancePointer.isEmpty() ? "/" : instancePointer, schemaPointer)
                                .build()
                );

        return withNestedMessages(pm, keywordOverride, keyPrefix, validationReportMessage);
    }

    private String buildMessage(final JsonNode pm, final String pointer) {
        return (pointer.isEmpty() ? "" : "[Path '" + pointer + "'] ") + capitalize(pm.get("message").textValue());
    }

    private String getInstancePointer(final JsonNode pm) {
        return pm.has("instance") ? pm.get("instance").get("pointer").textValue() : "";
    }

    private String getSchemaPointer(final JsonNode pm) {
        if (!pm.has("schema")) {
            return "/";
        }
        final JsonNode schemaNode = pm.get("schema");
        if (schemaNode.isTextual()) {
            return schemaNode.textValue();
        }
        if (schemaNode.isObject()) {
            return schemaNode.has("pointer") ? schemaNode.get("pointer").textValue() : "";
        }
        return "/";
    }

    private String getValidationKeyword(final JsonNode pm, final String keywordOverride) {
        final String suffix = pm.has("attribute") ? "." + pm.get("attribute").textValue() : "";
        if (keywordOverride != null) {
            return keywordOverride + suffix;
        }
        return pm.get("keyword").textValue() + suffix;
    }

    private ValidationReport.Message withNestedMessages(final JsonNode pm,
                                                        final String keywordOverride,
                                                        final String keyPrefix,
                                                        final ValidationReport.Message validationReportMessage) {
        if (!pm.has("reports")) {
            return validationReportMessage;
        }

        // Recursively convert 'reports' node children to ValidationReport.Message and add as nested messages
        final List<ValidationReport.Message> nestedMessages = new ArrayList<>();
        final JsonNode reports = pm.get("reports");

        // Reports may be an array, or an object keyed by the schema pointer
        if (reports.isArray()) {
            reports.iterator().forEachRemaining(report ->
                    nestedMessages.add(toValidationReportMessage(report, keywordOverride, keyPrefix))
            );
        } else if (reports.isObject()) {
            reports.fields().forEachRemaining(field ->
                    field.getValue().iterator().forEachRemaining(report ->
                            nestedMessages.add(toValidationReportMessage(report, keywordOverride, keyPrefix))
                    )
            );
        }
        return validationReportMessage.withNestedMessages(nestedMessages);
    }

}
