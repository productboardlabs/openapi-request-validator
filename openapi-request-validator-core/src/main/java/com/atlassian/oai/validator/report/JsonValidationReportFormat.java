package com.atlassian.oai.validator.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Formats a {@link ValidationReport} as a JSON string for use in tooling etc.
 */
public class JsonValidationReportFormat implements ValidationReportFormat {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.findAndRegisterModules();
        OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        OBJECT_MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private static final JsonValidationReportFormat INSTANCE = new JsonValidationReportFormat();

    public static JsonValidationReportFormat getInstance() {
        return INSTANCE;
    }

    @Override
    public String apply(final ValidationReport report) {
        try {
            return OBJECT_MAPPER.writeValueAsString(report);
        } catch (final JsonProcessingException e) {
            return "";
        }
    }

    public String apply(final ValidationReport.Message message) {
        try {
            return OBJECT_MAPPER.writeValueAsString(message);
        } catch (final JsonProcessingException e) {
            return "";
        }
    }

    private JsonValidationReportFormat() {

    }
}
