# ADR-005: OpenAPI v3.0 vs v3.1 Compatibility

**Status:** Accepted

**Date:** 2026-02-18

## Summary

The validator supports both OpenAPI 3.0 and OpenAPI 3.1 specifications through runtime detection and version-specific handling, using different JSON Schema validators (Draft 4 for 3.0, 2020-12 for 3.1) and separate JSON parsing libraries.

## Context

OpenAPI 3.0 and OpenAPI 3.1 have significant differences:

**JSON Schema Version:**
- OpenAPI 3.0: Based on JSON Schema Draft 4 (with extensions)
- OpenAPI 3.1: Fully aligned with JSON Schema 2020-12

**Schema Keywords:**
- OpenAPI 3.0: Uses `nullable: true` (extension keyword)
- OpenAPI 3.1: Uses standard `type: ["string", "null"]` (multi-type arrays)

**Exclusive Min/Max:**
- OpenAPI 3.0: `exclusiveMinimum: true` (boolean)
- OpenAPI 3.1: `exclusiveMinimum: 5` (numeric, standard JSON Schema)

**Parsing:**
- OpenAPI 3.0: Uses `io.swagger.v3.core.util.Json.mapper()`
- OpenAPI 3.1: Uses `io.swagger.v3.core.util.Json31.mapper()`

Supporting both versions was critical because:
- Many users have existing OpenAPI 3.0 specs
- OpenAPI 3.1 is the current standard
- Migration between versions is non-trivial
- Breaking compatibility would fragment the user base
- Same validator should work across both versions

## Decision

**Runtime Version Detection:**
```java
boolean isOpenApi30 = api.getSpecVersion() == SpecVersion.V30;
```

The validator checks the OpenAPI spec version at initialization and configures:

1. **JSON Schema Validator:**
   - OpenAPI 3.0 → `SpecVersion.VersionFlag.V4` with `OpenApi30` meta-schema
   - OpenAPI 3.1 → `SpecVersion.VersionFlag.V202012` with `OpenApi31` meta-schema

2. **JSON Parser:**
   - OpenAPI 3.0 → `Json.mapper()` (standard Jackson mapper)
   - OpenAPI 3.1 → `Json31.mapper()` (3.1-aware Jackson mapper)

3. **Schema Transformations:**
   - Version-specific transformers apply based on `isOpenApi30` flag
   - Different handling for nullable, exclusive min/max, type arrays

**Implementation:**
```java
public SchemaValidator(OpenAPI api, MessageResolver messages) {
    this.isOpenApi30 = api.getSpecVersion() == SpecVersion.V30;
    
    // Configure JSON Schema factory
    SpecVersion.VersionFlag specVersion = isOpenApi30 
        ? SpecVersion.VersionFlag.V4 
        : SpecVersion.VersionFlag.V202012;
    
    JsonMetaSchema baseMetaSchema = isOpenApi30
        ? OpenApi30.getInstance()
        : OpenApi31.getInstance();
    
    // Use version-appropriate JSON mapper
    ObjectMapper mapper = isOpenApi30 ? Json.mapper() : Json31.mapper();
    
    // Transformers adapt behavior based on isOpenApi30 flag
}
```

## Rationale

This approach provides:

1. **Transparent Compatibility** - Same API works for both versions
2. **Correct Validation** - Each version validated against its JSON Schema spec
3. **No User Action Required** - Version detected automatically from spec
4. **Version-Specific Features** - Each version gets proper JSON Schema semantics
5. **Future-Proof** - Easy to add new versions with same pattern
6. **No Breaking Changes** - Existing 3.0 specs continue working

## Alternatives Considered

1. **Separate Validators** - Different classes for OpenAPI 3.0 and 3.1
   - ❌ Duplicates validation logic
   - ❌ Forces users to know version and choose correct validator
   - ❌ Harder to maintain consistency

2. **Convert 3.0 to 3.1** - Automatically upgrade specs to 3.1
   - ❌ Risky automated conversion (semantic differences)
   - ❌ Users may not want spec modified
   - ❌ Loses original spec for error reporting

3. **Support Only Latest** - Drop OpenAPI 3.0 support
   - ❌ Breaks existing users
   - ❌ Forces migration before validation can work
   - ❌ Not backwards compatible

4. **Manual Version Configuration** - User specifies version
   - ❌ Error-prone (version mismatch between config and spec)
   - ❌ Adds configuration complexity
   - ❌ Spec already contains version information

## Consequences

### Positive Consequences
- Seamless support for both OpenAPI 3.0 and 3.1
- Users don't need to know or configure version
- Correct JSON Schema validation for each version
- Future versions can be added with same pattern
- No breaking changes for existing users
- Version-specific features work correctly (nullable, multi-type, etc.)

### Negative Consequences
- Slightly more complex initialization logic
- Two JSON mappers and JSON Schema factories in memory
- Version detection adds small initialization overhead
- Must maintain version-specific code paths
- Testing requires covering both versions

### Impact on Development

**For Library Developers:**
- Always check `isOpenApi30` flag when version-specific behavior needed
- Use appropriate JSON mapper: `isOpenApi30 ? Json.mapper() : Json31.mapper()`
- Test features against both OpenAPI 3.0 and 3.1 specs
- Schema transformers must handle version differences
- Keep up with changes in both JSON Schema Draft 4 and 2020-12

**For Library Users:**
- No action required - version detected automatically
- Same `OpenApiInteractionValidator` API for both versions
- Validation behavior matches OpenAPI version semantics
- Error messages reference correct spec version

**Version-Specific Transformations:**
```java
if (isOpenApi30) {
    // Apply OpenAPI 3.0 transformations (e.g., exclusive min/max conversion)
} else {
    // Apply OpenAPI 3.1 transformations (e.g., multi-type support)
}
```

## Multi-Type Schema Support (OpenAPI 3.1)

OpenAPI 3.1 introduced support for `type` as an array:
```yaml
type: ["string", "null"]  # OpenAPI 3.1
```

Instead of OpenAPI 3.0's nullable extension:
```yaml
type: string
nullable: true  # OpenAPI 3.0
```

The validator handles this through:
- `hasMultipartTypeSchema()`: Detects multi-type schemas
- `validateMultiTypeSchema()`: Validates against each type, succeeds on first match
- Version-appropriate JSON Schema validator understands semantics

## Related ADRs

- [ADR-004: Schema Transformation Pipeline](./004-schema-transformation-pipeline.md)
- [ADR-001: Framework-Agnostic Request/Response Abstraction](./001-framework-agnostic-abstraction.md)

## References

- `SchemaValidator.java`: Version detection and configuration (lines 82-166)
- OpenAPI 3.0 meta-schema: `com.networknt.schema.oas.OpenApi30`
- OpenAPI 3.1 meta-schema: `com.networknt.schema.oas.OpenApi31`
- Multi-type validation: `SchemaValidator.validateMultiTypeSchema()`
- Test specs: `openapi-request-validator-core/src/test/resources/oai/v3/` and `oai/v31/`
