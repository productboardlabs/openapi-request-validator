package com.atlassian.oai.validator.schema.format;

import com.networknt.schema.ExecutionContext;
import com.networknt.schema.Format;

public class Base64Format implements Format {
    private static final boolean[] BASE64_CHARACTERS = initBase64Characters();

    private static boolean[] initBase64Characters() {
        final boolean[] chars = new boolean[Character.MAX_VALUE];
        chars[43] = true; // '+'
        chars[47] = true; // '/'
        for (int i = 48; i <= 57; ++i) { // '0' to '9'
            chars[i] = true;
        }
        for (int i = 65; i <= 90; ++i) { // 'A' to 'Z'
            chars[i] = true;
        }
        for (int i = 97; i <= 122; ++i) { // 'a' to 'z'
            chars[i] = true;
        }
        return chars;
    }

    @Override
    public String getName() {
        return "byte";
    }

    @Override
    public boolean matches(final ExecutionContext executionContext, final String value) {
        final int length = value.length();
        if (length == 0) {
            return true;
        }

        // it is expected the Base64 string has padding - therefore its length is divisible by 4
        if (length % 4 != 0) {
            return false;
        }

        // check for padding at the end - which could be '', '=' or '=='
        final int end = (value.charAt(length - 1) != 61) ? length :
            (value.charAt(length - 2) != 61 ? length - 1 : length - 2);

        // the remaining characters may only be the Base64 characters
        for (int i = 0; i < end; ++i) {
            if (value.charAt(i) >= Character.MAX_VALUE || !BASE64_CHARACTERS[value.charAt(i)]) {
                return false;
            }
        }
        return true;
    }
}
