package com.atlassian.oai.validator.util;

public class StringUtils {

    /**
     * Quote the given string if needed
     *
     * @param value The value to quote (e.g. bob)
     * @return The quoted string (e.g. "bob")
     */
    public static String quote(final String value) {
        if (value == null) {
            return value;
        }
        String result = value;
        if (!result.startsWith("\"")) {
            result = "\"" + result;
        }
        if (!result.endsWith("\"")) {
            result = result + "\"";
        }
        return result;
    }

    /**
     * Capitalise the first letter of the provided string.
     *
     * @param value The value to capitalise.
     * @return The capitalised string.
     */
    public static String capitalise(final String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

}
