# Architecture Decision Records (ADRs)

This directory contains Architecture Decision Records (ADRs) for the Swagger Request Validator project. ADRs document important architectural decisions, constraints, and design patterns that guide development.

## What is an ADR?

An ADR is a lightweight document that records a significant architectural decision made on a project. It captures:
- The decision context and problem
- Why the decision was made
- Trade-offs and alternatives considered
- Consequences (benefits and drawbacks)

## ADR Index

| ID | Title | Status | Date |
|---|---|---|---|
| [001](./001-framework-agnostic-abstraction.md) | Framework-Agnostic Request/Response Abstraction | Accepted | 2026-02-18 |
| [002](./002-plugin-adapter-architecture.md) | Plugin Architecture for Framework Adapters | Accepted | 2026-02-18 |
| [003](./003-streaming-body-handling.md) | Streaming Body Handling Strategy | Accepted | 2026-02-18 |
| [004](./004-schema-transformation-pipeline.md) | Schema Transformation Pipeline | Accepted | 2026-02-18 |
| [005](./005-openapi-version-handling.md) | OpenAPI v3.0 vs v3.1 Compatibility | Accepted | 2026-02-18 |
| [006](./006-validation-error-whitelisting.md) | Validation Error Whitelisting | Accepted | 2026-02-18 |
| [007](./007-immutable-validation-reports.md) | Immutable Validation Reports | Accepted | 2026-02-18 |

## Guidelines for Reading ADRs

- ADRs are organized chronologically by ID
- Read the summary first for quick understanding
- See the "Consequences" section for impact on development
- Review "Alternatives Considered" to understand trade-offs

## Contributing New ADRs

When documenting new architectural decisions:
1. Use the next sequential ID number
2. Follow the standard template (see `TEMPLATE.md`)
3. Get team consensus before marking as "Accepted"
4. Update this README with the new ADR
