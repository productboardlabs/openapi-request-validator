package com.atlassian.oai.validator.util;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class StringUtils {

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
        if (isBlank(value)) {
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
    public static String addOpeningAndTrailingNewlines(final String string, final boolean doNotAddIfAlreadyThere) {
        final StringBuilder withNewlines = new StringBuilder(string);

        if (!(string.startsWith("\r\n") && doNotAddIfAlreadyThere)) {
            withNewlines.insert(0, "\r\n");
        }

        if (!(string.endsWith("\r\n") && doNotAddIfAlreadyThere)) {
            withNewlines.append("\r\n");
        }
        return withNewlines.toString();
    }

    /**
     * Inserts indentStr before every line
     *
     * @param stringToIndent
     * @param indentStr
     * @return
     */
    public static String indentString(final String stringToIndent, final String indentStr) {
        return indentStr + stringToIndent.replace("\n", "\n" + indentStr);
    }

}
