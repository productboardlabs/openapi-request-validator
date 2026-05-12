Up# ADR-007: Immutable Validation Reports

**Status:** Accepted

**Date:** 2026-02-18

## Summary

Validation results are represented through immutable `ValidationReport` objects containing messages with configurable severity levels. This ensures thread-safe validation, enables flexible result handling, and provides clear success/failure semantics.

## Context

Validation results need to be:
- **Thread-safe** - Concurrent validations must not interfere
- **Reusable** - Same validation results checked multiple times
- **Flexible** - Support different error severity thresholds
- **Composable** - Combine results from multiple validations
- **Clear** - Distinguish between hard failures and warnings

Without immutability:
- Concurrent validations could corrupt shared result state
- Results could be modified after validation (confusion about when it happened)
- Difficult to share results across threads
- No clear guarantee about result consistency

The validator needed:
- Immutable result containers
- Configurable severity (error, warning, info)
- Support for aggregating validation results
- Clear pass/fail semantics based on severity

## Decision

Validation results are represented through immutable `ValidationReport` containing:

```java
public interface ValidationReport {
    /**
     * @return true if validation passed (no messages >= configured threshold)
     */
    boolean isValid();
    
    /**
     * @return true if contains any error-level messages
     */
    boolean hasErrors();
    
    /**
     * @return all validation messages (with configurable level filtering)
     */
    Collection<Message> getMessages();
    
    // Factory methods
    static ValidationReport empty() { ... }
    static ValidationReport singleton(Message message) { ... }
    static ValidationReport merge(ValidationReport... reports) { ... }
}

public interface Message {
    String getKey();           // Message key for localization
    String getMessage();       // Formatted message text
    Level getLevel();          // ERROR, WARNING, INFO
    Optional<String> getPath(); // JSON pointer to error location
    Optional<String> getContext(); // Additional context
}
```

**Key Properties:**
1. **Immutable** - Cannot be modified after creation
2. **Thread-safe** - Safe for concurrent access
3. **Composable** - Merge multiple reports via `MergedValidationReport`
4. **Filterable** - Messages filtered by severity level
5. **Localized** - Messages resolved from property files

**Message Levels:**
- `ERROR` - Validation failure (default threshold)
- `WARNING` - Suspicious but technically valid
- `INFO` - Informational messages

## Rationale

This design provides:

1. **Thread Safety** - Immutability eliminates data races
2. **Compositional** - Easy to merge reports from different validators
3. **Flexible Thresholds** - Users can set validation failure threshold
4. **Immutable History** - Results cannot be modified after validation
5. **Clear Semantics** - `isValid()` has clear definition
6. **Localization Ready** - Messages resolved through `MessageResolver`
7. **Hierarchical** - Messages can have nested paths via JSON pointers

## Alternatives Considered

1. **Mutable Result Objects** - ValidationResult with getters/setters
   - ❌ Thread safety issues with concurrent validation
   - ❌ Unclear when result is final
   - ❌ Risk of accidental mutations
   - ❌ Harder to reason about

2. **Exception-Based Reporting** - Throw exceptions for validation failure
   - ❌ Exceptions for control flow (anti-pattern)
   - ❌ Can't report multiple errors at once
   - ❌ Stack traces pollute logs
   - ❌ Less efficient

3. **Stream-Based Results** - Return Stream<Message> for lazy evaluation
   - ❌ Can't reuse results
   - ❌ Single-pass consumption
   - ❌ Harder to compose results
   - ❌ Less clear failure semantics

4. **Callback-Based** - Pass consumer function to validator
   - ❌ Requires complex callback composition
   - ❌ Less discoverable API
   - ❌ Hard to test result handling

## Consequences

### Positive Consequences
- Validation results are thread-safe (immutable)
- Results can be safely passed between threads
- Concurrent validations don't interfere with each other
- Results can be cached and reused
- Clear success/failure semantics with configurable thresholds
- Messages support localization and hierarchical paths
- Easy to compose results from multiple validations
- Results are self-contained (include context and paths)

### Negative Consequences
- Creating ValidationReport objects has memory overhead
- Message creation strings are immutable (copied each time)
- No mutable builder pattern (less flexible for some use cases)
- Must collect all messages before reporting (can't stream results)

### Impact on Development

**For Validation Code:**
```java
// Collect validation messages
List<Message> messages = new ArrayList<>();
// ... perform validation, add messages ...
return ValidationReport.of(messages);
```

**For Users of Validation Results:**
```java
ValidationReport report = validator.validate(request, response);

if (!report.isValid()) {
    // Handle validation failure
    report.getMessages().forEach(msg -> 
        logger.warn("{}: {}", msg.getKey(), msg.getMessage())
    );
}

// Results are immutable and thread-safe
executor.execute(() -> processResult(report));
```

**Message Composition:**
```java
// Messages include context for better error reporting
Message msg = Message.create("validation.request.body.required")
    .withPath("/properties/userId")
    .withContext("Field required in POST /users")
    .withLevel(Level.ERROR);
```

**Report Merging:**
```java
// Combine request and response validation results
ValidationReport requestValidation = validateRequest(...);
ValidationReport responseValidation = validateResponse(...);

ValidationReport combined = ValidationReport.merge(
    requestValidation, 
    responseValidation
);
```

## Message Severity Configuration

Message levels are determined by:
1. Default level from message key (in `default-levels.properties`)
2. Custom levels from `ValidationConfiguration`
3. Whitelist rules can suppress messages

**Default Levels File:**
```properties
# Request validation
validation.request.body.required = ERROR
validation.request.body.schema = ERROR
validation.request.parameter = ERROR

# Response validation
validation.response.status = ERROR
validation.response.body = ERROR

# Informational
validation.interaction.deprecated = WARNING
```

Users can override via `ValidationConfiguration`:
```java
ValidationConfiguration config = new ValidationConfiguration()
    .withLevelResolver(key -> {
        if (key.contains("deprecated")) return Level.INFO;
        return Level.ERROR;
    });
```

## Report Types

### ImmutableValidationReport
- Single cohesive report
- Direct message storage
- Used for most validations

### EmptyValidationReport
- Represents successful validation
- No messages, always valid
- Lightweight singleton

### MergedValidationReport
- Combines multiple reports
- Delegates to underlying reports
- Preserves composition structure

## Related ADRs

- [ADR-006: Validation Error Whitelisting](./006-validation-error-whitelisting.md)
- [ADR-001: Framework-Agnostic Request/Response Abstraction](./001-framework-agnostic-abstraction.md)

## References

- `ValidationReport.java`: Core report interface
- `ImmutableValidationReport.java`: Primary implementation
- `EmptyValidationReport.java`: Empty/success case
- `MergedValidationReport.java`: Composition of reports
- `Message.java`: Individual message interface
- `LevelResolver.java`: Message severity determination
- `default-levels.properties`: Default message levels
