package com.atlassian.oai.validator.schema.format;

import com.github.fge.jackson.NodeType;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.format.AbstractFormatAttribute;
import com.github.fge.jsonschema.format.FormatAttribute;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;

public class Base64Attribute extends AbstractFormatAttribute {

    private static final FormatAttribute INSTANCE = new Base64Attribute();
    private static final boolean[] BASE64_CHARACTERS = initBase64Characters();

    public static FormatAttribute getInstance() {
        return INSTANCE;
    }

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

    private Base64Attribute() {
        super("byte", NodeType.STRING);
    }

    @Override
    public void validate(final ProcessingReport report,
                         final MessageBundle bundle,
                         final FullData data) throws ProcessingException {
        final String value = data.getInstance().getNode().textValue();

        final int length = value.length();
        if (length == 0) {
            return;
        }

        // it is expected the Base64 string has padding - therefore its length is divisible by 4
        if (length % 4 != 0) {
            report.error(newMsg(data, bundle, "err.format.base64.invalidLength")
                    .putArgument("length", length)
                    .put("key", "err.format.base64.invalidLength"));
            return;
        }

        // check for padding at the end - which could be '', '=' or '=='
        final int end = (value.charAt(length - 1) != 61) ? length :
                (value.charAt(length - 2) != 61 ? length - 1 : length - 2);

        // the remaining characters may only be the Base64 characters
        for (int i = 0; i < end; ++i) {
            if (!BASE64_CHARACTERS[value.charAt(i)]) {
                report.error(newMsg(data, bundle, "err.format.base64.invalid")
                        .putArgument("character", Character.toString(value.charAt(i)))
                        .putArgument("index", i)
                        .put("key", "err.format.base64.invalid"));
                return;
            }
        }
    }
}
