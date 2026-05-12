# Swagger Request Validator Features #

* [Swagger v2 support](SWAGGERv2.md)
* [OpenAPI v3 support](OPENAPIv3.md)
* Standalone - no dependencies on HTTP libraries or frameworks
* Adapters for commonly used HTTP libraries and testing frameworks
    * [Spring MockMVC](../openapi-request-validator-mockmvc/README.md)
    * [Spring MockMVC Legacy](../openapi-request-validator-mockmvc-legacy/README.md)
    * [Spring MVC](../openapi-request-validator-springmvc/README.md)
    * [Pact](../openapi-request-validator-pact/README.md)
    * [REST Assured](../openapi-request-validator-restassured/README.md)
    * [WireMock](../openapi-request-validator-wiremock/README.md)
* JSON Schema validation support - including schema references
* [Fine-grained control over which validations are applied](../openapi-request-validator-core/README.md)
    * Using either a message level mechanism; or
    * Specifying whitelists of expected validation messages