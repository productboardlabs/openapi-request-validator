package com.atlassian.oai.validator.schema.format;

import com.github.fge.jackson.NodeType;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.format.AbstractFormatAttribute;
import com.github.fge.jsonschema.format.FormatAttribute;
import com.github.fge.jsonschema.processors.data.FullData;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

public class Base64Attribute extends AbstractFormatAttribute {

    private static final FormatAttribute INSTANCE = new Base64Attribute();

    public static FormatAttribute getInstance() {
        return INSTANCE;
    }

    private Base64Attribute() {
        super("byte", NodeType.STRING);
    }

    private static boolean isBase64(final String string) {
        try {
            final InputStream in = BaseEncoding.base64().decodingStream(new StringReader(string));
            while (in.read() != -1) {
                // intentionally left blank
            }
            return true;
        } catch (final IOException e) {
            return false; // there was an decoding error - the Base64 string is invalid
        }
    }

    @Override
    public void validate(final ProcessingReport report,
                         final MessageBundle bundle,
                         final FullData data) throws ProcessingException {
        final String value = data.getInstance().getNode().textValue();
        if (!isBase64(value)) {
            report.error(newMsg(data, bundle, "err.format.base64.invalid")
                    .put("key", "err.format.base64.invalid"));
        }
    }
}
