# ADR-002: Plugin Architecture for Framework Adapters

**Status:** Accepted

**Date:** 2026-02-18

## Summary

Framework integrations are structured as independent modules that implement the adapter pattern, providing framework-specific implementations of the `Request` and `Response` interfaces. Each adapter is a separate Maven module that can be used independently.

## Context

The project supports 8+ different frameworks (Spring MockMVC, Spring WebMVC, REST Assured, Pact, WireMock, Ktor, Spring WebClient, and others). Without a clear architectural pattern for integrations:
- Each framework would have its own integration approach
- Code would be inconsistent and difficult to understand
- Module organization would be unclear
- Future framework additions would lack guidance

The project needed a standardized way to:
- Organize framework integrations
- Enable independent versioning of adapters
- Allow users to include only needed adapters
- Provide clear integration points

## Decision

Framework integrations follow a **Maven module per framework** pattern:

```
openapi-request-validator-{framework}/
├── src/main/java/
│   └── com/atlassian/oai/validator/{framework}/
│       ├── {Framework}Request.java          # Request implementation
│       ├── {Framework}Response.java         # Response implementation
│       ├── {Framework}Validator.java        # Optional: convenience facade
│       └── ...other framework-specific code
├── src/test/java/                          # Framework-specific tests
├── src/test/resources/                     # Framework test fixtures
└── pom.xml                                 # Framework-specific dependencies
```

Each adapter:
1. Depends on `openapi-request-validator-core` (no circular dependencies)
2. Implements `Request` and/or `Response` interfaces
3. Wraps framework-specific HTTP types
4. Provides framework-idiomatic API (e.g., `OpenApiMatchers` for MockMVC, filters for REST Assured)
5. Has independent test coverage

## Rationale

This architecture provides multiple benefits:

1. **Independent Modules** - Users include only adapters for frameworks they use
2. **Clear Ownership** - Each adapter has a dedicated module and test suite
3. **Flexible Versioning** - Adapter versions can evolve independently from core
4. **Familiar Integration** - Each adapter uses framework-idiomatic patterns
5. **Separation of Concerns** - Framework-specific code isolated from validation logic
6. **Scalability** - New frameworks can be added without touching core or other adapters
7. **Optional Dependencies** - Core builds without any framework dependencies

## Alternatives Considered

1. **Monolithic Approach** - All integrations in a single module
   - ❌ Core would have all framework dependencies
   - ❌ Bloated artifact even if only using one framework
   - ❌ Hard to manage different framework version requirements

2. **Single Adapter Module** - One module handles all frameworks
   - ❌ Creates hidden coupling between frameworks
   - ❌ Difficult to test framework-specific code
   - ❌ Complex dependency management

3. **Auto-Discovery Plugin System** - Runtime classpath scanning
   - ❌ Adds complexity and runtime overhead
   - ❌ Failures become runtime, not compile-time
   - ❌ Harder to debug

## Consequences

### Positive Consequences
- Clean separation of framework-specific code from validation logic
- Users only depend on adapters they actually need
- Easy to add new framework support
- Each adapter can evolve independently
- Framework expertise concentrated in dedicated modules
- Clear testing strategy per framework

### Negative Consequences
- More modules to maintain and document
- Users need to know which adapter matches their framework
- Potential duplication if adapters share similar code
- More complex project structure to navigate

### Impact on Development

**For Library Developers (Adding a New Framework):**
1. Create `openapi-request-validator-{framework}` module
2. Implement `Request` and `Response` interfaces
3. Write framework-idiomatic facade (optional)
4. Add framework-specific tests
5. Reference in parent `pom.xml`
6. Document in README

**For Library Maintainers:**
- Core changes don't require touching adapters
- Can release adapters independently when needed
- CI/CD can test each adapter with its framework version
- Each adapter has clear success criteria

**For Users:**
- Maven coordinate: `com.atlassian.oai:openapi-request-validator-{framework}`
- Import only adapters needed (smaller dependency footprint)
- Framework-idiomatic usage: `@ValidateOpenApiRequest` in Spring, filters in REST Assured, etc.

## Established Adapters

| Framework | Module | Primary Use |
|---|---|---|
| **Spring MockMVC** | `openapi-request-validator-mockmvc` | Testing Spring MVC controllers |
| **Spring WebMVC** | `openapi-request-validator-spring-webmvc` | Runtime production validation |
| **Spring WebClient** | `openapi-request-validator-spring-web-client` | HTTP client validation |
| **REST Assured** | `openapi-request-validator-restassured` | Integration test validation |
| **WireMock** | `openapi-request-validator-wiremock` | Mock validation (JUnit 4) |
| **WireMock JUnit5** | `openapi-request-validator-wiremock-junit5` | Mock validation (JUnit 5) |
| **Pact** | `openapi-request-validator-pact` | Consumer test validation |
| **Ktor Client** | `openapi-request-validator-ktor-client` | Kotlin HTTP client validation |

## Related ADRs

- [ADR-001: Framework-Agnostic Request/Response Abstraction](./001-framework-agnostic-abstraction.md)
- [ADR-004: Schema Transformation Pipeline](./004-schema-transformation-pipeline.md)

## References

- Adapter modules: All `openapi-request-validator-{framework}` directories
- Example: `openapi-request-validator-mockmvc/src/main/java/com/atlassian/oai/validator/mockmvc/`
- Parent POM: `pom.xml` (modules section)
