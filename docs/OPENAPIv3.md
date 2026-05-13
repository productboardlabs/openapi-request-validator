# OpenAPI v3 Specification Support #

This document covers support for both **OpenAPI 3.0.x** and **OpenAPI 3.1.x**.
Features specific to one version are called out in the Notes column.

---

## Core Objects

| Feature                           | Supported | Notes                                                                                |
|-----------------------------------|-----------|--------------------------------------------------------------------------------------|
| **Server Object**                 |           |                                                                                      |
| url                               | Yes       | Currently only supports a single Server definition                                   |
| variables                         | Yes       |                                                                                      |
| Multiple servers                  | No        |                                                                                      |
|                                   |           |                                                                                      |
| **Paths Object**                  |           |                                                                                      |
| variables                         | Yes       |                                                                                      |
| servers (path-level)              | No        |                                                                                      |
| parameters (path-level)           | Yes       |                                                                                      |
|                                   |           |                                                                                      |
| **HTTP Methods**                  |           |                                                                                      |
| get                               | Yes       |                                                                                      |
| put                               | Yes       |                                                                                      |
| post                              | Yes       |                                                                                      |
| delete                            | Yes       |                                                                                      |
| options                           | Yes       |                                                                                      |
| head                              | Yes       |                                                                                      |
| patch                             | Yes       |                                                                                      |
| trace                             | Yes       |                                                                                      |
|                                   |           |                                                                                      |
| **Operation Object**              |           |                                                                                      |
| parameters                        | Yes       |                                                                                      |
| requestBody                       | Yes       |                                                                                      |
| responses                         | Yes       |                                                                                      |
| callbacks                         | No        |                                                                                      |
| deprecated                        | No        |                                                                                      |
| security                          | Partial   | See Security Scheme Object below                                                     |
| servers (operation-level)         | No        |                                                                                      |

---

## Parameter Object

| Feature            | Supported                                                                                         | Notes                                                                                                              |
|--------------------|---------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| in                 | `query`, `header`, `path`, `cookie`                                                               |                                                                                                                    |
| required           | Yes                                                                                               |                                                                                                                    |
| deprecated         | No                                                                                                |                                                                                                                    |
| allowEmptyValue    | Yes                                                                                               |                                                                                                                    |
| style              | `matrix`, `label`, `form`, `simple`, `spaceDelimited`, `pipeDelimited`, `deepObject`              | `deepObject` has basic support only — string parameters only; arrays and nested objects are not supported          |
| allowReserved      | No                                                                                                |                                                                                                                    |
| schema             | Yes                                                                                               |                                                                                                                    |
| example            | No                                                                                                |                                                                                                                    |
| examples           | No                                                                                                |                                                                                                                    |
| content            | No                                                                                                |                                                                                                                    |

---

## Request Body Object

| Feature                           | Supported | Notes                                                                                |
|-----------------------------------|-----------|--------------------------------------------------------------------------------------|
| content                           | Yes       |                                                                                      |
| required                          | Yes       |                                                                                      |
|                                   |           |                                                                                      |
| application/json                  | Yes       |                                                                                      |
| application/hal+json              | Yes       |                                                                                      |
| application/xml                   | No        |                                                                                      |
| application/x-www-form-urlencoded | Yes       | Basic support using default encoding. The `encoding` object is not supported.        |
| multipart/form-data               | No        |                                                                                      |
| text/plain                        | No        |                                                                                      |
| text/xml                          | No        |                                                                                      |

---

## Media Type Object

| Feature  | Supported | Notes |
|----------|-----------|-------|
| schema   | Yes       |       |
| example  | No        |       |
| examples | No        |       |
| encoding | No        |       |

---

## Schema Object

### Common keywords (OpenAPI 3.0 and 3.1)

| Feature               | Supported | Notes                                                                                  |
|-----------------------|-----------|----------------------------------------------------------------------------------------|
| $ref                  | Yes       | Including recursive `$ref`                                                             |
| title                 | Yes       |                                                                                        |
| type                  | Yes       |                                                                                        |
| format                | Yes       | See Supported Data Type Formats below                                                  |
| enum                  | Yes       |                                                                                        |
| required              | Yes       |                                                                                        |
| properties            | Yes       |                                                                                        |
| additionalProperties  | Yes       | Both boolean (`true`/`false`) and schema object variants supported                     |
| items                 | Yes       |                                                                                        |
| allOf                 | Yes       |                                                                                        |
| oneOf                 | Yes       |                                                                                        |
| anyOf                 | Yes       |                                                                                        |
| not                   | Yes       |                                                                                        |
| discriminator         | Yes       | `propertyName` and `mapping` supported                                                 |
| readOnly              | Yes       | `readOnly` fields are not required in requests                                         |
| writeOnly             | Yes       | `writeOnly` fields are not required in responses                                       |
| nullable (3.0 only)   | Yes       | OAS 3.0 `nullable: true` extension                                                     |
| multipleOf            | Yes       |                                                                                        |
| maximum               | Yes       |                                                                                        |
| exclusiveMaximum      | Yes       | Boolean form (OAS 3.0) and numeric form (OAS 3.1) both supported                      |
| minimum               | Yes       |                                                                                        |
| exclusiveMinimum      | Yes       | Boolean form (OAS 3.0) and numeric form (OAS 3.1) both supported                      |
| maxLength             | Yes       |                                                                                        |
| minLength             | Yes       |                                                                                        |
| pattern               | Yes       |                                                                                        |
| maxItems              | Yes       |                                                                                        |
| minItems              | Yes       |                                                                                        |
| uniqueItems           | Yes       |                                                                                        |
| maxProperties         | Yes       |                                                                                        |
| minProperties         | Yes       |                                                                                        |
| xml                   | No        |                                                                                        |
| example               | No        |                                                                                        |
| deprecated            | No        |                                                                                        |

### OpenAPI 3.1 keywords (JSON Schema 2020-12)

| Feature                  | Supported | Notes                                                                             |
|--------------------------|-----------|-----------------------------------------------------------------------------------|
| type (array / union)     | Yes       | e.g. `type: [string, integer]` and `type: [string, 'null']` for nullability       |
| const                    | Yes       |                                                                                   |
| if / then / else         | Yes       |                                                                                   |
| dependentRequired        | Yes       |                                                                                   |
| dependentSchemas         | Yes       |                                                                                   |
| prefixItems              | Yes       | Tuple validation                                                                  |
| unevaluatedProperties    | Yes       |                                                                                   |
| unevaluatedItems         | Partial   | Basic parsing support; behaviour may vary in complex compositions                 |
| contentMediaType         | No        | Parses cleanly but value is not enforced                                          |
| contentEncoding          | No        | Parses cleanly but value is not enforced                                          |
| Boolean schemas          | Partial   | `true` / `false` schemas load but top-level boolean schema behaviour may vary     |
| $ref with sibling keywords | Yes     | OAS 3.1 allows additional keywords alongside `$ref`                               |
| examples (plural)        | No        | Parses cleanly; values are not validated                                          |

---

## Discriminator Object

| Feature      | Supported | Notes |
|--------------|-----------|-------|
| propertyName | Yes       |       |
| mapping      | Yes       |       |

---

## Responses Object

| Feature         | Supported | Notes                                                                    |
|-----------------|-----------|--------------------------------------------------------------------------|
| default         | Yes       |                                                                          |
| HTTP Status Code | Yes      |                                                                          |
| Range patterns  | Yes       | `1XX`, `2XX`, `3XX`, `4XX`, `5XX` wildcard ranges are supported          |

---

## Response Object

| Feature                           | Supported | Notes                                                    |
|-----------------------------------|-----------|----------------------------------------------------------|
| headers                           | Yes       |                                                          |
| content                           | Yes       |                                                          |
| links                             | No        |                                                          |
|                                   |           |                                                          |
| application/json                  | Yes       |                                                          |
| application/hal+json              | Yes       |                                                          |
| application/xml                   | No        |                                                          |
| application/x-www-form-urlencoded | Yes       | Basic support; `encoding` object not supported           |
| text/plain                        | No        |                                                          |
| text/xml                          | No        |                                                          |

---

## Security Scheme Object

| Feature          | Supported | Notes                                                                                       |
|------------------|-----------|---------------------------------------------------------------------------------------------|
| type: `apiKey`   | Yes       | `in: header`, `in: query`, and `in: cookie` all supported                                   |
| type: `http`     | Partial   | `scheme: basic` and `scheme: bearer` validated; other HTTP schemes ignored                  |
| type: `oauth2`   | No        | Presence of a security requirement is not enforced                                          |
| type: `openIdConnect` | No   | Presence of a security requirement is not enforced                                          |
| name             | Yes       |                                                                                             |
| in               | `header`, `query`, `cookie` | Applies to `apiKey` type                                                   |
| scheme           | Partial   | `basic` and `bearer` validated; others ignored                                              |
| bearerFormat     | No        |                                                                                             |
| flows            | No        |                                                                                             |
| openIdConnectUrl | No        |                                                                                             |

---

## Webhooks (OpenAPI 3.1 only)

| Feature                  | Supported | Notes                                                        |
|--------------------------|-----------|--------------------------------------------------------------|
| webhooks                 | Yes       | Request and response validation via dedicated validator methods |
| `validateWebhookRequest` | Yes       |                                                              |
| `validateWebhookResponse`| Yes       |                                                              |
| `validateWebhook`        | Yes       | Validates both request and response together                 |

---

## Components Object

| Feature                | Supported | Notes                                                            |
|------------------------|-----------|------------------------------------------------------------------|
| schemas                | Yes       |                                                                  |
| responses              | Yes       |                                                                  |
| parameters             | Yes       |                                                                  |
| requestBodies          | Yes       |                                                                  |
| headers                | Yes       |                                                                  |
| securitySchemes        | Partial   | See Security Scheme Object above                                 |
| pathItems (3.1 only)   | Partial   | Path item `$ref` resolution from components works for direct paths; reusable pathItems in webhooks may vary |
| links                  | No        |                                                                  |
| callbacks              | No        |                                                                  |

---

## Supported Data Type Formats

These formats are actively validated (both custom implementations and built-in library support):

| Format        | Supported | Notes                                                   |
|---------------|-----------|---------------------------------------------------------|
| int32         | Yes       | Custom validator — validates 32-bit integer range       |
| int64         | Yes       | Custom validator — validates 64-bit integer range       |
| float         | Yes       | Custom validator — validates 32-bit float range         |
| double        | Yes       | Custom validator — validates 64-bit double range        |
| byte          | Yes       | Custom validator — validates Base64 encoding            |
| binary        | No        |                                                         |
| boolean       | Yes       |                                                         |
| date          | Yes       |                                                         |
| date-time     | Yes       | RFC3339 normalisation applied                           |
| time          | Yes       | OpenAPI 3.1 / JSON Schema 2020-12                       |
| duration      | Yes       | OpenAPI 3.1 / JSON Schema 2020-12                       |
| password      | No        |                                                         |
| email         | Yes       |                                                         |
| idn-email     | Yes       | OpenAPI 3.1 / JSON Schema 2020-12                       |
| hostname      | Yes       |                                                         |
| idn-hostname  | Yes       | OpenAPI 3.1 / JSON Schema 2020-12                       |
| ipv4          | Yes       |                                                         |
| ipv6          | Yes       |                                                         |
| uri           | Yes       |                                                         |
| uri-reference | Yes       |                                                         |
| uri-template  | Yes       |                                                         |
| iri           | Yes       | OpenAPI 3.1 / JSON Schema 2020-12                       |
| iri-reference | Yes       | OpenAPI 3.1 / JSON Schema 2020-12                       |
| uuid          | Yes       |                                                         |
| json-pointer  | Yes       |                                                         |
| relative-json-pointer | Yes | OpenAPI 3.1 / JSON Schema 2020-12                    |
| regex         | Yes       |                                                         |
| md5           | Yes       |                                                         |
| sha1          | Yes       |                                                         |
| sha256        | Yes       |                                                         |
| sha512        | Yes       |                                                         |
