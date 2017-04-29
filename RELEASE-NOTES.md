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