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

    /**
     * Require that the given value is non-empty.
     *
     * @param value The value to check
     * @param msg The message to emit if validation fails
     *
     * @return the input value
     *
     * @throws IllegalArgumentException If the input value is null or empty
     */
    public static String requireNonEmpty(final String value, final String msg) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(msg);
        }
        return value;
    }

    private StringUtils() { }

    /**
     * Adds "\r\n" to the beginning and to the end of the string if not there
     * @param string - string to wrap
     * @param doNotAddIfAlreadyThere - when true, it won't append/prepend string with the new lines if they are already there
     */
    public static String addOpneingAndTrailnigNewlines(final String string, final boolean doNotAddIfAlreadyThere) {
        final StringBuilder withNewlines = new StringBuilder(string);

        if (!(string.startsWith("\r\n") && doNotAddIfAlreadyThere)) {
            withNewlines.insert(0, "\r\n");
        }

        if (!(string.endsWith("\r\n") && doNotAddIfAlreadyThere)) {
            withNewlines.append("\r\n");
        }
        return withNewlines.toString();
    }
}
