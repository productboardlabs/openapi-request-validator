# ADR-004: Schema Transformation Pipeline

**Status:** Accepted

**Date:** 2026-02-18

## Summary

OpenAPI schemas undergo transformations through a composable pipeline of `SchemaTransformer` implementations before JSON Schema validation. This enables the validator to handle OpenAPI-specific features, schema references, and version differences while delegating to standard JSON Schema validation libraries.

## Context

OpenAPI specifications are not pure JSON Schema. They include:
- Schema references (`$ref`) pointing to components
- OpenAPI-specific keywords (`discriminator`, `nullable`, `readOnly`, `writeOnly`)
- Additional properties constraints specific to OpenAPI
- Differences between OpenAPI 3.0 (JSON Schema Draft 4) and OpenAPI 3.1 (JSON Schema 2020-12)
- Context-specific validation (request vs. response)

Direct JSON Schema validation against raw OpenAPI schemas would fail because:
- JSON Schema validators don't understand OpenAPI `$ref` paths
- `nullable`, `discriminator` are OpenAPI extensions, not JSON Schema keywords
- OpenAPI 3.0 uses exclusive min/max differently than JSON Schema Draft 4
- Request-only and response-only fields (`readOnly`/`writeOnly`) need contextual handling
- Additional properties should be validated per OpenAPI semantics

The validator needed a way to:
- Normalize OpenAPI schemas to valid JSON Schema
- Handle schema references consistently
- Support version-specific transformations
- Apply context-aware transformations (request vs. response)
- Keep transformation logic modular and testable

## Decision

OpenAPI schemas are transformed through a **pipeline of composable transformers** before validation:

```java
List<SchemaTransformer> transformers = [
    SchemaDefinitionsInjectionTransformer,  // Resolve $ref to definitions
    ExclusiveMinMaxTransformer,             // Normalize exclusive min/max
    AdditionalPropertiesInjectionTransformer, // Handle additionalProperties
    RequiredFieldTransformer                // Context-aware required fields
];
```

Each transformer:
1. Receives the schema as a mutable `ObjectNode`
2. Receives transformation context (request/response, definitions, flags)
3. Applies specific mutations to normalize the schema
4. Passes result to next transformer

**Transformation Context:**
```java
SchemaTransformationContext {
    boolean forRequest;
    boolean forResponse;
    JsonNode definitions;           // Component schemas
    boolean additionalPropertiesValidation;
    boolean isOpenApi30;
}
```

**Order Matters:** Transformations apply in sequence, with each building on previous transformations.

## Rationale

This architecture provides:

1. **Separation of Concerns** - Each transformer handles one aspect of normalization
2. **Composability** - Easy to add/remove transformers for specific use cases
3. **Testability** - Each transformer can be tested independently
4. **Context Awareness** - Request vs. response transformations applied correctly
5. **Version Flexibility** - OpenAPI 3.0 vs 3.1 differences isolated in transformers
6. **Delegated Validation** - After transformation, standard JSON Schema validators work
7. **Clear Dependencies** - Transformation order explicitly defined

## Key Transformers

### 1. SchemaDefinitionsInjectionTransformer
- **Purpose:** Resolve OpenAPI `$ref` paths to actual schema definitions
- **How:** Injects `definitions` or `$defs` section with all component schemas
- **Why:** JSON Schema validators need local references, not OpenAPI component paths

### 2. ExclusiveMinMaxTransformer
- **Purpose:** Convert OpenAPI 3.0 `exclusiveMinimum`/`exclusiveMaximum` to JSON Schema format
- **How:** Transforms boolean flags to numeric boundary values
- **Why:** OpenAPI 3.0 uses different syntax than JSON Schema Draft 4

### 3. AdditionalPropertiesInjectionTransformer
- **Purpose:** Apply `additionalProperties: false` when OpenAPI spec doesn't allow extras
- **How:** Adds `additionalProperties: false` to object schemas without explicit setting
- **Why:** Enforces OpenAPI strict schema compliance

### 4. RequiredFieldTransformer
- **Purpose:** Handle `readOnly`/`writeOnly` fields based on context
- **How:** Removes `readOnly` from required fields in requests, `writeOnly` from responses
- **Why:** OpenAPI semantics: `readOnly` = response-only, `writeOnly` = request-only

## Alternatives Considered

1. **Custom JSON Schema Validator** - Build OpenAPI-aware JSON Schema validator
   - ❌ Reinventing the wheel (JSON Schema validation is complex)
   - ❌ Harder to maintain and keep up with JSON Schema evolution
   - ❌ Loses benefits of battle-tested validation libraries

2. **Pre-process Entire Spec** - Transform entire OpenAPI spec upfront
   - ❌ Wasteful (most schemas never validated)
   - ❌ Loses original spec for error reporting
   - ❌ Complex caching strategy needed

3. **Ad-hoc Transformations** - Apply transformations directly in validation code
   - ❌ Tightly couples validation to transformation
   - ❌ Hard to test transformations independently
   - ❌ Difficult to add new transformations

4. **Runtime Schema Patching** - Modify schema during validation
   - ❌ Confusing execution model
   - ❌ Side effects during validation
   - ❌ Thread-safety concerns

## Consequences

### Positive Consequences
- Clean separation between schema normalization and validation
- Easy to add new transformations for OpenAPI features
- Each transformer is independently testable
- Works with standard JSON Schema validators (networknt/json-schema-validator)
- Version differences (3.0 vs 3.1) handled transparently
- Context-aware transformations (request/response) built-in
- Immutable original schema preserved for error reporting

### Negative Consequences
- Adds transformation overhead before validation
- Transformation order must be carefully managed
- Schema transformations create copies (memory overhead)
- Complexity in understanding full transformation pipeline
- Deep copies required for thread-safety (prevents concurrent validation mutation)

### Impact on Development

**For Validation Logic:**
- Schema transformation happens automatically before validation
- Validators receive normalized JSON Schema-compliant schemas
- No OpenAPI-specific handling in validation code
- Trust that transformers produce valid JSON Schema

**For Adding New Transformations:**
1. Implement `SchemaTransformer` interface
2. Add to transformer list in `SchemaValidator` constructor
3. Consider correct order in pipeline
4. Test transformation independently
5. Ensure thread-safe (no shared mutable state)

**For OpenAPI Version Support:**
- Version-specific transformations controlled by `isOpenApi30` flag
- Different JSON Schema spec versions (Draft 4 vs 2020-12) handled
- Transformations can branch on version when needed

**Performance Considerations:**
- Transformations applied once per unique schema (cached)
- Deep copy of definitions required for thread-safety
- Schema cache enabled by default to amortize transformation cost

## Caching Strategy

Transformed schemas are cached using a composite key:
```java
JsonSchemaKey {
    Schema schema;
    boolean forRequest;
    boolean forResponse;
}
```

This ensures:
- Same schema with different contexts gets separate cache entries
- Transformations only run once per unique schema + context combination
- Thread-safe validation with immutable cached schemas

## Related ADRs

- [ADR-001: Framework-Agnostic Request/Response Abstraction](./001-framework-agnostic-abstraction.md)
- [ADR-005: OpenAPI v3.0 vs v3.1 Compatibility](./005-openapi-version-handling.md)

## References

- Schema transformers: `swagger-request-validator-core/src/main/java/com/atlassian/oai/validator/schema/transform/`
- `SchemaValidator.java`: Transformation pipeline implementation
- `SchemaTransformationContext.java`: Context passed through transformers
