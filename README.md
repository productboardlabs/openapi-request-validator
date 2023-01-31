# Swagger Request Validator #

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator)

A Java library for validating HTTP request/responses against an [OpenAPI / Swagger](https://openapis.org/) specification.

Designed to be used independently of any HTTP library or framework, the library can be used to validate
request/responses from almost any source (e.g. in a REST client, in unit tests that use mocked responses,
in Pact tests etc.)

## Key features ##

* Standalone - no dependencies on HTTP libraries or frameworks
* Adapters for commonly used HTTP libraries and testing frameworks
* JSON Schema validation support - including schema references
* Fine-grained control over which validations are applied
* Support for [Swagger v2](./docs/SWAGGERv2.md) and [OpenAPI v3](./docs/OPENAPIv3.md) specifications

See [Features](./docs/FEATURES.md) for more details.

## Usage ##

See the [examples module](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/?at=master)
for examples on how the library is used.

Usage details for specific modules can be found in the READMEs for those modules.

```
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-core</artifactId>
    <version>${swagger-request-validator.version}</version>
</dependency>
```

## Project structure ##

See individual module READMEs for more information, including how to use each module.

### swagger-request-validator-core 

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-core/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-core)

The core validator logic.

Provides a standalone validator and uses an implementation-agnostic abstraction of
HTTP request/responses that can be adapted to any 3rd party implementation.

### swagger-request-validator-pact 

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-pact/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-pact)

Adapters for validating [Pact](http://docs.pact.io/) request/response expectations with the OpenAPI / Swagger validator, 
shortening the feedback loop when writing Consumer tests.

Includes a JUnit rule that adds OpenAPI / Swagger validation to the [Pact-JVM](https://github.com/DiUS/pact-jvm) consumer 
test execution.

### swagger-request-validator-wiremock 

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-wiremock/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-wiremock)

Adapters for validating [WireMock](http://wiremock.org/) HTTP mocks against an OpenAPI / Swagger specification.

Includes a drop-in replacement for the `WireMockRule` that adds validation to mocked interactions, giving you 
confidence that your mocks reflect reality. 

### swagger-request-validator-restassured 

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-restassured/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-restassured)

Adapters for validating given-when-then interactions from the [REST Assured](http://rest-assured.io/) testing library 
against an OpenAPI / Swagger specification.

Useful for e.g. ensuring your service implementation matches its API specification.

### swagger-request-validator-mockmvc 

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-mockmvc/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-mockmvc)

Adapters for validating interactions using the [Spring 5+ MVC Test Framework](http://docs.spring.io/spring-framework/docs/current/spring-framework-reference/html/integration-testing.html#spring-mvc-test-framework)
against an OpenAPI / Swagger specification.

Includes a [ResultMatcher](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/test/web/servlet/ResultMatcher.html) that 
allows you to assert your service implementation matches its API specification.

Compatible with Spring 5+.

### swagger-request-validator-mockmvc-legacy

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-mockmvc-legacy/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-mockmvc-legacy)

Adapters for validating interactions using the (legacy) [Spring 4.x MVC Test Framework](https://docs.spring.io/spring-framework/docs/4.3.x/spring-framework-reference/html/integration-testing.html#spring-mvc-test-framework)
against an OpenAPI / Swagger specification.

Includes a [ResultMatcher](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/test/web/servlet/ResultMatcher.html) that
allows you to assert your service implementation matches its API specification.

Compatible with Spring 4.x.

### swagger-request-validator-spring-webmvc 

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-spring-webmvc/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-spring-mockmvc)

Adapter for validating interactions using the [Spring 6+ Web MVC framework](https://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html)
against an OpenAPI / Swagger specification during runtime in a production environment.

Useful for ensuring that the incoming requests match your service's API specification.

Compatible with Spring 6+, Spring Boot 3+ and the Jakarta namespace. Requires JDK17+.

### swagger-request-validator-springmvc

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-springmvc/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-springmvc)

Adapter for validating interactions using the (legacy) [Spring 5.x Web MVC framework](https://docs.spring.io/spring-framework/docs/5.3.x/reference/html/web.html#mvc)
against an OpenAPI / Swagger specification during runtime in a production environment.

Useful for ensuring that the incoming requests match your service's API specification.

Compatible with Spring 5.x and Spring Boot 2.x.

### swagger-request-validator-spring-web-client 

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-spring-web-client/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-spring-web-client)

Adapter for adding OpenAPI / Swagger validation to the [Spring Web HTTP client](https://docs.spring.io/spring/docs/4.3.16.RELEASE/javadoc-api/org/springframework/http/client/package-summary.html).

Useful for ensuring that a service implementation matches its API specification.

### swagger-request-validator-examples

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-examples/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-examples)

Working code samples that demonstrate the features of the `swagger-request-validator` and its various adapters.

## Building and testing ##

The project uses Maven 3.8+. We recommend using [mvnvm](http://mvnvm.org/) or similar.

The project requires JDK17+ to build, but currently builds all but the `swagger-request-validator-spring-webmvc` module
to be JDK8 compatible. We recommend using [sdkman](https://sdkman.io/) or similar to manage JDK versions.

To build the project:

```
>> mvn clean install
```

To run the project tests:

```
>> mvn test
```

To do a full build:

```
>> mvn clean verify javadoc:jar
```

## License ##

Copyright (c) 2016 Atlassian and others. Apache 2.0 licensed, see LICENSE.txt file.