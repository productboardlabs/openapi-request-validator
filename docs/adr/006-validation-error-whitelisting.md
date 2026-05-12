# ADR-006: Validation Error Whitelisting

**Status:** Accepted

**Date:** 2026-02-18

## Summary

The validator provides a composable rule-based system for selectively suppressing validation errors through `ValidationErrorsWhitelist`. This allows users to acknowledge known discrepancies between specs and implementations without disabling entire validation categories.

## Context

In real-world usage, there are legitimate scenarios where implementations intentionally deviate from OpenAPI specifications:
- **Gradual Migration:** Legacy APIs being incrementally migrated to match specs
- **Backward Compatibility:** Implementations providing extra fields for old clients
- **Spec Limitations:** Implementation details not expressible in OpenAPI
- **Known Issues:** Documented discrepancies being addressed over time
- **Test Environments:** Test data that doesn't perfectly match production constraints

Without selective error suppression:
- Users must choose: disable validation entirely OR fail on every discrepancy
- No way to acknowledge known issues while catching new problems
- Cannot enforce validation on new code while allowing legacy exceptions
- All-or-nothing validation is too rigid for real-world scenarios

The validator needed a mechanism to:
- Suppress specific validation errors selectively
- Define error suppression rules programmatically
- Compose multiple suppression rules
- Document WHY errors are suppressed
- Avoid disabling entire validation categories

## Decision

Provide a **composable whitelist system** using the `ValidationErrorsWhitelist` API:

```java
ValidationErrorsWhitelist whitelist = ValidationErrorsWhitelist.create()
    .withRule("Legacy user endpoint returns extra fields",
        WhitelistRules.allOf(
            WhitelistRules.messageHasKey("validation.response.body.schema.additionalProperties"),
            WhitelistRules.pathMatches("/api/users/.*")
        ))
    .withRule("Known date format issue in legacy responses",
        WhitelistRules.allOf(
            WhitelistRules.messageContains("format date-time"),
            WhitelistRules.operationMatches("GET", "/api/legacy/.*")
        ));

validator = OpenApiInteractionValidator.createFor(spec)
    .withWhitelist(whitelist)
    .build();
```

**Core Components:**

1. **`ValidationErrorsWhitelist`** - Immutable collection of named whitelist rules
2. **`WhitelistRule`** - Functional interface matching validation messages
3. **`WhitelistRules`** - Factory providing composable rule builders:
   - `messageHasKey()` - Match specific validation message keys
   - `messageContains()` - Match message content
   - `pathMatches()` - Match request paths (regex)
   - `operationMatches()` - Match HTTP method + path
   - `allOf()`, `anyOf()` - Compose multiple rules with AND/OR logic
   - `forRequest()`, `forResponse()` - Limit to request or response validation

4. **Named Rules** - Each rule has a descriptive title explaining WHY it exists

## Rationale

This design provides:

1. **Selective Suppression** - Suppress specific errors, not entire categories
2. **Self-Documenting** - Rule names explain why errors are whitelisted
3. **Composability** - Combine simple rules into complex conditions
4. **Type Safety** - Compile-time checked rule construction
5. **Immutability** - Whitelist cannot be modified after creation
6. **Flexibility** - Match on any combination of message, path, operation, entity type
7. **Auditability** - Whitelist rules are explicit and reviewable

## Whitelist Rule Examples

### Example 1: Allow Extra Properties on Specific Endpoint
```java
whitelist.withRule("Legacy endpoint returns undocumented fields",
    WhitelistRules.allOf(
        WhitelistRules.messageHasKey("validation.response.body.schema.additionalProperties"),
        WhitelistRules.pathMatches("/api/v1/legacy/.*")
    ));
```

### Example 2: Ignore Validation for Specific HTTP Method
```java
whitelist.withRule("OPTIONS requests not fully spec'd",
    WhitelistRules.allOf(
        WhitelistRules.operationMatches("OPTIONS", ".*"),
        WhitelistRules.forRequest()
    ));
```

### Example 3: Complex Composition
```java
whitelist.withRule("Known issues in user endpoints",
    WhitelistRules.anyOf(
        // Either additional properties on GET /users
        WhitelistRules.allOf(
            WhitelistRules.messageHasKey("validation.response.body.schema.additionalProperties"),
            WhitelistRules.operationMatches("GET", "/api/users/[0-9]+")
        ),
        // Or missing required field on POST /users
        WhitelistRules.allOf(
            WhitelistRules.messageContains("required property"),
            WhitelistRules.operationMatches("POST", "/api/users")
        )
    ));
```

## Alternatives Considered

1. **Global Disable Flags** - Configuration to disable specific validation types
   - ❌ Too coarse-grained (disables everywhere, not selectively)
   - ❌ No documentation of WHY disabled
   - ❌ Cannot scope to specific endpoints

2. **Comment-Based Suppression** - Annotations in OpenAPI specs
   - ❌ Pollutes spec files with implementation details
   - ❌ Cannot suppress errors for external specs
   - ❌ Requires spec modification (may not be possible)

3. **Callback-Based Filtering** - User provides filter function
   - ❌ Less discoverable API
   - ❌ No built-in composability
   - ❌ Harder to unit test filtering logic

4. **Configuration File** - External YAML/JSON defining suppressions
   - ❌ Adds configuration file management complexity
   - ❌ Type safety lost
   - ❌ Harder to maintain alongside code

## Consequences

### Positive Consequences
- Validation errors can be selectively suppressed with clear reasoning
- Composable rules enable complex matching logic
- Immutable design ensures thread-safe usage
- Named rules provide self-documentation
- Easy to audit which errors are being suppressed
- Enables gradual spec compliance improvements
- Doesn't require modifying OpenAPI specs

### Negative Consequences
- Risk of over-whitelisting (suppressing too many errors)
- Whitelist rules must be maintained alongside code
- Can hide real issues if rules are too broad
- Adds API surface area to learn

### Impact on Development

**For Library Users:**
- Build whitelists programmatically during validator construction
- Use `WhitelistRules` factory for common patterns
- Compose rules with `allOf()` / `anyOf()` for complex logic
- Provide descriptive names explaining WHY errors are whitelisted

**Best Practices:**
1. **Be Specific** - Whitelist narrow errors, not broad categories
2. **Document Reasons** - Always explain WHY error is whitelisted
3. **Temporary Suppressions** - Add TODO comments for issues to fix
4. **Review Regularly** - Audit whitelist rules periodically
5. **Avoid Wildcards** - Prefer specific paths over regex `.*`

**Anti-Patterns to Avoid:**
```java
// ❌ TOO BROAD - Suppresses all additional properties errors
whitelist.withRule("Allow extra fields",
    WhitelistRules.messageHasKey("validation.response.body.schema.additionalProperties"));

// ✅ BETTER - Specific endpoint and reason
whitelist.withRule("User endpoint includes computed 'age' field not in spec",
    WhitelistRules.allOf(
        WhitelistRules.messageHasKey("validation.response.body.schema.additionalProperties"),
        WhitelistRules.pathMatches("/api/users/[0-9]+"),
        WhitelistRules.forResponse()
    ));
```

## Integration with Validation Flow

When validation errors occur:
1. Validator generates `ValidationReport` with messages
2. Each message checked against whitelist rules
3. If matched by a rule, message marked as whitelisted
4. Whitelisted messages don't cause validation failure
5. Validation report includes which rule suppressed each message

This allows:
- Logging of whitelisted errors for monitoring
- Detecting when whitelisted errors no longer occur (rule cleanup)
- Auditing suppressed validation failures

## Related ADRs

- [ADR-007: Immutable Validation Reports](./007-immutable-validation-reports.md)
- [ADR-004: Schema Transformation Pipeline](./004-schema-transformation-pipeline.md)

## References

- `ValidationErrorsWhitelist.java`: Whitelist API implementation
- `WhitelistRule.java`: Rule interface
- `WhitelistRules.java`: Rule factory and builders
- Example usage: `openapi-request-validator-examples/src/test/java/com/atlassian/oai/validator/examples/whitelist/`
