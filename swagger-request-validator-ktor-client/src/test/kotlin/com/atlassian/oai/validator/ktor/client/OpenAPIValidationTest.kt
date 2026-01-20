package com.atlassian.oai.validator.ktor.client

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.report.SimpleValidationReportFormat
import com.atlassian.oai.validator.report.ValidationReport
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.collections.emptyList
import kotlin.test.Test

class OpenAPIValidationTest {
    val givenValidBody = itemJson("active")
    val givenValidHeaders = headersOf("X-Request-Id" to listOf("123"))

    @Test
    fun `validates successful request`() = runTest {
        val client = validatingHttpClient(stubJsonResponse(HttpStatusCode.Created, givenValidBody)) {
            it.withInlineApiSpecification(testApiSpec)
        }

        assertFailsWith {
            client.post("/items") {
                header(HttpHeaders.ContentType, "application/json")
                setBody(givenValidBody)
            }
        }
    }

    @Test
    fun `validates response status`() = runTest {
        val client = validatingHttpClient(stubJsonResponse(HttpStatusCode.InternalServerError, givenValidBody)) {
            it.withInlineApiSpecification(testApiSpec)
        }

        assertFailsWith("validation.response.status.unknown") {
            client.post("/items") {
                header(HttpHeaders.ContentType, "application/json")
                setBody(givenValidBody)
            }
        }
    }

    @Test
    fun `validates response body against schema`() = runTest {
        val client = validatingHttpClient(stubJsonResponse(HttpStatusCode.Created, itemJson("invalid"))) {
            it.withInlineApiSpecification(testApiSpec)
        }

        assertFailsWith("validation.response.body.schema.enum") {
            client.post("/items") {
                header(HttpHeaders.ContentType, "application/json")
                setBody(givenValidBody)
            }
        }
    }

    @Test
    fun `validates required request header`() = runTest {
        val client = validatingHttpClient(stubJsonResponse(body = givenValidBody, headers = givenValidHeaders)) {
            it.withInlineApiSpecification(testApiSpec)
        }

        assertFailsWith("validation.request.parameter.header.missing") {
            client.get("/items/123") {
                url {
                    parameters.append("status", "active")
                }
                // Missing required X-API-Key header
            }
        }
    }

    @Test
    fun `validates required query parameter`() = runTest {
        val client = validatingHttpClient(stubJsonResponse(givenValidBody, headers = givenValidHeaders)) {
            it.withInlineApiSpecification(testApiSpec)
        }

        assertFailsWith("validation.request.parameter.query.missing") {
            client.get("/items/123") {
                header("X-API-Key", "test-key")
                // Missing required 'status' query parameter
            }
        }
    }

    @Test
    fun `validates required response header`() = runTest {
        val client = validatingHttpClient(stubJsonResponse(body = givenValidBody)) {
            it.withInlineApiSpecification(testApiSpec)
        }

        assertFailsWith("validation.response.header.missing") {
            client.get("/items/123") {
                header("X-API-Key", "test-key")
                url {
                    parameters.append("status", "active")
                }
            }
        }
    }

    @Test
    fun `validates request content type`() = runTest {
        val client = validatingHttpClient(stubJsonResponse(HttpStatusCode.Created, givenValidBody)) {
            it.withInlineApiSpecification(testApiSpec)
        }

        assertFailsWith("validation.request.contentType.notAllowed") {
            client.post("/items") {
                header(HttpHeaders.ContentType, "text/plain")
                setBody(givenValidBody)
            }
        }
    }

    private fun itemJson(status: String) = """
        {
          "id": 123,
          "status": "$status"
        }
    """.trimIndent()

    private fun validatingHttpClient(
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
        buildValidator: (OpenApiInteractionValidator.Builder) -> OpenApiInteractionValidator.Builder,
    ) = HttpClient(MockEngine(handler)) {
        install(OpenAPIValidation) {
            validator(buildValidator)
        }
    }

    private fun assertValidationExceptionMatches(
        expectedReport: ValidationReport,
        exception: OpenApiValidationException,
    ) {
        assertEquals(
            SimpleValidationReportFormat.getInstance().apply(expectedReport),
            exception.message,
        )
    }

    suspend fun assertFailsWith(vararg validationKeys: String, block: suspend () -> Unit) {
        val exception = try {
            block()
            null
        } catch (e: OpenApiValidationException) {
            e
        }

        assertEquals(
            validationKeys.toList(),
            exception?.report?.messages?.map { it.key } ?: emptyList<String>(),
        )
    }

    private fun stubJsonResponse(body: String, headers: Headers = headersOf()) =
        stubJsonResponse(HttpStatusCode.OK, body, headers)

    private fun stubJsonResponse(
        statusCode: HttpStatusCode,
        body: String,
        headers: Headers = headersOf(),
    ): MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { request ->
        respond(
            content = body,
            status = statusCode,
            headers =
            headers {
                append(HttpHeaders.ContentType, "application/json")
                appendAll(headers)
            },
        )
    }

    private val ref = "\$ref"
    private val testApiSpec =
        """
        {
          "openapi": "3.1.0",
          "info": { "title": "Test API", "version": "1.0.0" },
          "paths": {
            "/items": {
              "post": {
                "requestBody": {
                  "required": true,
                  "content": {
                    "application/json": {
                      "schema": {
                        "$ref": "#/components/schemas/Item"
                      }
                    }
                  }
                },
                "responses": {
                  "201": {
                    "description": "Item created",
                    "content": {
                      "application/json": {
                        "schema": {
                          "$ref": "#/components/schemas/Item"
                        }
                      }
                    }
                  }
                }
              }
            },
            "/items/{id}": {
              "get": {
                "parameters": [
                  {
                    "name": "id",
                    "in": "path",
                    "required": true,
                    "schema": {
                      "type": "string"
                    }
                  },
                  {
                    "name": "status",
                    "in": "query",
                    "required": true,
                    "schema": {
                      "type": "string",
                      "enum": ["active", "inactive"]
                    }
                  },
                  {
                    "name": "X-API-Key",
                    "in": "header",
                    "required": true,
                    "schema": {
                      "type": "string"
                    }
                  }
                ],
                "responses": {
                  "200": {
                    "description": "Item details",
                    "headers": {
                      "X-Request-Id": {
                        "required": true,
                        "schema": {
                          "type": "string"
                        }
                      }
                    },
                    "content": {
                      "application/json": {
                        "schema": {
                          "$ref": "#/components/schemas/Item"
                        }
                      }
                    }
                  }
                }
              }
            }
          },
          "components": {
            "schemas": {
              "Item": {
                "type": "object",
                "required": ["id", "status"],
                "properties": {
                  "id": {
                    "type": "integer"
                  },
                  "status": {
                    "type": "string",
                    "enum": ["active", "inactive"]
                  }
                }
              }
            }
          }
        }
        """.trimIndent()
}
