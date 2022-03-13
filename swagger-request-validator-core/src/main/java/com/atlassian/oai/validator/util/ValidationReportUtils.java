package com.atlassian.oai.validator.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;

public class ValidationReportUtils {
    private ValidationReportUtils() {
    }

    /**
     * Walk a JSON object representing a report and replace all instances of the search string
     * in schema pointers or report paths with the given replacement value. This mutates the provided report.
     *
     * @param reportInput A JsonNode representing a validation report from, to be rewritten in place
     * @param searchRegex A valid regular expression to find in the report
     * @param replaceString The target string by which instances of the given pattern will be replaced
     */
    public static void replaceReportOutput(
            final JsonNode reportInput,
            final String searchRegex,
            final String replaceString
    ) {
        if (reportInput.isArray()) {
            reportInput.forEach(it -> {
                replaceReportOutput(it, searchRegex, replaceString);
            });
            return;
        }
        if (!reportInput.isObject()) {
            return;
        }
        if (reportInput.has("reports")) {
            if (reportInput.get("reports").isObject()) {
                final ObjectNode reportsAsJson = (ObjectNode) reportInput.get("reports");

                final ArrayList<String> propertiesToModify = new ArrayList<>();
                reportsAsJson.fields().forEachRemaining(it -> {
                    final String reportFieldName = it.getKey();
                    final JsonNode reportValue = it.getValue();
                    replaceReportOutput(reportValue, searchRegex, replaceString);

                    if (reportFieldName.contains(searchRegex)) {
                        propertiesToModify.add(reportFieldName);
                    }

                });

                propertiesToModify.forEach(reportFieldName -> {
                    final String replacementFieldName = reportFieldName.replaceAll(searchRegex, replaceString);
                    if (!reportsAsJson.has(replacementFieldName)) {
                        reportsAsJson.set(replacementFieldName, reportsAsJson.get(reportFieldName));
                        reportsAsJson.remove(reportFieldName);
                    }
                });
            }
        }
        if (reportInput.has("schema")) {
            if (reportInput.get("schema").isObject()) {
                final ObjectNode schemaObject = (ObjectNode) reportInput.get("schema");
                if (schemaObject.has("pointer")) {
                    schemaObject.put("pointer", schemaObject.get("pointer").textValue().replaceAll(searchRegex, replaceString));
                }
            }
        }
    }

}
