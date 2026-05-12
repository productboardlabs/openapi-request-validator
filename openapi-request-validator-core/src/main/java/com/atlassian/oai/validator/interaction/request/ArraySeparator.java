package com.atlassian.oai.validator.interaction.request;

import io.swagger.v3.oas.models.parameters.Parameter;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.FORM;
import static io.swagger.v3.oas.models.parameters.Parameter.StyleEnum.SIMPLE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.removeStart;

/**
 * @see <a href="https://github.com/OAI/OpenAPI-Specification/blob/master/versions/3.0.1.md#parameterObject">OAI spec</a>
 */
class ArraySeparator {

    static ArraySeparator from(final Parameter parameter) {
        final Builder builder = new Builder()
                .withParamName(parameter.getName())
                .withSeparator(",");

        if (parameter.getStyle() == null) {
            // See https://github.com/swagger-api/swagger-parser/issues/690 - mapping from Swagger 2.0 isn't fully implemented yet
            return builder.build();
        }

        final Parameter.StyleEnum style = getStyleFromParam(parameter);
        final boolean explode = getExplodeFromParam(style, parameter);
        switch (style) {
            case SIMPLE:
                builder.withSeparator(",");
                break;
            case MATRIX:
                builder.withPrefix(";");
                if (explode) {
                    builder.withSeparator(";").withEmbeddedParamName();
                } else {
                    builder.withSeparator(",").withLeadingParamName();
                }
                break;
            case LABEL:
                builder.withSeparator("\\.").withPrefix(".");
                break;
            case FORM:
                if (explode) {
                    builder.withSeparator(null).asMultiValueParam();
                }
                break;
            case SPACEDELIMITED:
                builder.withSeparator(" ");
                break;
            case PIPEDELIMITED:
                builder.withSeparator("\\|");
                break;
            default:
        }

        return builder.build();
    }

    private static boolean getExplodeFromParam(final Parameter.StyleEnum style,
                                               final Parameter parameter) {
        // Explode defaults to "true" for "form" params; false otherwise
        if (parameter.getExplode() != null) {
            return parameter.getExplode();
        }
        return style == FORM;
    }

    private static Parameter.StyleEnum getStyleFromParam(final Parameter parameter) {
        if (parameter.getStyle() != null) {
            return parameter.getStyle();
        }
        // See https://swagger.io/specification/#parameter-object for defaults to use
        // based on the "in" field
        final String in = parameter.getIn();
        if ("query".equalsIgnoreCase(in) || "cookie".equalsIgnoreCase(in)) {
            return FORM;
        }
        return SIMPLE;
    }

    private final String paramName;
    private final String prefix;

    @Nullable
    private final String separator;

    private final boolean isMultiValueParam;
    private final boolean expectEmbeddedParamName;
    private final boolean expectLeadingParamName;

    private ArraySeparator(final String paramName,
                           @Nullable final String separator,
                           final String prefix,
                           final boolean isMultiValueParam,
                           final boolean expectEmbeddedParamName,
                           final boolean expectLeadingParamName) {
        this.paramName = paramName;
        this.separator = separator;
        this.prefix = prefix;
        this.isMultiValueParam = isMultiValueParam;
        this.expectEmbeddedParamName = expectEmbeddedParamName;
        this.expectLeadingParamName = expectLeadingParamName;
    }

    boolean isMultiValueParam() {
        return isMultiValueParam;
    }

    Collection<String> split(final String value) {
        if (separator == null) {
            return singletonList(value);
        }
        if (value.isEmpty()) {
            return emptyList();
        }

        String valueToSplit = removeStart(value, prefix);
        if (expectLeadingParamName) {
            valueToSplit = removeStart(valueToSplit, paramName + "=");
        }

        List<String> result = asList(valueToSplit.split(separator));
        if (expectEmbeddedParamName) {
            result = result.stream()
                    .map(v -> removeStart(v, paramName + "="))
                    .collect(toList());
        }
        return result;
    }

    private static final class Builder {
        private String paramName;
        private String separator;
        private String prefix = "";
        private boolean isMultiValueParam = false;
        private boolean expectEmbeddedParamName = false;
        private boolean expectLeadingParamName = false;

        private Builder() {
        }

        private Builder withParamName(final String paramName) {
            this.paramName = paramName;
            return this;
        }

        private Builder withSeparator(final String separator) {
            this.separator = separator;
            return this;
        }

        private Builder asMultiValueParam() {
            this.isMultiValueParam = true;
            return this;
        }

        private Builder withPrefix(final String prefix) {
            this.prefix = prefix;
            return this;
        }

        private Builder withEmbeddedParamName() {
            this.expectEmbeddedParamName = true;
            return this;
        }

        private Builder withLeadingParamName() {
            this.expectLeadingParamName = true;
            return this;
        }

        private ArraySeparator build() {
            return new ArraySeparator(
                    paramName,
                    separator,
                    prefix,
                    isMultiValueParam,
                    expectEmbeddedParamName,
                    expectLeadingParamName);
        }
    }
}
