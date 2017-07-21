# Swagger Request Validator #

[![build-status](https://bitbucket-badges.atlassian.io/badge/atlassian/swagger-request-validator.svg)](https://bitbucket.org/atlassian/swagger-request-validator/addon/pipelines/home#!/) [![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator)

A Java library for validating HTTP request/responses against a [Swagger/OpenAPI](https://openapis.org/) specification.

Designed to be used independently of any HTTP library or framework, the library can be used to validate
request/responses from almost any source (e.g. in a REST client, in unit tests that use mocked responses,
in Pact tests etc.)

## Key features ##

* Standalone - no dependencies on HTTP libraries or frameworks
* Adapters for commonly used HTTP libraries and testing frameworks
* JSON Schema validation support - including schema references
* Fine-grained control over which validations are applied

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

See individual module READMEs for more information, including how to use use each module.

###swagger-request-validator-core 

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-core/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-core)

The core validator logic.

Provides a standalone validator and uses an implementation-agnostic abstraction of
HTTP request/responses that can be adapted to any 3rd party implementation.

###swagger-request-validator-pact 

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-pact/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-pact)

Adapters for validating [Pact](http://docs.pact.io/) request/response expectations with the Swagger validator, 
shortening the feedback loop when writing Consumer tests.

Includes a JUnit rule that adds Swagger/OAI validation to the [Pact-JVM](https://github.com/DiUS/pact-jvm) consumer 
test execution.

###swagger-request-validator-wiremock 

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-wiremock/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-wiremock)

Adapters for validating [WireMock](http://wiremock.org/) HTTP mocks against a Swagger/OAI specification.

Includes a drop-in replacement for the `WireMockRule` that adds validation to mocked interactions, giving you 
confidence that your mocks reflect reality. 

###swagger-request-validator-restassured 

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-restassured/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-restassured)

Adapters for validating given-when-then interactions from the [REST Assured](http://rest-assured.io/) testing library 
against a Swagger/OAI specification.

Useful for e.g. ensuring your service implementation matches its API specification.

###swagger-request-validator-mockmvc 

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-mockmvc/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-mockmvc)

Adapters for validating interactions using the [Spring MVC Test Framework](http://docs.spring.io/spring-framework/docs/current/spring-framework-reference/html/integration-testing.html#spring-mvc-test-framework)
against a Swagger/OAI specification.

Includes a [ResultMatcher](http://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/test/web/servlet/ResultMatcher.html) that 
allows you to assert your service implementation matches its API specification.

###swagger-request-validator-springmvc 

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-mockmvc/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-mockmvc)

Adapter for validating interactions using the [Spring Web MVC framework](https://docs.spring.io/spring/docs/current/spring-framework-reference/html/mvc.html)
against a Swagger/OAI specification during runtime in a production environment.

Useful for ensuring that the client requests matching the API specification.

###swagger-request-validator-examples

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-examples/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-examples)

Working code samples that demonstrate the features of the `swagger-request-validator` and its various adapters.

## Building and testing ##

The project uses Maven 3.3+. We recommend using [mvnvm](http://mvnvm.org/) or similar.

To build the project:

```
>> mvn clean install
```

To run the project tests:

```
>> mvn test
```

## Contributors ##

Pull requests, issues and comments welcome. For pull requests:

* Add tests for new features and bug fixes
* Follow the existing style (checkstyle checking is enabled by default in builds)
* Separate unrelated changes into multiple pull requests

Please ensure that your branch builds successfully before you open your PR. The Pipelines build won't run by default 
on a remote branch, so either enable Pipelines for your fork or run the build locally: 

```
mvn clean verify javadoc:javadoc
```

See the existing [issues](https://bitbucket.org/atlassian/swagger-request-validator/issues) for things to start
contributing. If you want to start working on an issue, please assign the ticket to yourself and mark it as `open`
so others know it is in progress.

For bigger changes, make sure you start a discussion first by creating
an issue and explaining the intended change.

Atlassian requires contributors to sign a Contributor License Agreement,
known as a CLA. This serves as a record stating that the contributor is
entitled to contribute the code/documentation/translation to the project
and is willing to have it used in distributions and derivative works
(or is willing to transfer ownership).

Prior to accepting your contributions we ask that you please follow the appropriate
link below to digitally sign the CLA. The Corporate CLA is for those who are
contributing as a member of an organization and the individual CLA is for
those contributing as an individual.

* [CLA for corporate contributors](https://na2.docusign.net/Member/PowerFormSigning.aspx?PowerFormId=e1c17c66-ca4d-4aab-a953-2c231af4a20b)
* [CLA for individuals](https://na2.docusign.net/Member/PowerFormSigning.aspx?PowerFormId=3f94fbdc-2fbe-46ac-b14c-5d152700ae5d)

## License ##

Copyright (c) 2016 Atlassian and others. Apache 2.0 licensed, see LICENSE.txt file.