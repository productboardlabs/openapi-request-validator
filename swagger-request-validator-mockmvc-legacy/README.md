# Swagger Request Validator - Spring MockMvc Legacy #

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-mockmvc-legacy/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-mockmvc-legacy)

Integrations between the Swagger Request Validator and V4.x and V5.0 of [Spring MockMvc](http://docs.spring.io/spring-security/site/docs/current/reference/html/test-mockmvc.html).

For integration with later versions of Spring MockMvc see [swagger-request-validator-mockmvc](../swagger-request-validator-mockmvc).

This module includes request/response adaptors that allow validation of Spring MockMvc interactions with the Swagger Request
Validator, and a `ResultMatcher` that can be used to add validation to a MockMvc interaction.

## Usage ##

```
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-mockmvc-legacy</artifactId>
    <version>${swagger-request-validator.version}</version>
</dependency>
```

See [SwaggerValidationMatchersTest](src/test/java/com/atlassian/oai/validator/mockmvc/SwaggerValidationMatchersTest.java) 
and the [examples module](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/?at=master)
for running examples of how the MockMvc module can be used.

### ResultMatcher ###
The simplest way to use the integration is to add the `ResultMatcher` to the expectations of a MockMvc interaction.

```

@Test
public void testGetValidPet() {
    this.mvc
        .perform(get("/hello/bob"))
        .andExpect(openApi().isValid("api.json"));
}
```
