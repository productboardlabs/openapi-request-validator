# Swagger Request Validator - Core #

The core validator logic in the Swagger Request Validator.

Designed to be be standalone and used independently of any HTTP library or mocking framework etc.

## Features ##

* Standalone - no dependency on HTTP libraries etc.
* JSON Schema validation support - including schema references

## Usage ##

See the [examples module](https://bitbucket.org/atlassian/swagger-request-validator/src/master/swagger-request-validator-examples/?at=master)
for examples on how the library is used.

The main entry point to the library is the `com.atlassian.oai.validator.SwaggerRequestResponseValidator`.
This validator takes a specification file (local or remote URL) and can then be used to validate request/response pairs.

The validator returns a `com.atlassian.oai.validator.report.ValidationReport` which will contain any errors that
occurred during the validation. These can be used to generate a report for users etc.

```
final SwaggerRequestResponseValidator validator = new SwaggerRequestResponseValidator(swaggerJsonUrl, basePathOverride);
final ValidationReport report = validator.validate(request, response);

if (report.hasErrors()) {
    ...
}
```

See the javadoc for the library for more information on how to use individual classes.