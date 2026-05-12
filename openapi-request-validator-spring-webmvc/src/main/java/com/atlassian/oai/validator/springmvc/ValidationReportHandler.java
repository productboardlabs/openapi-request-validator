package com.atlassian.oai.validator.springmvc;

import com.atlassian.oai.validator.report.ValidationReport;

/**
 * By implementing this interface you can define your own
 * way how to react on validation issues - either request
 * or response. You can define own logging or throw your
 * specific exceptions.
 */
public interface ValidationReportHandler {

    /**
     * Method which gives you way how to override logging of request validation issues.
     *
     * @param loggingKey request identifier - method and request path
     * @param validationReport result of validation
     */
    void handleRequestReport(final String loggingKey, ValidationReport validationReport);

    /**
     * Method which gives you way how to override logging of response validation issues.
     *
     * @param loggingKey request identifier - method and request path
     * @param validationReport result of validation
     */
    void handleResponseReport(final String loggingKey, ValidationReport validationReport);
}
