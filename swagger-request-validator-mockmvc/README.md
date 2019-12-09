# Swagger Request Validator - Spring MockMvc #

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-mockmvc/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-mockmvc)

Integrations between the Swagger Request Validator and version 5.1 onwards of [Spring MockMvc](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/servlet/MockMvc.html).

For integration with earlier versions of Spring MockMvc see [swagger-request-validator-mockmvc-legacy](../swagger-request-validator-mockmvc-legacy).

This module includes request/response adaptors that allow validation of Spring MockMvc interactions with the Swagger Request
Validator, and a `ResultMatcher` that can be used to add validation to a MockMvc interaction.

## Usage ##

```
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-mockmvc</artifactId>
    <version>${swagger-request-validator.version}</version>
</dependency>
```

See [SwaggerValidationMatchersTest](src/test/java/com/atlassian/oai/validator/mockmvc/SwaggerValidationMatchersTest.java) 
and the [examples module](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/?at=master)
for running examples of how the MockMvc module can be used.

### ResultMatcher ###
The simplest way to use the integration is to add the `ResultMatcher` to the expectations of a MockMvc interaction.

```java

@Test
public void testStuff() {
    this.mvc
        .perform(get("/hello/bob"))
        .andExpect(openApi().isValid("api.json"));
}
```

If you need to customise the validation behavior you can supply a pre-initialised validator to the `isValid` matcher.

```java
@BeforeClass
public static void setup() {
    validator = ...some validator initialisation and configuration...
}

@Test
public void testStuff() throws Exception {
    this.mvc
        .perform(post("/hello/bob")
        .andExpect(openApi().isValid(validator));
}
```

See the [README for swagger-request-validator-core](../swagger-request-validator-core/README.md) for details on how to configure the validator.