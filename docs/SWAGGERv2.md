# Swagger v2 Specification Support #

| Feature                             | Supported                                       | Notes                                                  |
|------------------------------------ | ----------------------------------------------- | -------------------------------------------------------|
| **Swagger Object**                  |                                                 |                                                        |
| host                                | No                                              |                                                        |
| basePath                            | Yes                                             | Can be overridden                                      |
| schemes                             | No                                              |                                                        |
| consumes                            | Yes                                             |                                                        |
| produces                            | Yes                                             |                                                        |
| paths                               | Yes                                             |                                                        |
| definitions                         | Yes                                             |                                                        |
| parameters                          | Yes                                             |                                                        |
| responses                           | Yes                                             |                                                        |
| securityDefinitions                 | Partial                                         |                                                        |
| security                            | Yes                                             |                                                        |
| tags                                | No                                              |                                                        |
|                                     |                                                 |                                                        |
| **Path Item Object**                |                                                 |                                                        |
| $ref                                | Yes                                             |                                                        |
| parameters                          | Yes                                             |                                                        |
|                                     |                                                 |                                                        |
| get                                 | Yes                                             |                                                        |
| put                                 | Yes                                             |                                                        |
| post                                | Yes                                             |                                                        |
| delete                              | Yes                                             |                                                        |
| options                             | Yes                                             |                                                        |
| head                                | Yes                                             |                                                        |
| patch                               | Yes                                             |                                                        |
|                                     |                                                 |                                                        |
| **Operation Object**                |                                                 |                                                        |
| consumes                            | Yes                                             |                                                        |
| produces                            | Yes                                             |                                                        |
| parameters                          | Yes                                             |                                                        |
| responses                           | Yes                                             |                                                        |
| schemes                             | No                                              |                                                        |
| deprecated                          | No                                              |                                                        |
| security                            | Yes                                             |                                                        |
|                                     |                                                 |                                                        |
| **Parameter Object**                |                                                 |                                                        |
| name                                | Yes                                             |                                                        |
| in                                  | Yes                                             |                                                        |
| required                            | Yes                                             |                                                        |
| schema                              | Yes                                             |                                                        |
| type                                | Yes                                             | See supported data types below                         |
| format                              | Yes                                             | See supported data types below                         |
| allowEmptyValue                     | Yes                                             |                                                        |
| items                               | Yes                                             |                                                        |
| collectionFormat                    | Yes                                             |                                                        |
|                                     |                                                 |                                                        |
| maximum                             | Yes                                             |                                                        |
| exclusiveMaximum                    | Yes                                             |                                                        |
| minimum                             | Yes                                             |                                                        |
| exclusiveMinimum                    | Yes                                             |                                                        |
| maxLength                           | Yes                                             |                                                        |
| minLength                           | Yes                                             |                                                        |
| pattern                             | Yes                                             |                                                        |
| maxItems                            | Yes                                             |                                                        |
| minItems                            | Yes                                             |                                                        |
| uniqueItems                         | Yes                                             |                                                        |
| enum                                | Yes                                             |                                                        |
| multipleOf                          | Yes                                             |                                                        |
|                                     |                                                 |                                                        |
| **Response Object**                 |                                                 |                                                        |
| schema                              | Yes                                             |                                                        |
| headers                             | Yes                                             |                                                        |
| examples                            | No                                              |                                                        |
|                                     |                                                 |                                                        |
| **Header Object**                   |                                                 |                                                        |
| type                                | Yes                                             |                                                        |
| format                              | Yes                                             |                                                        |
| items                               | Yes                                             |                                                        |
| collectionFormat                    | Yes                                             |                                                        |
|                                     |                                                 |                                                        |
| maximum                             | Yes                                             |                                                        |
| exclusiveMaximum                    | Yes                                             |                                                        |
| minimum                             | Yes                                             |                                                        |
| exclusiveMinimum                    | Yes                                             |                                                        |
| maxLength                           | Yes                                             |                                                        |
| minLength                           | Yes                                             |                                                        |
| pattern                             | Yes                                             |                                                        |
| maxItems                            | Yes                                             |                                                        |
| minItems                            | Yes                                             |                                                        |
| uniqueItems                         | Yes                                             |                                                        |
| enum                                | Yes                                             |                                                        |
| multipleOf                          | Yes                                             |                                                        |
|                                     |                                                 |                                                        |
| **Schema Object**                   |                                                 |                                                        |
| $ref                                | Yes                                             |                                                        |
|                                     |                                                 |                                                        |
| title                               | Yes                                             |                                                        |
| multipleOf                          | Yes                                             |                                                        |
| maximum                             | Yes                                             |                                                        |
| exclusiveMaximum                    | Yes                                             |                                                        |
| minimum                             | Yes                                             |                                                        |
| exclusiveMinimum                    | Yes                                             |                                                        |
| maxLength                           | Yes                                             |                                                        |
| minLength                           | Yes                                             |                                                        |
| pattern                             | Yes                                             |                                                        |
| maxItems                            | Yes                                             |                                                        |
| minItems                            | Yes                                             |                                                        |
| uniqueItems                         | Yes                                             |                                                        |
| maxProperties                       | Yes                                             |                                                        |
| minProperties                       | Yes                                             |                                                        |
| required                            | Yes                                             |                                                        |
| enum                                | Yes                                             |                                                        |
| type                                | Yes                                             | See supported data types below                         |
| allOf                               | Yes                                             | Does not work when `additionalProperties` is `false`   |
| items                               | Yes                                             |                                                        |
| properties                          | Yes                                             |                                                        |
| additionalProperties                | Yes                                             |                                                        |
| format                              | Yes                                             | See supported data types below                         |
|                                     |                                                 |                                                        |
| discriminator                       | Yes                                             |                                                        |
| readOnly                            | No                                              |                                                        |
| xml                                 | No                                              |                                                        |
| example                             | No                                              |                                                        |
|                                     |                                                 |                                                        |
| **Security Scheme Object**          |                                                 |                                                        |
| type                                | `basic`, `apiKey`                               |                                                        |
| name                                | Yes                                             |                                                        |
| in                                  | Yes                                             |                                                        |
| flow                                | No                                              |                                                        |
| authorizationUrl                    | No                                              |                                                        |
| tokenUrl                            | No                                              |                                                        |
| scopes                              | No                                              |                                                        |
|                                     |                                                 |                                                        |
| **Supported Data Type Formats**     |                                                 |                                                        |
| int32                               | Yes                                             |                                                        |
| int64                               | Yes                                             |                                                        |
| float                               | Yes                                             |                                                        |
| double                              | Yes                                             |                                                        |
| string                              | Yes                                             |                                                        |
| byte                                | No                                              |                                                        |
| binary                              | No                                              |                                                        |
| boolean                             | Yes                                             |                                                        |
| date                                | Yes                                             |                                                        |
| date-time                           | Yes                                             |                                                        |
| password                            | No                                              |                                                        |
| email                               | Yes                                             |                                                        |
| ipv4                                | Yes                                             |                                                        |
| ipv6                                | Yes                                             |                                                        |
| uri                                 | Yes                                             |                                                        |
| uri-template                        | Yes                                             |                                                        |
| uuid                                | Yes                                             |                                                        |
| md5                                 | Yes                                             |                                                        |
| sha1                                | Yes                                             |                                                        |
| sha256                              | Yes                                             |                                                        |
| sha512                              | Yes                                             |                                                        |
|                                     |                                                 |                                                        |
| **Supported Media Types**           |                                                 |                                                        |
| application/json                    | Yes                                             |                                                        |
| application/hal+json                | Yes                                             |                                                        |
| application/xml                     | No                                              |                                                        |
| application/x-www-form-encoded      | Yes                                             |                                                        |
| text/plain                          | No                                              |                                                        |
| text/xml                            | No                                              |                                                        |
| multipart/form-data                 | No                                              |                                                        |