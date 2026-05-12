# ADR-001: Framework-Agnostic Request/Response Abstraction

**Status:** Accepted

**Date:** 2026-02-18

## Summary

The Swagger Request Validator core provides framework-independent abstraction interfaces (`Request` and `Response`) that allow the library to work with HTTP interactions from any source, without direct dependencies on specific HTTP libraries or frameworks.

## Context

The project needed to support validation of HTTP request/response interactions across multiple diverse contexts:
- REST testing frameworks (REST Assured, MockMVC)
- Contract testing tools (Pact)
- HTTP mocking libraries (WireMock)
- Real HTTP clients (Spring WebClient, Ktor)
- Custom user implementations

Each framework has its own HTTP abstraction, and building tight coupling to any single framework would:
- Limit extensibility to new frameworks
- Create dependency conflicts
- Complicate testing and maintenance
- Force users of different frameworks to choose between the validator and their preferred framework

## Decision

The core validation logic operates exclusively through two lightweight interface contracts:

1. **`Request` interface** - Abstracts:
   - HTTP method and path
   - Query parameters and headers
   - Request body (with streaming support)
   - Content-type negotiation

2. **`Response` interface** - Abstracts:
   - HTTP status code
   - Response headers
   - Response body (with streaming support)
   - Content-type detection

Each framework adapter implements these interfaces by wrapping framework-specific types. The core validator remains completely decoupled from any external HTTP library.

## Rationale

This approach provides several key benefits:

1. **Zero External Coupling** - The core library depends only on standard Java and validation libraries, not on HTTP frameworks
2. **Framework Agnostic** - Same validation logic works across all frameworks consistently
3. **Easy Integration** - New frameworks require only implementing two small interfaces
4. **Testable** - Simple mock implementations enable thorough testing without framework overhead
5. **Future Proof** - Adapting to new frameworks doesn't require core changes
6. **Clear Separation** - Validation logic is completely isolated from transport concerns

## Alternatives Considered

1. **Direct Framework Integration** - Build separate validators for each framework
   - ❌ Leads to code duplication and inconsistency
   - ❌ Maintenance nightmare as frameworks evolve
   - ❌ Cannot compose different frameworks

2. **Wrapping Framework Types Directly** - Accept framework-specific types in core
   - ❌ Couples core to external dependencies
   - ❌ Breaks when frameworks change their APIs
   - ❌ Difficult for users with custom HTTP implementations

3. **Adapter Pattern with Framework Detection** - Use reflection/classpath scanning
   - ❌ Complex, error-prone approach
   - ❌ Runtime failures instead of compile-time clarity
   - ❌ Hides coupling issues

## Consequences

### Positive Consequences
- Minimal core library footprint - no framework baggage
- Consistent validation behavior across all frameworks
- Simple abstraction enables broad adoption
- Clean architecture with clear layer separation
- Easy for users to implement custom adapters
- Reduced dependency conflicts and version management issues

### Negative Consequences
- Each framework requires an adapter implementation
- Users must use the appropriate adapter for their framework
- Small overhead of interface method calls (negligible)

### Impact on Development

**For Library Developers:**
- Core validation changes don't require framework-specific work
- Adding new framework support is well-defined: implement `Request`/`Response`
- Test implementations provide simple mocks (e.g., `SimpleRequest`, `SimpleResponse`)
- Validation logic remains pure and testable

**For Library Users:**
- Depend on `openapi-request-validator-core` + the adapter for your framework
- Cannot mix frameworks in a single validation instance (each has its adapter)
- Custom implementations possible by implementing the interface contract

**Important Constraint:**
- Core validation is **read-only** on requests/responses - no mutation
- Immutability ensures thread-safe validation across concurrent requests

## Related ADRs

- [ADR-002: Plugin Architecture for Framework Adapters](./002-plugin-adapter-architecture.md)
- [ADR-003: Streaming Body Handling Strategy](./003-streaming-body-handling.md)

## References

- Request interface: `openapi-request-validator-core/src/main/java/com/atlassian/oai/validator/model/Request.java`
- Response interface: `openapi-request-validator-core/src/main/java/com/atlassian/oai/validator/model/Response.java`
- Example implementations: `MockMvcRequest`, `RestAssuredRequest`, `PactRequest`, etc.
