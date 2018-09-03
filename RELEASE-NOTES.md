#v1.5.0
* Bumped version of Pact from 3.2.13 to 3.5.20
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/100) 
  
  *Important:* This changes the required Scala version from 2.11 to 2.12. There are also breaking changes in the Pact API.
  Importantly, usages of the `ValidatedPactProviderRule` should now use `provider.getUrl()` instead of `provider.config().url()`.

#v1.4.7
* Fixed the 'additional properties' validation for inline and nested schema definitions
[[#131]](https://bitbucket.org/atlassian/swagger-request-validator/issues/131)
* Bumped the version of `swagger-parser` to pick up a fix for pattern validation
[[#44]](https://bitbucket.org/atlassian/swagger-request-validator/issues/44)
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/95)
* Added response validation to the Spring MVC adaptor 
[[#58]](https://bitbucket.org/atlassian/swagger-request-validator/issues/58)
* Added control over 'unknown request' validation in the Spring MVC adaptor
[[#55]](https://bitbucket.org/atlassian/swagger-request-validator/issues/55)

#v1.4.6
* Added support for HAL+JSON content type
[[#127]](https://bitbucket.org/atlassian/swagger-request-validator/issues/127)
* Added an overloaded constructor to allow the host/port of the `ValidatedPactProviderRule` to be specified
[[#72]](https://bitbucket.org/atlassian/swagger-request-validator/issues/72)
* Ensuring the stream is closed after reading the request/response body in the `SwaggerRequestValidationService`  
[[#132]](https://bitbucket.org/atlassian/swagger-request-validator/issues/132)

#v1.4.5
* Fixed validation of non-string enums in request/response bodies. 
[[#118]](https://bitbucket.org/atlassian/swagger-request-validator/issues/118)
* Removed usage of `servletRequest.getContentLengthLong()` to be compatible with Servlet 2.0
[[#114]](https://bitbucket.org/atlassian/swagger-request-validator/issues/114)
* Added support for dynamic path matching to the operation resolver.
[[#121]](https://bitbucket.org/atlassian/swagger-request-validator/issues/121)

#v1.4.4
* Added support for basic auth in the Pact Provider validator for retrieving Pacts from a 
broker that requires authentication
[[#122]](https://bitbucket.org/atlassian/swagger-request-validator/issues/122)
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/79)

#v1.4.3
* Bumped version of `json-schema-validator` to pick up a bugfix
[[#120]](https://bitbucket.org/atlassian/swagger-request-validator/issues/120)
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/77)

#v1.4.2
* Stopped trying to validate non-JSON request/response bodies with the JSON schema validator
[[#65]](https://bitbucket.org/atlassian/swagger-request-validator/issues/65)
[[#94]](https://bitbucket.org/atlassian/swagger-request-validator/issues/94)

#v1.4.1
* Added additional context to validation messages to describe where a validation error occurred
[[#33]](https://bitbucket.org/atlassian/swagger-request-validator/issues/33)
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/68)

#v1.4.0
* Bumped dependency versions
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/73)

#v1.3.10
* Added support for HTTP Basic auth validation, and AND/OR security operations
[[#99]](https://bitbucket.org/atlassian/swagger-request-validator/issues/99)

#v1.3.9
* Split comma-separated headers into individual values before validating
[[#97]](https://bitbucket.org/atlassian/swagger-request-validator/issues/97)

#v1.3.8
* Added support for `multipart/form-data` requests
[[#62]](https://bitbucket.org/atlassian/swagger-request-validator/issues/62)
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/70)

#v1.3.7
* Improved the way API operations are resolved when multiple paths potentially match an incoming request
[[#83]](https://bitbucket.org/atlassian/swagger-request-validator/issues/83)
* Exposed the RestAssured `SwaggerValidationException` so it can be handled in tests
[[#89]](https://bitbucket.org/atlassian/swagger-request-validator/issues/89)

#v1.3.6
Version burned

#v1.3.5
Version burned

#v1.3.4
Version burned

#v1.3.3
* Fixed a bug with handling of leading/trailing slashes in the api basePath
[[#87]](https://bitbucket.org/atlassian/swagger-request-validator/issues/87)
* Added support for `uri`, `ipv4`, `ipv6`, `email` and `uuid` format validation in query/path params 
[[#85]](https://bitbucket.org/atlassian/swagger-request-validator/issues/85)

#v1.3.2
* Added support for path params that aren't an entire path part, e.g. `/{param}.json`
[[#81]](https://bitbucket.org/atlassian/swagger-request-validator/issues/81)

#v1.3.1
* Improved performance of the SpringMVC filter
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/52)
* Fixed a bug where params with $ref would throw a NPE
[[#59]](https://bitbucket.org/atlassian/swagger-request-validator/issues/59)
* Fixed a bug where specs with a `definitions` block would fail validation
[[#74]](https://bitbucket.org/atlassian/swagger-request-validator/issues/74)
* Bumped version of `swagger-parser` to `1.0.32`

#v1.3.0
* Added support for fine-grained whitelisting of validation errors based on user-defined rules.
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/58)

#v1.2.4
* Fixed NPE when using an 'anything' request schema. Validation with the `{}` schema will now accept any JSON value.
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/57)

#v1.2.3
* Fixed an NPE with Spring MockMVC when no body
[[#71]](https://bitbucket.org/atlassian/swagger-request-validator/issues/71)

#v1.2.2
* Default collection format to CSV when not provided
[[#68]](https://bitbucket.org/atlassian/swagger-request-validator/issues/68)
* SpringMVC filter now ignores CORS pre-flight requests during validation
[[#67]](https://bitbucket.org/atlassian/swagger-request-validator/issues/67)

#v1.2.1
* Fixed a bug where RFC3339 timestamps in headers could cause validation to fail even if they are valid
[[#54]](https://bitbucket.org/atlassian/swagger-request-validator/issues/54)
[[#63]](https://bitbucket.org/atlassian/swagger-request-validator/issues/63)
* Added examples of using Spring MVC exception resolvers with the request validator
[[#56]](https://bitbucket.org/atlassian/swagger-request-validator/issues/54)

#v1.2.0
* Refactored request/response adapters to use Builders that create `SimpleRequest` and `SimpleResponse`
instances. The module-specific adapter constructors are now deprecated and will be removed in a future release.
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/43)
* Added support for Spring MVC. Requests can now be validated at runtime in production.
[[#51]](https://bitbucket.org/atlassian/swagger-request-validator/issues/51)
* Fixed a bug where non-form params were validated as form params
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/45)

#v1.1.1
* Fixed a bug where a `StackOverflowError` occurs when there are too many messages in a report
[[#52]](https://bitbucket.org/atlassian/swagger-request-validator/issues/52)

#v1.1.0
* Added support for Spring MockMvc
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/37/integrate-validator-with-spring-mockmvc/diff)
* Fixed a bug where subsequent validations of 'discriminator' changed behavior
[[#46]](https://bitbucket.org/atlassian/swagger-request-validator/issues/46)
* Bumped version of `json-schema-validator` and switched to new groupId

#v1.0.19
* Added support for RFC3339 timestamp validation
[[#48]](https://bitbucket.org/atlassian/swagger-request-validator/issues/48)
* Moved message bundle to avoid collision with parent project
[[#49]](https://bitbucket.org/atlassian/swagger-request-validator/issues/49)

#v1.0.18
* Fixed a bug with Pact validation not treating header names as case-insensitive
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/35)

#v1.0.17
* Added support for additional HTTP methods
[[#42]](https://bitbucket.org/atlassian/swagger-request-validator/issues/42)
* Fixed NPE when missing required header parameters
[[#43]](https://bitbucket.org/atlassian/swagger-request-validator/issues/43)

#v1.0.16
* Removed `MutableValidationReport` and improved validation report behavior
[[#35]](https://bitbucket.org/atlassian/swagger-request-validator/issues/35)
[[#14]](https://bitbucket.org/atlassian/swagger-request-validator/issues/14)

#v1.0.15
* Added support for providing auth data when retrieving remote Swagger API documents 
[[#41]](https://bitbucket.org/atlassian/swagger-request-validator/issues/41)

#v1.0.14
* Fixed a bug where API path prefix is not used in operation lookup 
[[#40]](https://bitbucket.org/atlassian/swagger-request-validator/issues/40)

#v1.0.13
* Fixed a bug in validation of nulls in arrays, or in objects within arrays
[[#37]](https://bitbucket.org/atlassian/swagger-request-validator/issues/37)
* Added support for no-value query params and headers in the SimpleRequest builder
[[#34]](https://bitbucket.org/atlassian/swagger-request-validator/issues/34)


#v1.0.12

* Added support for Swagger format validation within JSON schema (string/date, integer/int32 etc.)
[[#36]](https://bitbucket.org/atlassian/swagger-request-validator/issues/36)
* Added support for matching on method+path where there are overlapping path patterns across request operations
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/27/swagger-validation-failed-for-similar/diff)


# v1.0.11

* Added ability to validate requests/responses independently
[[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/23)

# v1.0.10

* Added support for request and response header validation 
[[#22]](https://bitbucket.org/atlassian/swagger-request-validator/issues/22)
* Added support for request Content-Type validation against spec Consumes types
[[#22]](https://bitbucket.org/atlassian/swagger-request-validator/issues/22)
* Added support for request Accept validation against spec Produces types 
[[#22]](https://bitbucket.org/atlassian/swagger-request-validator/issues/22)
* Added support for response Content-Type validation against spec Produces types 
[[#22]](https://bitbucket.org/atlassian/swagger-request-validator/issues/22)
* Added support for the Swagger/OpenAPI `discriminator` keyword
[[#26]](https://bitbucket.org/atlassian/swagger-request-validator/issues/26)


# v1.0.9

* Added basic support for validation of security tokens in headers and query params
[[#30]](https://bitbucket.org/atlassian/swagger-request-validator/issues/30)

# v1.0.8

* Added support for validation of `x-www-form-urlencoded` request bodies
[[#28]](https://bitbucket.org/atlassian/swagger-request-validator/issues/28)

# v1.0.7

* Added additional error messages from schema validation when composite schema validation fails (e.g. `allOf`) 
[[#25]](https://bitbucket.org/atlassian/swagger-request-validator/issues/25)

# v1.0.6

* Disabling `additionalProperties` validation when message level `validation.schema.additionalProperties=IGNORE` to 
support validation of `allOf` etc. 
[[#24]](https://bitbucket.org/atlassian/swagger-request-validator/issues/24)

# v1.0.5

* Additional parameter validation support (pattern, min/max length, date format etc.)
* Support for JSON string payloads as well as URLs
* Added Pact Provider validator that retrieves Consumer Pacts from a broker and validates them against a spec
[[#20]](https://bitbucket.org/atlassian/swagger-request-validator/issues/20)