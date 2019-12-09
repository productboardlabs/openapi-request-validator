## FAQ ##

#### I use JSON schema composition with `allOf`, `anyOf` or `oneOf` and am getting unexpected validation errors. Whats going on?

E.g. 
```
ERROR - Object instance has properties which are not allowed by the schema: ["city","country","firstname","lastname"] 
ERROR - Instance failed to match all required schemas (matched only 1 out of 2)
```

This is a known problem to do with the interaction of `additionalProperties` and composition (`anyOf`, `oneOf`, `allOf`) in 
JSON Schema ([see this discussion for more information](http://stackoverflow.com/questions/22689900/json-schema-allof-with-additionalproperties)).

By default the validator will check for additional properties in your request/response interactions and report an 
error if they contain properties not explicitly defined in the API specification. 

This is useful in test scenarios where, for example, you might be checking that your mocks are valid according to the
specification. In this case it is beneficial to ensure that you aren't including additional fields in your mock response 
that don't exist in the API specification (to detect e.g. removal of a field by a provider service, or to ensure you 
aren't relying on a field that will never be returned by the service).

The validator does this by inserting `additionalProperties:false` into the schema before it validates 
the request/response against it.

In a schema that uses composition, however, this can lead to the above errors.

This problem will also occur in any schema that uses `discriminator`, as that by necessity uses the `allOf` keyword.

To fix the problem you will need to opt-out of the `additionalProperties` validation by setting the level of 
`validation.schema.additionalProperties` to `IGNORE`.

e.g.

```java
final OpenApiInteractionValidator validator = OpenApiInteractionValidator
        .createFor(specUrl)
        .withLevelResolve(LevelResolverFactory.withAdditionalPropertiesIgnored())
        .build;
```

or via a `swagger-validator` config file

```
validation.schema.additionalProperties=IGNORE
```

See the [Core README](../swagger-request-validator-core/README.md) for more details on controlling validation behavior.

#### I want to raise an error when I have an expected query parameter in my request

By default, the validator will ignore unexpected query parameters. To raise an error in this case, 
you will need to set `validation.request.parameter.query.unexpected` to `ERROR`

e.g.

```java

final OpenApiInteractionValidator validator = OpenApiInteractionValidator
         .createFor(specUrl)
         .withLevelResolver(LevelResolver.create()
            .withLevel("validation.request.parameter.query.unexpected",
               ValidationReport.Level.ERROR)
             .build())
         .build());
```

or via a `swagger-validator` config 