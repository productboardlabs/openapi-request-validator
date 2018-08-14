# Swagger Request Validator - REST Assured #

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-restassured/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-restassured)

Integrations between the Swagger Request Validator and the [REST Assured testing library](http://rest-assured.io/).

This module includes request/response adaptors that allow validation of REST Assured interactions with the Swagger Request
Validator, and a `SwaggerValidationFilter` that can be used to add validation to a REST Assured interaction.

## Usage ##

```
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-restassured</artifactId>
    <version>${swagger-request-validator.version}</version>
</dependency>
```

See the [examples module](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/?at=master)
for running examples of how the REST Assured module can be used.

### SwaggerValidationFilter ###
The simplest way to use the integration is to add the `OpenApiValidationFilter` to a REST Assured interaction.

```
private final OpenApiValidationFilter validationFilter = new OpenApiValidationFilter(API_SPEC_URL);

...

@Test
public void testGetValidPet() {
    given()
            .port(PORT)
            .filter(validationFilter)
    .when()
            .get("/pet/1")
    .then()
            .assertThat()
            .statusCode(200);
}
```