package com.atlassian.oai.validator.util;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Minimal media type representation for content-type parsing and matching.
 * <p>
 * Supports wildcard matching (e.g. {@code *}{@code /*}, {@code application/*}) and parameter stripping.
 * <p>
 * This is a package-private replacement for {@code com.google.common.net.MediaType}.
 */
public final class MediaType {

    static final MediaType ANY_TYPE = new MediaType("*", "*", Collections.emptyMap());
    static final MediaType ANY_APPLICATION_TYPE = new MediaType("application", "*", Collections.emptyMap());
    static final MediaType ANY_TEXT_TYPE = new MediaType("text", "*", Collections.emptyMap());
    static final MediaType ANY_IMAGE_TYPE = new MediaType("image", "*", Collections.emptyMap());

    // Common constants used by ContentTypeUtils
    static final MediaType JSON_UTF_8 = new MediaType("application", "json",
            Collections.singletonMap("charset", "utf-8"));
    static final MediaType FORM_DATA = new MediaType("application", "x-www-form-urlencoded",
            Collections.emptyMap());

    private final String type;
    private final String subtype;
    private final Map<String, String> parameters;

    private MediaType(final String type, final String subtype, final Map<String, String> parameters) {
        this.type = type;
        this.subtype = subtype;
        this.parameters = Collections.unmodifiableMap(parameters);
    }

    /**
     * Parse a raw content-type string into a {@link MediaType}.
     *
     * @param input the content-type string (e.g. {@code "application/json; charset=utf-8"})
     * @return the parsed {@link MediaType}
     * @throws IllegalArgumentException if the input is null or malformed (missing {@code /})
     */
    public static MediaType parse(@Nullable final String input) {
        if (input == null) {
            throw new IllegalArgumentException("MediaType input must not be null");
        }
        final String trimmed = input.trim();
        final String[] parts = trimmed.split(";", -1);
        final String base = parts[0].trim();
        final int slashIdx = base.indexOf('/');
        if (slashIdx < 0) {
            throw new IllegalArgumentException("Invalid media type (missing '/'): " + input);
        }
        final String type = base.substring(0, slashIdx).trim().toLowerCase();
        final String subtype = base.substring(slashIdx + 1).trim().toLowerCase();

        final Map<String, String> params = new LinkedHashMap<>();
        for (int i = 1; i < parts.length; i++) {
            final String param = parts[i].trim();
            if (param.isEmpty()) {
                continue;
            }
            final int eqIdx = param.indexOf('=');
            if (eqIdx < 0) {
                params.put(param.toLowerCase(), "");
            } else {
                final String key = param.substring(0, eqIdx).trim().toLowerCase();
                String value = param.substring(eqIdx + 1).trim();
                // Strip surrounding quotes
                if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                    value = value.substring(1, value.length() - 1);
                }
                params.put(key, value.toLowerCase());
            }
        }
        return new MediaType(type, subtype, params);
    }

    /**
     * Returns the primary type (e.g. {@code "application"}).
     */
    public String type() {
        return type;
    }

    /**
     * Returns the subtype (e.g. {@code "json"}).
     */
    public String subtype() {
        return subtype;
    }

    /**
     * Returns a new {@link MediaType} with the same type and subtype but no parameters.
     */
    public MediaType withoutParameters() {
        if (parameters.isEmpty()) {
            return this;
        }
        return new MediaType(type, subtype, Collections.emptyMap());
    }

    /**
     * Returns whether this media type is compatible with {@code other}, respecting wildcards and parameters.
     * <p>
     * A wildcard ({@code *}) in {@code other} matches any value in {@code this}.
     * For example: {@code parse("application/json").is(parse("application/*"))} → {@code true}.
     * <p>
     * If {@code other} has parameters, this type must have matching parameter values (case-insensitive).
     *
     * @param other the media type to check compatibility against (may contain wildcards)
     * @return {@code true} if this type is compatible with {@code other}
     */
    public boolean is(final MediaType other) {
        final boolean typeMatch = other.type.equals("*") || other.type.equalsIgnoreCase(this.type);
        final boolean subtypeMatch = other.subtype.equals("*") || other.subtype.equalsIgnoreCase(this.subtype);
        if (!typeMatch || !subtypeMatch) {
            return false;
        }
        // If other has parameters, all of other's parameters must match this type's parameters (case-insensitive values)
        for (final Map.Entry<String, String> entry : other.parameters.entrySet()) {
            final String thisValue = this.parameters.get(entry.getKey());
            if (thisValue == null || !thisValue.equalsIgnoreCase(entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the number of parameters defined on this media type.
     */
    public int parameterCount() {
        return parameters.size();
    }

    /**
     * Returns the charset parameter value, if present.
     */
    public Optional<Charset> charset() {
        final String charsetValue = parameters.get("charset");
        if (charsetValue == null || charsetValue.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Charset.forName(charsetValue));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Guava compatibility: returns charset as an optional (using Guava's Optional-like pattern).
     * Returns {@code null} if no charset is present (for use with {@code Optional.ofNullable}).
     */
    @Nullable
    public Charset charsetOrNull() {
        return charset().orElse(null);
    }

    @Override
    public String toString() {
        if (parameters.isEmpty()) {
            return type + "/" + subtype;
        }
        final StringBuilder sb = new StringBuilder(type).append('/').append(subtype);
        parameters.forEach((k, v) -> {
            sb.append("; ").append(k);
            if (!v.isEmpty()) {
                sb.append('=').append(v);
            }
        });
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MediaType)) {
            return false;
        }
        final MediaType other = (MediaType) o;
        return type.equals(other.type) && subtype.equals(other.subtype) && parameters.equals(other.parameters);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + subtype.hashCode();
        result = 31 * result + parameters.hashCode();
        return result;
    }
}
