# Swagger Request Validator Features #

* [Swagger v2 support](SWAGGERv2.md)
* [OpenAPI v3 support](OPENAPIv3.md)
* Standalone - no dependencies on HTTP libraries or frameworks
* Adapters for commonly used HTTP libraries and testing frameworks
    * [Spring MockMVC](../swagger-request-validator-mockmvc/README.md)
    * [Spring MockMVC Legacy](../swagger-request-validator-mockmvc-legacy/README.md)
    * [Spring MVC](../swagger-request-validator-springmvc/README.md)
    * [Pact](../swagger-request-validator-pact/README.md)
    * [REST Assured](../swagger-request-validator-restassured/README.md)
    * [WireMock](../swagger-request-validator-wiremock/README.md)
* JSON Schema validation support - including schema references
* [Fine-grained control over which validations are applied](../swagger-request-validator-core/README.md)
    * Using either a message level mechanism; or
    * Specifying whitelists of expected validation messages