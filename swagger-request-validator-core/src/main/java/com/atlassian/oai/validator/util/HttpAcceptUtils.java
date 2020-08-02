package com.atlassian.oai.validator.util;

import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Utils for parsing a HTTP Accept header into a list of media types
 */
public final class HttpAcceptUtils {

    /**
     * Split the given header value into individual media types that can then be parsed as necessary.
     * <p>
     * This implementation supports the presence of commas within parameters e.g.
     * <code>application/hal+json;charset=UTF-8, foo/bar; q="A,B,C"</code>
     *
     * @param acceptHeaderValue The header to split.
     *
     * @return A list of media type strings from the accept header after splitting
     */
    public static List<String> splitAcceptHeader(@Nullable final String acceptHeaderValue) {
        if (isBlank(acceptHeaderValue)) {
            return ImmutableList.of();
        }
        final List<String> result = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotedValue = false;
        for (int i = 0; i < acceptHeaderValue.length(); i++) {
            final char c = acceptHeaderValue.charAt(i);
            if (c == ',' && !inQuotedValue) {
                result.add(currentValue.toString().trim());
                currentValue = new StringBuilder();
                continue;
            } else if (c == '"') {
                inQuotedValue = !inQuotedValue;
            }
            currentValue.append(c);
        }
        result.add(currentValue.toString().trim());
        return result;
    }

    private HttpAcceptUtils() {

    }

}
