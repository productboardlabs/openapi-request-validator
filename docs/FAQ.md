# FAQ

### How do I disable a validation error?

The validator supports specifying the "level" at which validations are emitted. By default, all validation problems are
emitted at an `ERROR` level. To disable a validation simply set its level to `IGNORE`.

You can control the level of validation errors in one of several ways:

1. With a `LevelResolver` when creating the validator instance

        final OpenApiInteractionValidator validator = 
            OpenApiInteractionValidator
                .createFor(specUrl)
                .withLevelResolver(
                    LevelResolver
                        .create()
                        .withLevel("validation.error.key", ValidationReport.Level.INFO)
                        .build()
                )      
            .build());

2. In a `swagger-validator.properties` file on your classpath

        validation.error.key=INFO

3. Via system properties specified at runtime

        -Dvalidation.error.key=INFO

See the javadoc for `LevelLoader` for more information.

If you need more fine-grained control over validation behavior you can also use the whitelisting mechanism which allows
much more control over when to ignore a specific validation error that can take into account the operation, path,
parameter values etc.

There are some examples of controlling validation behavior in the `swagger-request-validator-examples` module:

- [WhitelistingValidationErrorsTestExample](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/src/test/java/com/atlassian/oai/validator/examples/whitelist/WhitelistingValidationErrorsTestExample.java)
- [ValidationControlExamplesTest](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/src/test/java/com/atlassian/oai/validator/examples/validationcontrol/ValidationControlExamplesTest.java)

See the [Core README](../swagger-request-validator-core/README.md) for more details on controlling validation behavior.

### I use JSON schema composition with `allOf`, `anyOf` or `oneOf` and am getting unexpected validation errors. What's going on?

E.g.

```
ERROR - Object instance has properties which are not allowed by the schema: ["city","country","firstname","lastname"] 
ERROR - Instance failed to match all required schemas (matched only 1 out of 2)
```

This is a known problem to do with the interaction of `additionalProperties` and composition (`anyOf`, `oneOf`, `allOf`)
in JSON
Schema ([see this discussion for more information](http://stackoverflow.com/questions/22689900/json-schema-allof-with-additionalproperties))
.

By default, the validator will check for additional properties in your request/response interactions and report an error
if they contain properties not explicitly defined in the API specification.

This is useful in test scenarios where, for example, you might be checking that your mocks are valid according to the
specification. In this case it is beneficial to ensure that you aren't including additional fields in your mock response
that don't exist in the API specification (to detect e.g. removal of a field by a provider service, or to ensure you
aren't relying on a field that will never be returned by the service).

The validator does this by inserting `additionalProperties:false` into the schema before it validates the
request/response against it.

In a schema that uses composition, however, this can lead to the above errors.

There are two ways to address this problem:

#### Option 1: Resolve/inline the schemas

The underlying [swagger-parser](https://github.com/swagger-api/swagger-parser#3-flatten) library supports parse options
that can be used to resolve or inline the schemas (e.g. combine the `allOf` schemas into a single flattened schema).

You can set this option when creating the validator instance:

```java
final OpenApiInteractionValidator validator = OpenApiInteractionValidator
        .createForSpecificationUrl(specUrl)
        .withResolveCombinators(true)
        .build();
```

Or you can pass a fully constructed `ParseOptions` instance to further control the parse behavior.

```java
final ParseOptions parseOptions = new ParseOptions();
parseOptions.setResolveFully(true);
parseOptions.setResolveCombinators(true);

final OpenApiInteractionValidator validator = OpenApiInteractionValidator
        .createForSpecificationUrl(specUrl)
        .withParseOptions(parseOptions)
        .build();
```

See the [swagger-parser documentation](https://github.com/swagger-api/swagger-parser#3-flatten) for more details on what
the options do.

#### Option 2: Disable `additionalProperties` validation

Another way to fix the problem is to opt-out of the `additionalProperties` validation by setting the level of
`validation.schema.additionalProperties` to `IGNORE`.

e.g.

```java
final OpenApiInteractionValidator validator = OpenApiInteractionValidator
        .createForSpecificationUrl(specUrl)
        .withLevelResolve(LevelResolverFactory.withAdditionalPropertiesIgnored())
        .build();
```

or via a `swagger-validator` config file

```
validation.schema.additionalProperties=IGNORE
```

See the [Core README](../swagger-request-validator-core/README.md) for more details on controlling validation behavior.

### I want to raise an error when I have an unexpected query parameter in my request

By default, the validator will ignore unexpected query parameters. To raise an error in this case, you will need to
set `validation.request.parameter.query.unexpected` to `ERROR`

e.g.

```java

final OpenApiInteractionValidator validator = OpenApiInteractionValidator
        .createFor(specUrl)
        .withLevelResolver(
            LevelResolver
                .create()
                .withLevel("validation.request.parameter.query.unexpected", ValidationReport.Level.ERROR)
                .build()
            )   
        .build());
```

or via a `swagger-validator` config.

See the [Core README](../swagger-request-validator-core/README.md) for more details on controlling validation behavior.