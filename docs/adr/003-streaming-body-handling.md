# ADR-003: Streaming Body Handling Strategy

**Status:** Accepted

**Date:** 2026-02-18

## Summary

Request and response bodies are abstracted through a `Body` interface supporting multiple implementations (String, ByteArray, InputStream), enabling efficient handling of large payloads and streaming scenarios without loading entire bodies into memory.

## Context

HTTP bodies can be very large, and validating against OpenAPI specs requires:
- Parsing JSON/XML from bodies
- Validating content against schemas
- Supporting various HTTP contexts (servlet, client, test frameworks)

Different contexts provide bodies differently:
- Servlet request input streams must be read once
- Byte arrays from in-memory mocks
- String responses from test frameworks
- Streaming responses from HTTP clients

Loading entire bodies into memory as strings has limitations:
- Fails with large payloads
- Forces unnecessary serialization
- Incompatible with streaming HTTP clients
- Problematic in servlet contexts where streams can only be read once

The validator needed a flexible body representation that:
- Supports multiple formats without conversion overhead
- Works with streaming contexts (avoiding stream exhaustion)
- Handles large payloads efficiently
- Works uniformly across all adapters

## Decision

Bodies are represented through a `Body` interface with multiple implementations:

```java
public interface Body {
    boolean hasBody();
    JsonNode toJsonNode() throws IOException;
    String toString(Charset encoding) throws IOException;
}
```

**Implementations:**

1. **`StringBody`** - In-memory body as String
   - Best for: Test frameworks, already-loaded responses
   - Use when: Framework provides String representation

2. **`ByteArrayBody`** - In-memory body as byte array
   - Best for: Byte manipulation, exact content control
   - Use when: Framework provides byte[] representation

3. **`InputStreamBody`** - Streaming body from InputStream
   - Best for: Large payloads, servlet requests, streaming clients
   - Use when: Framework provides stream without buffering

**Validation Flow:**
1. Validator requests body as `JsonNode` via `toJsonNode()`
2. Body implementation converts from its native format
3. JSON Schema validation operates on the `JsonNode`
4. Stream is closed/reset after validation

## Rationale

This design provides:

1. **Memory Efficiency** - No unnecessary copies or buffering
2. **Stream Compatibility** - Works with streaming HTTP contexts
3. **Format Flexibility** - Supports multiple body representations
4. **Lazy Evaluation** - Body not parsed until validation needs it
5. **Adapter Independence** - Each adapter chooses appropriate Body type
6. **Error Handling** - Clear exception contract for body parsing failures

## Alternatives Considered

1. **Always Convert to String** - Require all adapters to provide String bodies
   - ❌ Defeats streaming support
   - ❌ Inefficient for large payloads
   - ❌ Forces serialization overhead

2. **Always Convert to Bytes** - Single ByteArray representation
   - ❌ Still loads entire body into memory
   - ❌ Incompatible with true streaming clients
   - ❌ More memory overhead than streaming

3. **Lazy Stream Wrapper** - Parse stream only when needed
   - ✓ Reduces memory for unvalidated streams
   - ❌ Complex stream state management
   - ❌ Risk of stream exhaustion
   - ❌ Hard to debug stream issues

4. **Framework-Specific Body Types** - Different classes per framework
   - ❌ Validation logic becomes framework-aware
   - ❌ Defeats abstraction principle (ADR-001)
   - ❌ Requires core changes for each framework

## Consequences

### Positive Consequences
- Works efficiently with large payloads and streams
- No unnecessary memory overhead or copying
- Flexible format support (string, bytes, stream)
- Each adapter can choose optimal format
- Streaming HTTP clients can work without buffering
- Lazy parsing defers cost until validation actually needs body

### Negative Consequences
- Multiple Body implementations to maintain
- Adapters must correctly choose Body type
- Stream exhaustion possible if misused
- Conversion to JSON requires copying data (inherent to JSON validation)

### Impact on Development

**For Adapter Developers:**
- Choose appropriate Body implementation:
  - Use `StringBody` if framework already has String representation
  - Use `ByteArrayBody` for byte[] availability
  - Use `InputStreamBody` for streaming contexts (servlet, streaming clients)
- Implement body retrieval consistently in `Request`/`Response`
- Ensure streams are properly managed (closed after use)

**For Validation Code:**
- Access bodies through `Body` interface only
- Call `toJsonNode()` when schema validation needed
- No direct stream handling in validation logic
- Assume body can be read once per validation

**For Users:**
- No difference in validation behavior between body types
- Framework adapter handles body format transparently
- Large payloads work automatically without special handling

## Special Case: Servlet Request Bodies

Servlet request input streams can only be read once. This required special handling:

- **`ResettableRequestServletWrapper`** - Caches request body on first read
- **`ResettableInputStreamBody`** - Wraps cached stream for reuse
- **Spring WebMVC Adapter** - Uses these special types for servlet contexts

This ensures servlet request validation works despite stream limitations.

## Related ADRs

- [ADR-001: Framework-Agnostic Request/Response Abstraction](./001-framework-agnostic-abstraction.md)
- [ADR-004: Schema Transformation Pipeline](./004-schema-transformation-pipeline.md)

## References

- Body interface: `swagger-request-validator-core/src/main/java/com/atlassian/oai/validator/model/Body.java`
- Implementations: `StringBody.java`, `ByteArrayBody.java`, `InputStreamBody.java`
- Servlet wrapper: `swagger-request-validator-spring-webmvc/src/main/java/com/atlassian/oai/validator/springmvc/ResettableRequestServletWrapper.java`
