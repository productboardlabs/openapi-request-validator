# Swagger Request Validator - Core #

The core validator logic in the Swagger Request Validator.

Designed to be be standalone and used independently of any HTTP library or mocking framework etc.

## Features ##

* Standalone - no dependency on HTTP libraries etc.
* JSON Schema validation support - including schema references
* Fine-grained control over validation behaviour

## Usage ##

```
<dependency>
    <groupId>com.atlassian.oai</groupId>
    <artifactId>swagger-request-validator-core</artifactId>
    <version>${swagger-request-validator.version}</version>
</dependency>
```

See the [examples module](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/?at=master)
for examples on how the library is used.

The main entry point to the library is the `com.atlassian.oai.validator.SwaggerRequestResponseValidator`.
This validator takes a specification file (local or remote URL) and can then be used to validate request/response pairs.

The validator returns a `com.atlassian.oai.validator.report.ValidationReport` which will contain any errors that
occurred during the validation. These can be used to generate a report for users etc.

```
final SwaggerRequestResponseValidator validator = SwaggerRequestResponseValidator
        .createFor(swaggerJsonUrl)
        .withBasePathOverride(basePathOverride)
        .build;
final ValidationReport report = validator.validate(request, response);

if (report.hasErrors()) {
    ...
}
```

See the javadoc for the library for more information on how to use individual classes.

### Controlling validation behaviour ###

By default all validation failures are emitted at `ERROR` level. This behaviour can be controlled on a per-validation 
level, or for groups of validations.

The validation level resolution mechanism uses a hierarchical mechanism to resolve the level for a given validation message. 
It begins with the message key and then checks each parent key for a level. If none are found, a global default 
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
and `validation.schema.required` will be ignored (the `defaultLevel` will be applied as there are no parent 
keys that match).

There are four levels that messages can be emitted at:

1. `ERROR` - Considered a failure
2. `WARN` - Considered important but won't cause a failure
3. `INFO` - A validation error has been found but is not considered important
4. `IGNORED` - No validation errors will be emitted

The list of validation messages can be found in `src/main/resources/messages.properties`.

There are 4 options for controlling validation behavior in your project.

#### Option 1 - Programmatically ####

When creating a `SwaggerRequestResponseValidator` instance you can specify a `LevelResolver` instance 
with programmatically added validation level configuration.

```
this.validator = SwaggerRequestResponseValidator
        .createFor(swaggerJsonUrl)
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

The second option is to load configuration from a `swagger-validator.properties` file located at the root of 
your project classpath (e.g. `src/main/resources/swagger-validator.properties`).

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

This file has the same format as the `swagger-validator.properties` file above. 
Keys in this file wil override any set via `swagger-validator.properties`.

#### Option 4 - via system properties ####

Finally, keys can be overridden via system properties of the form `swagger.{key}={LEVEL}`.

```
    java -jar my-project.jar -Dswagger.defaultLevel=WARN -Dswagger.validation.request=ERROR
```

These keys will override any other key that has been set.