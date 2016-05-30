# Swagger Request Validator #

A Java library for validating HTTP request/responses against a Swagger/OpenAPI specification.

Designed to be used independently of any HTTP library or framework, the library can be used to validate
request/responses from almost any source (e.g. in a REST client, in unit tests that use mocked responses,
in Pact tests etc.)

## Key features ##

* Standalone - no dependencies on HTTP libraries or frameworks
* Adapters for commonly used HTTP libraries and testing frameworks
* JSON Schema validation support - including schema references

## Project structure ##
See individual module READMEs for more information.

**swagger-request-validator-core**

The core validator logic.

Provides a standalone validator and uses an implementation-agnostic abstraction of
HTTP request/responses that can be adapted to any 3rd party implementation.

**swagger-request-validator-pact**

Adapters for validating Pact request/response expectations with the Swagger validator.

Includes a JUnit rule that adds Swagger/OAI validation to the Pact-JVM consumer test execution.

See: https://github.com/DiUS/pact-jvm

