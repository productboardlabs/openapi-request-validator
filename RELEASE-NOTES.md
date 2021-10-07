# v2.20.0

* Bumped a number of dependencies:
  * `spring`: 5.3.3 -> 5.3.9
  * `netty`: 4.1.66.Final -> 4.1.68.Final
  * `jetty`: 9.4.43.v20210629 -> 9.4.44.v20210927
  * `jackson`: 2.12.3 -> 2.12.5
  * `swagger-parser`: 2.0.27 -> 2.0.28
  * `mockito-core`: 3.11.2 -> 3.12.4
  * `logback-classic`: 1.2.5 -> 1.2.6
  * `checkstyle`: 8.45.1 -> 9.0.1
  * `jmh`: 1.32 -> 1.33

# v2.19.5

* Fixed the broken release pipeline. No lib changes
  (see release notes for `2.19.2`, `2.19.3` and `2.19.4`).

# v2.19.4

**Important** Due to a broken release pipeline this version is not available in Maven Central. Please use `2.19.5`.

* Added an example configuration for request-only validation. See the FAQ for details.
  [[#348]](https://bitbucket.org/atlassian/swagger-request-validator/issues/348)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/251)
* Fixed behavior of mediatype matching for the `ContentType` header when wildcard ranges exist
  [[#318]](https://bitbucket.org/atlassian/swagger-request-validator/issues/318)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/252)
* Fixed a problem with the JDK8 time module not being registered with Jackson during schema parsing
  [[#331]](https://bitbucket.org/atlassian/swagger-request-validator/issues/331)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/250)

# v2.19.3

**Important** Due to a broken release pipeline this version is not available in Maven Central. Please use `2.19.5`.

* Fixed behavior around `additionalProperties` validations for the case of non-Object schemas. This should address a
  number of problems around `oneOf`, `anyOf` and `allOf` validation for schemas that contain non-Object types.
  [[#336]](https://bitbucket.org/atlassian/swagger-request-validator/issues/336)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/238)

# v2.19.2

**Important** Due to a broken release pipeline this version is not available in Maven Central. Please use `2.19.5`.

* Made the library backwards compatible with older versions of Guava (this is a temporary state - do not rely on this
  being the case going forward)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/246)
* Bumped parent pom version to fix incorrect licensing
  [[#338]](https://bitbucket.org/atlassian/swagger-request-validator/issues/338)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/245)
* Bumped a number of minor/patch versions of dependencies:
  * `jackson`: 2.12.2 -> 2.12.3
  * `logback`: 1.2.3 -> 1.2.5
  * `slf4j`: 1.7.30 -> 1.7.32
  * `mockito`: 3.9.0 -> 3.11.2
  * `rest-assured`: 4.3.3 -> 4.4.0
  * `swagger-parser`: 2.0.25 -> 2.0.27
  * `jmh`: 1.29 -> 1.32
  * `jetty`: 9.4.39.v20210325 -> 9.4.43.v20210629
  * `netty`: 4.1.63.FINAL -> 4.1.66.FINAL

# v2.19.1

Version burned (broken release pipeline)

# v2.19.0

Version burned (broken release pipeline)

# v2.18.1

* Allow adding multiple headers using `OpenApiInteractionValidator.Builder#withAuthHeaderData`.
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/241)

# v2.18.0

* Exposed `ParseOptions` from the underlying [swagger-parser](https://github.com/swagger-api/swagger-parser)
  library so they can be set during validator creation. Specifically, the `resolveCombinators` option may be used in
  some cases to avoid the problem with `additionalProperties` validation with the `allOf` keyword
  (see the [FAQ](./docs/FAQ.md) for details)
  [[#320]](https://bitbucket.org/atlassian/swagger-request-validator/issues/320)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/228)
* Fixed a regression where nested `Message`s from sub-validations were not being included in the validation report
  [[#316]](https://bitbucket.org/atlassian/swagger-request-validator/issues/316)
  [[#323]](https://bitbucket.org/atlassian/swagger-request-validator/issues/323)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/229)
* Added optional `Pointer` information to the message context to give access to the instance and schema pointers in the
  case where a validation error has come from schema validation.
  [[#300]](https://bitbucket.org/atlassian/swagger-request-validator/issues/300)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/229)

# v2.17.0

* Bumped a number of minor/patch versions of dependencies:
  * `jackson`: 2.12.1 -> 2.12.2
  * `swagger-parser`: 2.0.24 -> 2.0.25
  * `jetty`: 9.4.36.v20210114 -> 9.4.39.v20210325
  * `netty`: 4.1.59.FINAL -> 4.1.63.FINAL

# v2.16.0

* Bumped the version of `spring` 5.1.14 -> 5.3.3
* Bumped the version of `spring-boot` used in tests 1.5.22 -> 2.4.2

# v2.15.1

* Fixed the version specifier of `json-schema-validator` from `v2.2.14` to `2.2.14`
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/214)

# v2.15.0

* Bumped `rest-assured` 3.3.0 -> 4.3.3

*Note*: This is a major version bump of `rest-assured`.

# v2.14.0

**Important**: This version includes an incorrect version specifier for `json-schema-validator` that may prevent it from
building. This has been fixed in version `2.15.1` onwards. If you are unable to roll forward to this version you may
need to override the dependency yourself:

```xml

<dependency>
  <groupId>com.github.java-json-tools</groupId>
  <artifactId>json-schema-validator</artifactId>
  <version>2.2.14</version>
</dependency>
```

* Bumped some dependencies from renovate:
  * `spring-core`: 4.3.26-RELEASE -> 4.3.30.RELEASE
  * `json-schema-validator`: 2.2.11 -> 2.2.14
  * `guava`: 28.1-jre -> 28.2-jre
  * `checkstyle`: 8.25 -> 8.40
  * `jmh`: 1.26 -> 1.27
  * `hamcrest-optional`: 1.1.5 -> 1.2.0
  * `junit`: 4.12 -> 4.13.1
  * `mockito`: 3.3.3 -> 3.7.7
  * `netty`: 4.1.58.Final -> 4.1.59.Final

# v2.13.0

* Bumped some dependencies to address vulnerabilities in transitive dependencies
  * `pact`: 3.6.14 -> 3.6.15
  * `netty`: 4.1.30.Final -> 4.1.58.Final
  * `jetty`: 9.2.28.v20190418 -> 9.4.36.v20210114
  * `wiremock`: 2.25.1 -> 2.27.2
  * `jackson`: 2.10.3 -> 2.12.1
  * `commons-codec`: 1.10 -> 1.15
  * `httpclient`: 4.5.6 -> 4.5.13

# v2.12.1

* Fixed a problem with `ConcurrentModificationException` during schema validation
  [[#253]](https://bitbucket.org/atlassian/swagger-request-validator/issues/253)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/188)

# v2.12.0

* A number of improvements to the validation behavior around use of `discriminator` with schema composition (`allOf`
  , `oneOf`, `anyOf`).
  [[#286]](https://bitbucket.org/atlassian/swagger-request-validator/issues/286)
  [[#241]](https://bitbucket.org/atlassian/swagger-request-validator/issues/241)
  [[#269]](https://bitbucket.org/atlassian/swagger-request-validator/issues/269)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/180)
  * Better adherence to the OpenAPI v3 spec
  * Fixed a number of scenarios where a validation loop is reported when validating the `discriminator`
  * Better handling of `discriminator` for `anyOf` and `oneOf`
  * Fixes a problem with dirty state left over between validations

*Note*: This change makes the validator more strict in it's handling of `discriminator`. Some schemas that were
previously passing may now be reported as invalid.

# v2.11.5

* Performance improvement: Memoized the processed schema to avoid de-serializing the schema multiple times for the same
  spec
  [[#292]](https://bitbucket.org/atlassian/swagger-request-validator/issues/292)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/185)

# v2.11.4

* Bumped `swagger-parser` to `v2.0.24` to pick up a change to included versions of `jackson-coreutils`
  (see https://github.com/swagger-api/swagger-parser/pull/1480)
  [[#305]](https://bitbucket.org/atlassian/swagger-request-validator/issues/305)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/187)

# v2.11.3

* Added support for async requests in SpringMVC
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/184)
* Fixed a problem with the request body being corrupted if it is not validated (e.g. its content-type is not supported
  by the validator)
  [[#312]](https://bitbucket.org/atlassian/swagger-request-validator/issues/312)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/186)

# v2.11.2

* Added ability to set the request/response bodies as `byte[]` or `InputStream` to avoid unnecessary deserialization
  [[#273]](https://bitbucket.org/atlassian/swagger-request-validator/issues/273)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/181)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/183)
* Added some benchmarks to allow future performance improvements to be better tested
  [[#294]](https://bitbucket.org/atlassian/swagger-request-validator/issues/294)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/182)

# v2.11.1

* Improved performance of form-data validation by not deserializing the data multiple times
  [[#274]](https://bitbucket.org/atlassian/swagger-request-validator/issues/274)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/178)
* Fixed a problem with form params not available for validation in the SpringMVC module
  [[#303]](https://bitbucket.org/atlassian/swagger-request-validator/issues/303)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/179)

# v2.11.0

* Fixed handling of headers so that we no longer split on `,` unless the header is marked as an array param with an
  appropriate style.
  [[#256]](https://bitbucket.org/atlassian/swagger-request-validator/issues/256)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/177)
* Improved handling of array params, with better support for `matrix` and `label` style params
  [[#290]](https://bitbucket.org/atlassian/swagger-request-validator/issues/290)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/177)

*Note:* This version is stricter in the handling of array parameters, particularly headers, and now follows the
default `style` for each parameter type as described in the OAI v3 spec. One result of this is that e.g. header array
params that previously would have accepted multiple header values will now likely fail validation unless the `style` has
been set correctly (the default `style` for header params is `simple` which expects comma-separated values in a single
header).

# v2.10.3

* Fix a bug in matching of responses with wildcards in the defined media type
  [[#177]](https://bitbucket.org/atlassian/swagger-request-validator/issues/177)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/175)
* Added support for the `allowEmptyValues` param property and cleaned up handling of empty arrays
  [[#61]](https://bitbucket.org/atlassian/swagger-request-validator/issues/61)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/176)

# v2.10.2

* Bump `swagger-parser` to pickup change around treatment of multiple path params in a single path segment. Added tests
  to exercise this scenario.
  [[#275]](https://bitbucket.org/atlassian/swagger-request-validator/issues/275)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/172)
* Add support for basic validation for [cookie authentication](https://swagger.io/docs/specification/authentication/cookie-authentication/)
  [[#278]](https://bitbucket.org/atlassian/swagger-request-validator/issues/278)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/173)

# v2.10.1

* Fix operation resolution to favor exact matches
  [[#252]](https://bitbucket.org/atlassian/swagger-request-validator/issues/252)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/169)
* Improve naming of whitelist rules to make it clearer that a regex match is used; Add support for plain substring
  matching.
  [[#267]](https://bitbucket.org/atlassian/swagger-request-validator/issues/267)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/168)
* Add additional information to the Base64 validation messages
  [[#285]](https://bitbucket.org/atlassian/swagger-request-validator/issues/285)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/170)
* Add support for requests with `byte[]` bodies in the RestAssured module
  [[#270]](https://bitbucket.org/atlassian/swagger-request-validator/issues/270)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/171)
* Fix handling of empty string defaults in sever variables
  [[#276]](https://bitbucket.org/atlassian/swagger-request-validator/issues/276)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/167)

# v2.10.0

* Enforce validation of number formats.
  [[#264]](https://bitbucket.org/atlassian/swagger-request-validator/issues/264)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/164)

*Note*: This change adds new error keys of the form `validation.prefix.schema.format.int32` etc. It also adds a format
type suffix to existing format keys, allowing finer-grained control of validation behavior.

# v2.9.1

* Fix behavior of `readOnly`/`writeOnly` with `required` for nested/composed schemas
  [[#265]](https://bitbucket.org/atlassian/swagger-request-validator/issues/265)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/160)
* Add a new factory method to construct validator instances from pre-parsed `OpenAPI` instances
  [[#226]](https://bitbucket.org/atlassian/swagger-request-validator/issues/226)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/162)
* Fix the example tests so that they run as expected
  [[#261]](https://bitbucket.org/atlassian/swagger-request-validator/issues/261)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/161)
* Bump some dependency versions
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/163)
  * `swagger-parser` -> 2.0.19
  * `jackson` -> 2.10.3
  * `slf4j` -> 1.7.30
  * `mockito` -> 3.3.3
  * `wiremock` -> 2.25.1
  * `spring` -> 4.3.25-RELEASE and 5.1.14-RELEASE
  * `hamcrest` -> 2.2

*Note*: The `swagger-parser` is now strictly enforcing API spec adherence to schema. Most notably it now enforces that
all path params must have `required: true`. Path params that are missing the `required: true` will fail to load.

# v2.9.0

* Bump `swagger-parser` 2.0.14 -> 2.0.17 (including bump of `jackson` 2.9.10 -> 2.10.2 )
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/159)

# v2.8.4

* Improve performance of Base64 validation
  [[#259]](https://bitbucket.org/atlassian/swagger-request-validator/issues/259)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/158)
* Include `deepObject` params in the unexpected param validation
  [[#258]](https://bitbucket.org/atlassian/swagger-request-validator/issues/258)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/157)

# v2.8.3

* Fix a NPE when using a `discriminator` with the `oneOf` keyword
  [[#166]](https://bitbucket.org/atlassian/swagger-request-validator/issues/166)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/156)

# v2.8.2

Version burned

# v2.8.1

* Add ability to provide a pre-parsed `OpenApi` object to the validator
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/152)
* Improve Base64 format validation by detecting strings that are missing padding
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/151)
* Remove usage of `@Beta` Guava methods
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/155)
* Fix a race condition in the `LevelResolver`
  [[#251]](https://bitbucket.org/atlassian/swagger-request-validator/issues/251)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/153)

# v2.8.0

* Basic support for `deepObject` style query params
  [[#234]](https://bitbucket.org/atlassian/swagger-request-validator/issues/234)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/141)

# v2.7.2

* Updated SpringMVC module to use new builders
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/150)
* Allow discriminator property to be a $ref
  [[#247]](https://bitbucket.org/atlassian/swagger-request-validator/issues/247)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/147)
* Fix validation of readOnly/writeOnly properties within items in an array
  [[#240]](https://bitbucket.org/atlassian/swagger-request-validator/issues/240)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/146)

# v2.7.1

* Removed duplicate validation of Base64 encoded String/Byte fields
  [[#214]](https://bitbucket.org/atlassian/swagger-request-validator/issues/214)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/149)

# v2.7.0

* Bumped versions of a number of dependencies

### Upgrade notes

#### Swagger / OpenAPI parsing

The Swagger / OpenAPI parser is now stricter in applying schema validation to the parsed spec. If your spec does not
adhere to the schema it will fail to load.

#### Pact

If you use the Pact adapter, the change from version `v3.5.20` to `v3.6.14` of the Pact JVM library has brought some
changes to the `ConsumerInfo` constructor. If you create these directly (rather than using the builders on
the `PactProviderValidator`) please review your usage.

The Pact JUnit fragment builder now also requires returning a `RequestResponsePact` rather than a `PactFragment`.
See `OpenApiValidatorPactConsumerTestExample` for examples.

# v2.6.0

* Added factory methods to create `OpenApiInteractionValidator` instances with a spec or URL to avoid having to guess at
  what has been provided.
  [[#236]](https://bitbucket.org/atlassian/swagger-request-validator/issues/236)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/142)

# v2.5.0

* Added support for Cookie parameter validation
  [[#224]](https://bitbucket.org/atlassian/swagger-request-validator/issues/224)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/138)
* Improved validation messages by including messages from deeply nested objects during schema validation
  [[#221]](https://bitbucket.org/atlassian/swagger-request-validator/issues/221)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/140)

# v2.4.7

* Added a constructor to `com.atlassian.oai.validator.springmvc.OpenApiValidationInterceptor` to allow a custom report
  handler to be provided
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/139)

# v2.4.6

* Improved performance for validation of long Base64 encoded strings
  [[#225]](https://bitbucket.org/atlassian/swagger-request-validator/issues/225)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/137)
* Fixed treatment of the `required` flag when used with `readOnly` and `writeOnly` in request/response bodies
  [[#207]](https://bitbucket.org/atlassian/swagger-request-validator/issues/207)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/133)

# v2.4.5

* Fixed a bug where backslashes in query params etc. are treated incorrectly during validation
  [[#220]](https://bitbucket.org/atlassian/swagger-request-validator/issues/220)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/134)

# v2.4.4

* Fixed a problem where the request path can be null under some circumstances in Spring MVC
  [[#218]](https://bitbucket.org/atlassian/swagger-request-validator/issues/218)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/136)

# v2.4.3

* Fix NPE in the `IsEntityWhitelistRule` when there is no content in the response
  [[#222]](https://bitbucket.org/atlassian/swagger-request-validator/issues/222)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/135)

# v2.4.2

* Bump version of `json-schema-validator` to pickup improvements
  [[#216]](https://bitbucket.org/atlassian/swagger-request-validator/issues/216)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/132)
* Fix decoding of query param names in the `swagger-request-validator-springmvc` module
  [[#215]](https://bitbucket.org/atlassian/swagger-request-validator/issues/215)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/130)

# v2.4.1

* Improve memory usage in the Spring MVC adapter
  [[#213]](https://bitbucket.org/atlassian/swagger-request-validator/issues/213)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/129)

# v2.4.0

* Added support for failing validation on unexpected query parameters
  [[#109]](https://bitbucket.org/atlassian/swagger-request-validator/issues/109)
  [[Docs]](https://bitbucket.org/atlassian/swagger-request-validator/src/master/docs/FAQ.md)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/125)

# v2.3.0

* Added support for custom validation logic that can be used to e.g. provide validation for vendor extensions
  [[Docs]](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-core/README.md)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/122)

# v2.2.3

* Fixed content type matching to return un-modified content types
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/128)

# v2.2.2

* Added support for basic HTTP Bearer auth validation
  [[#195]](https://bitbucket.org/atlassian/swagger-request-validator/issues/195)

# v2.2.1

* Fixed NPE when `security` defined but no `securitySchemes` defined
  [[#188]](https://bitbucket.org/atlassian/swagger-request-validator/issues/188)
* Added better support for [using multiple authentication types](https://swagger.io/docs/specification/authentication/#multiple)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/123)

# v2.2.0

* To address a breaking change in `spring-test-5.1.0.RELEASE`, introduced
  `swagger-request-validator-mockmvc-legacy` for pre-5.1 versions.
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/121)
  [[#181]](https://bitbucket.org/atlassian/swagger-request-validator/issues/181)
  [[#190]](https://bitbucket.org/atlassian/swagger-request-validator/issues/190)

**Important**

If you use versions of Spring prior to 5.1.0.RELEASE (or Spring Boot prior to 2.1.0) you will need to change your
dependency from `swagger-request-validator-mockmvc` to `swagger-request-validator-mockmvc-legacy`.

# v2.1.0

* Added a new module for performing Swagger / Open API validation with Spring Web Client
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/120)
* Changed JSON mediatype detection to support `+json` suffixes
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/118)

# v2.0.4

* Fixed a bug when the `produces` clause is empty in a swagger v2 spec
  [[#179]](https://bitbucket.org/atlassian/swagger-request-validator/issues/179)

# v2.0.3

* Added ability to change behavior in the `OpenApiValidationInterceptor` to suit consumer use cases
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/117)

# v2.0.2

* Fixed a bug in validation when consumes clause is empty in a swagger v2 spec
  [[#167]](https://bitbucket.org/atlassian/swagger-request-validator/issues/167)
* Fixed cause exceptions in the spring-mockmvc module when the request body is empty
  [[#163]](https://bitbucket.org/atlassian/swagger-request-validator/issues/163)

# v2.0.1

* Decode query params before validation in Spring MVC
  [[#155]](https://bitbucket.org/atlassian/swagger-request-validator/issues/155)
* Bumped version of `swagger-parser` to 2.0.5
* Fixed behavior of validation with referenced request bodies
  [[#165]](https://bitbucket.org/atlassian/swagger-request-validator/issues/165)

# v1.5.1

* Decode query params before validation in Spring MVC
  [[#155]](https://bitbucket.org/atlassian/swagger-request-validator/issues/155)

# v2.0.0

Major release milestone.

Provides support for both Swagger v2 and OpenAPI v3 specifications.

### Major changes from v1.x

* Support for OpenAPI v3
* Additional context attached to validation messages
* Standardization of validation message keys
* Substantial refactoring within the library
* Bumping dependency versions

### Upgrade notes

* `SwaggerRequestResponseValidator` has been deprecated in favor of `OpenApiInteractionValidator`. The original `SwaggerRequestResponseValidator` will be removed in a future release.
* Various filters and interceptors etc. in the adapter modules have been deprecated and replaced with versions named
  with `OpenApi*`. The original `Swagger*` named versions will be removed in a future release.
* Schema validation errors now have the form `validation.{request|response}.{body|parameter}.schema.{keyword}`
  e.g. `validation.request.parameter.schema.type`
* Multi-part formdata validation is currently not supported. This will be re-added in an upcoming release.

See [OpenAPI v3 feature coverage](./docs/OPENAPIv3.md) for details on supported OpenAPI v3 features.

# v1.5.0

* Bumped version of Pact from 3.2.13 to 3.5.20
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/100)

  *Important:* This changes the required Scala version from 2.11 to 2.12. There are also breaking changes in the Pact
  API. Importantly, usages of the `ValidatedPactProviderRule` should now use `provider.getUrl()` instead of `provider.config().url()`.

# v1.4.7

* Fixed the 'additional properties' validation for inline and nested schema definitions
  [[#131]](https://bitbucket.org/atlassian/swagger-request-validator/issues/131)
* Bumped the version of `swagger-parser` to pick up a fix for pattern validation
  [[#44]](https://bitbucket.org/atlassian/swagger-request-validator/issues/44)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/95)
* Added response validation to the Spring MVC adaptor
  [[#58]](https://bitbucket.org/atlassian/swagger-request-validator/issues/58)
* Added control over 'unknown request' validation in the Spring MVC adaptor
  [[#55]](https://bitbucket.org/atlassian/swagger-request-validator/issues/55)

# v1.4.6

* Added support for HAL+JSON content type
  [[#127]](https://bitbucket.org/atlassian/swagger-request-validator/issues/127)
* Added an overloaded constructor to allow the host/port of the `ValidatedPactProviderRule` to be specified
  [[#72]](https://bitbucket.org/atlassian/swagger-request-validator/issues/72)
* Ensuring the stream is closed after reading the request/response body in the `SwaggerRequestValidationService`  
  [[#132]](https://bitbucket.org/atlassian/swagger-request-validator/issues/132)

# v1.4.5

* Fixed validation of non-string enums in request/response bodies.
  [[#118]](https://bitbucket.org/atlassian/swagger-request-validator/issues/118)
* Removed usage of `servletRequest.getContentLengthLong()` to be compatible with Servlet 2.0
  [[#114]](https://bitbucket.org/atlassian/swagger-request-validator/issues/114)
* Added support for dynamic path matching to the operation resolver.
  [[#121]](https://bitbucket.org/atlassian/swagger-request-validator/issues/121)

# v1.4.4

* Added support for basic auth in the Pact Provider validator for retrieving Pacts from a broker that requires
  authentication
  [[#122]](https://bitbucket.org/atlassian/swagger-request-validator/issues/122)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/79)

# v1.4.3

* Bumped version of `json-schema-validator` to pick up a bugfix
  [[#120]](https://bitbucket.org/atlassian/swagger-request-validator/issues/120)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/77)

# v1.4.2

* Stopped trying to validate non-JSON request/response bodies with the JSON schema validator
  [[#65]](https://bitbucket.org/atlassian/swagger-request-validator/issues/65)
  [[#94]](https://bitbucket.org/atlassian/swagger-request-validator/issues/94)

# v1.4.1

* Added additional context to validation messages to describe where a validation error occurred
  [[#33]](https://bitbucket.org/atlassian/swagger-request-validator/issues/33)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/68)

# v1.4.0

* Bumped dependency versions
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/73)

# v1.3.10

* Added support for HTTP Basic auth validation, and AND/OR security operations
  [[#99]](https://bitbucket.org/atlassian/swagger-request-validator/issues/99)

# v1.3.9

* Split comma-separated headers into individual values before validating
  [[#97]](https://bitbucket.org/atlassian/swagger-request-validator/issues/97)

# v1.3.8

* Added support for `multipart/form-data` requests
  [[#62]](https://bitbucket.org/atlassian/swagger-request-validator/issues/62)
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/70)

# v1.3.7

* Improved the way API operations are resolved when multiple paths potentially match an incoming request
  [[#83]](https://bitbucket.org/atlassian/swagger-request-validator/issues/83)
* Exposed the RestAssured `SwaggerValidationException` so it can be handled in tests
  [[#89]](https://bitbucket.org/atlassian/swagger-request-validator/issues/89)

# v1.3.6

Version burned

# v1.3.5

Version burned

# v1.3.4

Version burned

# v1.3.3

* Fixed a bug with handling of leading/trailing slashes in the api basePath
  [[#87]](https://bitbucket.org/atlassian/swagger-request-validator/issues/87)
* Added support for `uri`, `ipv4`, `ipv6`, `email` and `uuid` format validation in query/path params
  [[#85]](https://bitbucket.org/atlassian/swagger-request-validator/issues/85)

# v1.3.2

* Added support for path params that aren't an entire path part, e.g. `/{param}.json`
  [[#81]](https://bitbucket.org/atlassian/swagger-request-validator/issues/81)

# v1.3.1

* Improved performance of the SpringMVC filter
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/52)
* Fixed a bug where params with $ref would throw a NPE
  [[#59]](https://bitbucket.org/atlassian/swagger-request-validator/issues/59)
* Fixed a bug where specs with a `definitions` block would fail validation
  [[#74]](https://bitbucket.org/atlassian/swagger-request-validator/issues/74)
* Bumped version of `swagger-parser` to `1.0.32`

# v1.3.0

* Added support for fine-grained whitelisting of validation errors based on user-defined rules.
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/58)

# v1.2.4

* Fixed NPE when using an 'anything' request schema. Validation with the `{}` schema will now accept any JSON value.
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/57)

# v1.2.3

* Fixed an NPE with Spring MockMVC when no body
  [[#71]](https://bitbucket.org/atlassian/swagger-request-validator/issues/71)

# v1.2.2

* Default collection format to CSV when not provided
  [[#68]](https://bitbucket.org/atlassian/swagger-request-validator/issues/68)
* SpringMVC filter now ignores CORS pre-flight requests during validation
  [[#67]](https://bitbucket.org/atlassian/swagger-request-validator/issues/67)

# v1.2.1

* Fixed a bug where RFC3339 timestamps in headers could cause validation to fail even if they are valid
  [[#54]](https://bitbucket.org/atlassian/swagger-request-validator/issues/54)
  [[#63]](https://bitbucket.org/atlassian/swagger-request-validator/issues/63)
* Added examples of using Spring MVC exception resolvers with the request validator
  [[#56]](https://bitbucket.org/atlassian/swagger-request-validator/issues/54)

# v1.2.0

* Refactored request/response adapters to use Builders that create `SimpleRequest` and `SimpleResponse`
  instances. The module-specific adapter constructors are now deprecated and will be removed in a future release.
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/43)
* Added support for Spring MVC. Requests can now be validated at runtime in production.
  [[#51]](https://bitbucket.org/atlassian/swagger-request-validator/issues/51)
* Fixed a bug where non-form params were validated as form params
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/45)

# v1.1.1

* Fixed a bug where a `StackOverflowError` occurs when there are too many messages in a report
  [[#52]](https://bitbucket.org/atlassian/swagger-request-validator/issues/52)

# v1.1.0

* Added support for Spring MockMvc
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/37/integrate-validator-with-spring-mockmvc/diff)
* Fixed a bug where subsequent validations of 'discriminator' changed behavior
  [[#46]](https://bitbucket.org/atlassian/swagger-request-validator/issues/46)
* Bumped version of `json-schema-validator` and switched to new groupId

# v1.0.19

* Added support for RFC3339 timestamp validation
  [[#48]](https://bitbucket.org/atlassian/swagger-request-validator/issues/48)
* Moved message bundle to avoid collision with parent project
  [[#49]](https://bitbucket.org/atlassian/swagger-request-validator/issues/49)

# v1.0.18

* Fixed a bug with Pact validation not treating header names as case-insensitive
  [[Details]](https://bitbucket.org/atlassian/swagger-request-validator/pull-requests/35)

# v1.0.17

* Added support for additional HTTP methods
  [[#42]](https://bitbucket.org/atlassian/swagger-request-validator/issues/42)
* Fixed NPE when missing required header parameters
  [[#43]](https://bitbucket.org/atlassian/swagger-request-validator/issues/43)

# v1.0.16

* Removed `MutableValidationReport` and improved validation report behavior
  [[#35]](https://bitbucket.org/atlassian/swagger-request-validator/issues/35)
  [[#14]](https://bitbucket.org/atlassian/swagger-request-validator/issues/14)

# v1.0.15

* Added support for providing auth data when retrieving remote Swagger API documents
  [[#41]](https://bitbucket.org/atlassian/swagger-request-validator/issues/41)

# v1.0.14

* Fixed a bug where API path prefix is not used in operation lookup
  [[#40]](https://bitbucket.org/atlassian/swagger-request-validator/issues/40)

# v1.0.13

* Fixed a bug in validation of nulls in arrays, or in objects within arrays
  [[#37]](https://bitbucket.org/atlassian/swagger-request-validator/issues/37)
* Added support for no-value query params and headers in the SimpleRequest builder
  [[#34]](https://bitbucket.org/atlassian/swagger-request-validator/issues/34)

# v1.0.12

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