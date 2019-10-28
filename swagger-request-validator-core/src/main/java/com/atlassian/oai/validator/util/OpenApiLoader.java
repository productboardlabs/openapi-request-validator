package com.atlassian.oai.validator.util;

import com.atlassian.oai.validator.OpenApiInteractionValidator.ApiLoadException;
import com.atlassian.oai.validator.OpenApiInteractionValidator.SpecSource;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class OpenApiLoader {

    /**
     * Loads the {@link OpenAPI} from the specified source and prepares it for usage.
     * <p>
     * See {@link #removeRegexPatternOnStringsOfFormatByte(OpenAPI)} for more information
     * on the preparation.
     *
     * @param specSource The OpenAPI / Swagger specification to use in the validator.
     * @param authData   Authentication data for reading the specification.
     * @return the loaded and prepared {@link OpenAPI}
     */
    public OpenAPI loadApi(@Nonnull final SpecSource specSource,
                           @Nonnull final List<AuthorizationValue> authData) {
        final SwaggerParseResult parseResult = readSwaggerParserResult(specSource, authData);
        if (parseResult == null || parseResult.getOpenAPI() == null ||
                (parseResult.getMessages() != null && !parseResult.getMessages().isEmpty())) {
            throw new ApiLoadException(specSource.getValue(), parseResult);
        }

        final OpenAPI api = parseResult.getOpenAPI();
        removeRegexPatternOnStringsOfFormatByte(api);
        return api;
    }

    private SwaggerParseResult readSwaggerParserResult(final SpecSource specSource, final List<AuthorizationValue> authData) {
        final OpenAPIParser openAPIParser = new OpenAPIParser();
        final ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(false);

        try {
            if (specSource.isInlineSpecification()) {
                return openAPIParser.readContents(specSource.getValue(), authData, parseOptions);
            }
            if (specSource.isSpecUrl()) {
                return openAPIParser.readLocation(specSource.getValue(), authData, parseOptions);
            }

            // Try to load as a URL first...
            final SwaggerParseResult parseResult = openAPIParser.readLocation(specSource.getValue(), authData, parseOptions);
            if (parseResult != null && parseResult.getOpenAPI() != null) {
                return parseResult;
            }

            // ...then try to load as a content string
            return openAPIParser.readContents(specSource.getValue(), authData, parseOptions);
        } catch (final RuntimeException e) {
            throw new ApiLoadException(specSource.getValue(), e);
        }
    }

    /**
     * Removes the Base64 pattern on the {@link OpenAPI} model.
     * <p>
     * If that pattern would stay on the model all fields of type string / byte would be validated twice. Once
     * with the {@link com.github.fge.jsonschema.keyword.validator.common.PatternValidator} and once with
     * the {@link com.atlassian.oai.validator.schema.format.Base64Attribute}.
     * To improve validation performance and memory footprint the pattern on string / byte fields will be
     * removed - so the PatternValidator will not be triggered for those kind of fields.
     *
     * @param openAPI the {@link OpenAPI} to correct
     */
    private static void removeRegexPatternOnStringsOfFormatByte(@Nonnull final OpenAPI openAPI) {
        if (openAPI.getPaths() != null) {
            openAPI.getPaths().values().forEach(pathItem -> {
                pathItem.readOperations().forEach(operation -> {
                    excludeBase64PatternFromEachValue(operation.getResponses(), ApiResponse::getContent);
                    if (operation.getRequestBody() != null) {
                        excludeBase64PatternFromSchema(operation.getRequestBody().getContent());
                    }
                    if (operation.getParameters() != null) {
                        operation.getParameters().forEach(it -> excludeBase64PatternFromSchema(it.getContent()));
                        operation.getParameters().forEach(it -> excludeBase64PatternFromSchema(it.getSchema()));
                    }
                });
            });
        }
        if (openAPI.getComponents() != null) {
            excludeBase64PatternFromEachValue(openAPI.getComponents().getResponses(), ApiResponse::getContent);
            excludeBase64PatternFromEachValue(openAPI.getComponents().getRequestBodies(), RequestBody::getContent);
            excludeBase64PatternFromEachValue(openAPI.getComponents().getHeaders(), Header::getContent);
            excludeBase64PatternFromEachValue(openAPI.getComponents().getHeaders(), Header::getSchema);
            excludeBase64PatternFromEachValue(openAPI.getComponents().getParameters(), Parameter::getContent);
            excludeBase64PatternFromEachValue(openAPI.getComponents().getParameters(), Parameter::getSchema);
            excludeBase64PatternFromEachValue(openAPI.getComponents().getSchemas(), schema -> schema);
        }
    }

    private static <T> void excludeBase64PatternFromEachValue(final Map<String, T> map, final Function<T, Object> function) {
        if (map != null) {
            map.values().forEach(it -> excludeBase64PatternFromSchema(function.apply(it)));
        }
    }

    private static void excludeBase64PatternFromSchema(@Nonnull final Object object) {
        if (object instanceof Content) {
            excludeBase64PatternFromEachValue((Content) object, MediaType::getSchema);
        } else if (object instanceof ObjectSchema) {
            excludeBase64PatternFromEachValue(((ObjectSchema) object).getProperties(), schema -> schema);
        } else if (object instanceof ArraySchema) {
            excludeBase64PatternFromSchema(((ArraySchema) object).getItems());
        } else if (object instanceof StringSchema) {
            final StringSchema stringSchema = (StringSchema) object;
            // remove the pattern _only_ if it's a String / Byte field
            if ("byte".equals(stringSchema.getFormat())) {
                stringSchema.setPattern(null);
            }
        }
    }
}
