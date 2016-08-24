# Swagger Request Validator #

![build-status](https://bitbucket-badges.atlassian.io/badge/atlassian/swagger-request-validator.svg)

A Java library for validating HTTP request/responses against a [Swagger/OpenAPI](https://openapis.org/) specification.

Designed to be used independently of any HTTP library or framework, the library can be used to validate
request/responses from almost any source (e.g. in a REST client, in unit tests that use mocked responses,
in Pact tests etc.)

## Key features ##

* Standalone - no dependencies on HTTP libraries or frameworks
* Adapters for commonly used HTTP libraries and testing frameworks
* JSON Schema validation support - including schema references

## Usage ##

See the [examples module](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/?at=master)
for examples on how the library is used.

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
* Follow the existing style
* Separate unrelated changes into multiple pull requests

See the existing [issues](https://bitbucket.org/atlassian/swagger-request-validator/issues) for things to start
contributing.

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