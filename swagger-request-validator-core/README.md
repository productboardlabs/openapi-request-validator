# Swagger Request Validator - Core #

[![maven-central](https://maven-badges.herokuapp.com/maven-central/com.atlassian.oai/swagger-request-validator-core/badge.svg)](http://mvnrepository.com/artifact/com.atlassian.oai/swagger-request-validator-core)

The core validator logic in the Swagger Request Validator.

Designed to be be standalone and used independently of any HTTP library or mocking framework etc.

## Features ##

* Standalone - no dependency on HTTP libraries etc.
* JSON Schema validation support - including schema references
* Fine-grained control over validation behaviour

See [Features](../docs/FEATURES.md) for more details.

## Usage ##

```
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-core</artifactId>
    <version>${swagger-request-validator.version}</version>
</dependency>
```

See
the [examples module](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/?at=master)
for examples on how the library is used.

The main entry point to the library is the `com.atlassian.oai.validator.OpenApiInteractionValidator`. This validator
takes a specification file (local or remote URL) and can then be used to validate request/response pairs.

The validator returns a `com.atlassian.oai.validator.report.ValidationReport` which will contain any errors that
occurred during the validation. These can be used to generate a report for users etc.

```java
final OpenApiInteractionValidator validator = OpenApiInteractionValidator
        .createForSpecificationUrl(specUrl)
        .withBasePathOverride(basePathOverride)
        .build;
final ValidationReport report = validator.validate(request, response);

if (report.hasErrors()) {
    ...
}
```

Each report will contain 0 or more `Message`s, which include a key, a human-readable message (suitable for display to
users), a 'level' indicating the severity of the message, and additional context to help identify where the message was
generated.

A String representation of the report can be generated using one of the `ValidationReportFormat` implementations.
Currently two implementations are provided:

1. A `SimpleValidationReportFormat` that can be used to generate a human-readable format suitable for logging etc.; and
2. A `JsonValidationReportFormat` that outputs the report in a JSON format suitable for consumption by scripts / tooling
   etc.

```java
final ValidationReport report = validator.validate(request, response);
if (report.hasErrors()) {
  log.error(SimpleValidationReportFromat.getInstance().apply(report));
}
```

See the javadoc for the library for more information on how to use individual classes.

### Controlling validation behaviour ###

By default, all validation failures are emitted at `ERROR` level. This behaviour can be controlled on a per-validation
level, or for groups of validations.

The validation level resolution mechanism uses a hierarchical mechanism to resolve the level for a given validation
message. It begins with the message key and then checks each parent key for a level. If none are found, a global default
level is used.

For example, with the following configuration:

```
defaultLevel=IGNORE
validation.request=ERROR
validation.response=WARN
validation.response.body.missing=ERROR
```

The validation error `validation.response.body.missing` will be emitted at `ERROR` level, while the
error `validation.response.status.unknown` will be emitted at `WARN` (as it is a child of `validation.response`)
and `validation.schema.required` will be ignored (the `defaultLevel` will be applied as there are no parent keys that
match).

There are four levels that messages can be emitted at:

1. `ERROR` - Considered a failure
2. `WARN` - Considered important but won't cause a failure
3. `INFO` - A validation error has been found but is not considered important
4. `IGNORED` - No validation errors will be emitted

The list of validation messages can be found in `src/main/resources/messages.properties`.

There are 4 options for controlling validation behavior in your project.

#### Option 1 - Programmatically ####

When creating a `OpenApiInteractionValidator` instance you can specify a `LevelResolver` instance with programmatically
added validation level configuration.

```
this.validator = OpenApiInteractionValidator
        .createForSpecificationUrl(swaggerJsonUrl)
        .withLevelResolver(
                LevelResolver.create()
                        .withLevel("validation.schema.required", ValidationReport.Level.INFO)
                        .withLevel("validation.response.body.missing", ValidationReport.Level.INFO)
                        .build())
        .withBasePathOverride(basePathOverride)
        .build();
```

This is useful if you want to define a set of validation rules to be used across your project.

#### Option 2 - via `swagger-validator.properties` ####

The second option is to load configuration from a `swagger-validator.properties` file located at the root of your
project classpath (e.g. `src/main/resources/swagger-validator.properties`).

This file should contain properties of the form `{key}={LEVEL}`.

A special key `defaultLevel` can be used to set the global default.

```
defaultLevel=IGNORE
validation.request=ERROR
validation.response=WARN
validation.response.body.missing=ERROR
```

Keys in this file wil override any set programmatically.

#### Option 3 - in `.swagger-validator` ####

The third option is to have a file `.swagger-validator` in the working directory of your project
(e.g. the directory your project was run from).

This file has the same format as the `swagger-validator.properties` file above. Keys in this file wil override any set
via `swagger-validator.properties`.

#### Option 4 - via system properties ####

Finally, keys can be overridden via system properties of the form `swagger.{key}={LEVEL}`.

```
    java -jar my-project.jar -Dswagger.defaultLevel=WARN -Dswagger.validation.request=ERROR
```

These keys will override any other key that has been set.

### Whitelisting errors ###

There are scenarios where simple control of message levels is not enough. Perhaps you want to treat all messages of
certain types as errors but not in this one particular endpoint. Or maybe the validator reports some errors incorrectly
in a few obscure edge cases. Or maybe your OpenAPI / Swagger spec is not really that precise but for whatever reason you
can't make it 100% correct.

If that's the case, you can define whitelists to ignore messages based on fine-grained rules, defined using a
declarative fluent interface.

#### Example

Let's say that:

1. We have beans with properties of schema types valid in OpenApi 3.0 spec, but not 2.0, currently supported by this
   validator. Because of this, it incorrectly reports "validation.schema.additionalProperties"
   for these beans errors, which we would very much like to ignore.
2. What's more, we don't want to document 401 or 403 responses at all, and so we don't care about  
   "validation.response.status.unknown" errors for theses codes.
3. Also, we have some endpoints that don't return or accept "application/json", and we don't want validation to run in
   this case.

Here is a whitelist definition that we would create to rule out all the above:

```java
    ValidationErrorsWhitelist whitelist = ValidationErrorsWhitelist.create()
        .withRule(
            "1. Ignore additional properties in EntityPropertyBean",
            allOf( // logical AND: all conditions must be satisfied to whitelist a message
                messageHasKey("validation.schema.additionalProperties"), // whitelist only this message type
                entityIs("EntityPropertyBean"), // for the entity with problematic property
                messageContainsRegexp("\[\"value\"\]")) // and only if it's this particular property that is additional
        )
        .withRule(
            "2. Ignore 401 and 403 status codes",
            allOf(
                anyOf( // logical OR, we want to match errors for respones with either 401 or 403 status code
                    responseStatusIs(401),
                    responseStatusIs(403)),
                messageHasKey("3. Validation.response.status.unknown"))
        )
        .withRule("3. Ignore validation altogether if Content-type is not JSON, no questions asked",
            headerContainsRegexp("Content-Type", "application/json").not()); // notice "not()" at the end
```

All rules (`allOf`, `messageHasKey`, `entityIs` etc.) available for creating whitelists are defined in
the `WhitelistRules` class as static factory methods. Additionally, each rule can be negated with `.not()`.

Once you have a whitelist, simply pass it on to the validator builder:

```java
ValidationErrorsWhitelist whitelist = ...

final OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(spec)
                .withWhitelist(whitelist)
                .build();
```

If a message is whitelisted, it will still remain in the validation report, but its level will be changed to IGNORE, and
additional information with the matched rule name will be attached to it.

### Custom validation ###

In some cases, validation may be desired that is not provided out of the box. To add your own validation logic, a custom
validator can be created and registered. Custom validators can be registered to verify either the request or response. A
registered custom validator will be run for each request or response being validated against a specified operation.

An example of when to use a custom validator would be when a specification includes known extensions.

```java
final OpenApiInteractionValidator validator = OpenApiInteractionValidator.createFor(spec)
        .withCustomRequestValidator(new SimpleRequestValidator())
        .withCustomResponseValidator(new SimpleResponseValidator())
        .build();

...

private class SimpleRequestValidator implements CustomRequestValidator {
    @Override
    public ValidationReport validate(@Nonnull Request request, @Nonnull ApiOperation apiOperation) {
        if (apiOperation.getOperation().getVendorExtensions().containsKey("x-some-extension")) {
            if (!request.getHeaderValue("foo").isPresent()) {
                return ValidationReport.singleton((ValidationReport.Message.create("some.extension", "Required header foo missing.").build()));
            }
        }
        return ValidationReport.empty();
    }
}

private class SimpleResponseValidator implements CustomResponseValidator {
    @Override
    public ValidationReport validate(@Nonnull Response response, @Nonnull ApiOperation apiOperation) {
        if (apiOperation.getOperation().getVendorExtensions().containsKey("x-some-extension")) {
            if (!response.getHeaderValue("foo").isPresent()) {
                return ValidationReport.singleton((ValidationReport.Message.create("some.extension", "Required header foo missing.").build()));
            }
        }
        return ValidationReport.empty();
    }
}
```
