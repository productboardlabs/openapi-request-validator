# OpenAPI v3 Specification Support #

| Feature                          | Supported                                                                | Notes                                                                          |
|-- ------------------------------ | ------------------------------------------------------------------------ | ---------------------------------------------------------------------------- --|
| **Server Object**                |                                                                          |                                                                                |
| url                              | Yes                                                                      | Currently only supports a single Server definition                             |
| variables                        | Yes                                                                      |                                                                                |
|                                  |                                                                          |                                                                                |
| **Paths Object**                 |                                                                          |                                                                                |
| variables                        | Yes                                                                      |                                                                                |
| servers                          | No                                                                       |                                                                                |
| parameters                       | Yes                                                                      |                                                                                |
|                                  |                                                                          |                                                                                |
| get                              | Yes                                                                      |                                                                                |
| put                              | Yes                                                                      |                                                                                |
| post                             | Yes                                                                      |                                                                                |
| delete                           | Yes                                                                      |                                                                                |
| options                          | Yes                                                                      |                                                                                |
| head                             | Yes                                                                      |                                                                                |
| patch                            | Yes                                                                      |                                                                                |
| trace                            | Yes                                                                      |                                                                                |
|                                  |                                                                          |                                                                                |
| **Operation Object**             |                                                                          |                                                                                |
| parameters                       | Yes                                                                      |                                                                                |
| requestBody                      | Yes                                                                      |                                                                                |
| responses                        | Yes                                                                      |                                                                                |
| callbacks                        | No                                                                       |                                                                                |
| deprecated                       | No                                                                       |                                                                                |
| security                         | Partial                                                                  |                                                                                |
| servers                          | No                                                                       |                                                                                |
|                                  |                                                                          |                                                                                |
| **Parameter Object**             |                                                                          |                                                                                |
| in                               | `query`, `header`, `path`                                                |                                                                                |
| required                         | Yes                                                                      |                                                                                |
| deprecated                       | No                                                                       |                                                                                |
| allowEmptyValue                  | Yes                                                                      |                                                                                |
| style                            | `matrix`, `label`, `form`, `simple`, `spaceDelimited`, `pipeDelimited`   |                                                                                |
| allowReserved                    | No                                                                       |                                                                                |
| schema                           | Yes                                                                      |                                                                                |
| example                          | No                                                                       |                                                                                |
| examples                         | No                                                                       |                                                                                |
| content                          | No                                                                       |                                                                                |
|                                  |                                                                          |                                                                                |
| **Request Body Object**          |                                                                          |                                                                                |
| content                          | Yes                                                                      |                                                                                |
| required                         | Yes                                                                      |                                                                                |
|                                  |                                                                          |                                                                                |
| application/json                 | Yes                                                                      |                                                                                |
| application/hal+json             | Yes                                                                      |                                                                                |
| application/xml                  | No                                                                       |                                                                                |
| application/x-www-form-encoded   | No                                                                       |                                                                                |
| text/plain                       | No                                                                       |                                                                                |
| text/xml                         | No                                                                       |                                                                                |
| multipart/form-data              | No                                                                       |                                                                                |
|                                  |                                                                          |                                                                                |
| **Media Type Object**            |                                                                          |                                                                                |
| schema                           | Yes                                                                      |                                                                                |
| example                          | No                                                                       |                                                                                |
| examples                         | No                                                                       |                                                                                |
| encoding                         | No                                                                       | Not needed until multipart support is added                                    |
|                                  |                                                                          |                                                                                |
| **Schema Object**                |                                                                          |                                                                                |
| $ref                             | Yes                                                                      |                                                                                |
|                                  |                                                                          |                                                                                |
| title                            | Yes                                                                      |                                                                                |
| multipleOf                       | Yes                                                                      |                                                                                |
| maximum                          | Yes                                                                      |                                                                                |
| exclusiveMaximum                 | Yes                                                                      |                                                                                |
| minimum                          | Yes                                                                      |                                                                                |
| exclusiveMinimum                 | Yes                                                                      |                                                                                |
| maxLength                        | Yes                                                                      |                                                                                |
| minLength                        | Yes                                                                      |                                                                                |
| pattern                          | Yes                                                                      |                                                                                |
| maxItems                         | Yes                                                                      |                                                                                |
| minItems                         | Yes                                                                      |                                                                                |
| uniqueItems                      | Yes                                                                      |                                                                                |
| maxProperties                    | Yes                                                                      |                                                                                |
| minProperties                    | Yes                                                                      |                                                                                |
| required                         | Yes                                                                      |                                                                                |
| enum                             | Yes                                                                      |                                                                                |
|                                  |                                                                          |                                                                                |
| type                             | Yes                                                                      |                                                                                |
| allOf                            | Yes                                                                      | Does not work when `additionalProperties` is `false`                           |
| oneOf                            | ?                                                                        |                                                                                |
| anyOf                            | ?                                                                        |                                                                                |
| not                              | ?                                                                        |                                                                                |
| items                            | Yes                                                                      |                                                                                |
| properties                       | Yes                                                                      |                                                                                |
| additionalProperties             | Partial                                                                  | Only supports the boolean flag variant                                         |
| format                           | Yes                                                                      | See below for supported formats                                                |
|                                  |                                                                          |                                                                                |
| nullable                         | Yes                                                                      |                                                                                |
| discriminator                    | Yes                                                                      |                                                                                |
| readOnly                         | No                                                                       |                                                                                |
| writeOnly                        | No                                                                       |                                                                                |
| xml                              | No                                                                       |                                                                                |
| example                          | No                                                                       |                                                                                |
| deprecated                       | No                                                                       |                                                                                |
|                                  |                                                                          |                                                                                |
| **Discriminator Object**         |                                                                          |                                                                                |
| propertyName                     | Yes                                                                      |                                                                                |
| mapping                          | Yes                                                                      |                                                                                |
|                                  |                                                                          |                                                                                |
| **Security Scheme Object**       |                                                                          |                                                                                |
| type                             | `apiKey`, `http`                                                         |                                                                                |
| name                             | Yes                                                                      |                                                                                |
| in                               | `header`, `query`                                                        | Ignored when `type` is `http`                                                  |
| scheme                           | No                                                                       |                                                                                |
| bearerFormat                     | No                                                                       |                                                                                |
| flows                            | No                                                                       |                                                                                |
| openIdConnectUrl                 | No                                                                       |                                                                                |
|                                  |                                                                          |                                                                                |
| **Supported Data Type Formats**  |                                                                          |                                                                                |
| int32                            | Yes                                                                      |                                                                                |
| int64                            | Yes                                                                      |                                                                                |
| float                            | Yes                                                                      |                                                                                |
| double                           | Yes                                                                      |                                                                                |
| string                           | Yes                                                                      |                                                                                |
| byte                             | No                                                                       |                                                                                |
| binary                           | No                                                                       |                                                                                |
| boolean                          | Yes                                                                      |                                                                                |
| date                             | Yes                                                                      |                                                                                |
| date-time                        | Yes                                                                      |                                                                                |
| password                         | No                                                                       |                                                                                |
| email                            | Yes                                                                      |                                                                                |
| ipv4                             | Yes                                                                      |                                                                                |
| ipv6                             | Yes                                                                      |                                                                                |
| uri                              | Yes                                                                      |                                                                                |
| uri-template                     | Yes                                                                      |                                                                                |
| uuid                             | Yes                                                                      |                                                                                |
| md5                              | Yes                                                                      |                                                                                |
| sha1                             | Yes                                                                      |                                                                                |
| sha256                           | Yes                                                                      |                                                                                |
| sha512                           | Yes                                                                      |                                                                                |