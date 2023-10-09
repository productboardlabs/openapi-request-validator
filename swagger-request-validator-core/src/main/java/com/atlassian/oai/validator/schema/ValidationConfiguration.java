package com.atlassian.oai.validator.schema;

/**
 * Validation configuration for Open API validation.
 *
 * <p>This allows you to configure the following aspects of validation:</p>
 *
 * <ul>
 *     <li>The cache size of {@link com.github.fge.jsonschema.main.JsonSchema} </li>
 * </ul>
 *
 */
public class ValidationConfiguration {
    private static final int DEFAULT_MAX_CACHE_SIZE = 100;
    private int maxCacheSize = DEFAULT_MAX_CACHE_SIZE;

    /**
     * Specifies the maximum number of JsonSchema entries the cache in {@link com.atlassian.oai.validator.schema.SchemaValidator} may contain.
     * @return the maximum number of the cache.
     */
    public int getMaxCacheSize() {
        return maxCacheSize;
    }

    public ValidationConfiguration setMaxCacheSize(final int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
        return this;
    }

    /**
     * If the maxCacheSize is less than or equal to 0, then disable jsonSchemaCache in {@link com.atlassian.oai.validator.schema.SchemaValidator}.
     * @return boolean
     */
    public boolean isCacheEnabled() {
        return getMaxCacheSize() > 0;
    }
}
