package com.atlassian.oai.validator.ktor.client

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
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headers
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.readText
import io.ktor.utils.io.writeStringUtf8
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import kotlin.collections.emptyList
import kotlin.test.Test

class OpenAPIValidationTest {
    val givenValidBody = itemJson("active")
    val givenValidHeaders = headersOf("X-Request-Id" to listOf("123"))

    @Test
    fun `validates successful request`() = runTest {
        val client = validatingHttpClient(stubJsonResponse(HttpStatusCode.Created, givenValidBody)) {
            validator { withInlineApiSpecification(testApiSpec) }
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
            validator { withInlineApiSpecification(testApiSpec) }
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
            validator { withInlineApiSpecification(testApiSpec) }
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
            validator { withInlineApiSpecification(testApiSpec) }
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
            validator { withInlineApiSpecification(testApiSpec) }
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
            validator { withInlineApiSpecification(testApiSpec) }
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
            validator { withInlineApiSpecification(testApiSpec) }
        }

        assertFailsWith("validation.request.contentType.notAllowed") {
            client.post("/items") {
                header(HttpHeaders.ContentType, "text/plain")
                setBody(givenValidBody)
            }
        }
    }

    @Test
    fun `supports validating read channel request bodies`() = runTest {
        val client = validatingHttpClient(stubJsonResponse(HttpStatusCode.Created, givenValidBody)) {
            validator { withInlineApiSpecification(testApiSpec) }
        }

        client.post("/items") {
            header(HttpHeaders.ContentType, "application/json")
            setBody(
                ByteChannel(autoFlush = true).apply {
                    writeStringUtf8(givenValidBody)
                    close()
                },
            )
        }
    }

    @Test
    fun `supports validating write channel request bodies`() = runTest {
        val client = validatingHttpClient(stubJsonResponse(HttpStatusCode.Created, givenValidBody)) {
            validator { withInlineApiSpecification(testApiSpec) }
        }

        client.post("/items") {
            setBody(object : OutgoingContent.WriteChannelContent() {
                override val contentType: ContentType = ContentType.Application.Json
                override suspend fun writeTo(channel: ByteWriteChannel) {
                    channel.writeStringUtf8(givenValidBody)
                    channel.flushAndClose()
                }
            })
        }
    }

    @Test
    fun `can disable supports content mapping for request bodies`() = runTest {
        val client = validatingHttpClient(stubJsonResponse(HttpStatusCode.Created, givenValidBody)) {
            validator { withInlineApiSpecification(testApiSpec) }
            disableReplayableOutgoingContentMapping = true
        }

        val exception = assertThrows<IllegalStateException> {
            client.post("/items") {
                header(HttpHeaders.ContentType, "application/json")
                setBody(
                    ByteChannel(autoFlush = true).apply {
                        writeStringUtf8(givenValidBody)
                        close()
                    },
                )
            }
        }
        assertEquals("OutgoingContent type is not supported: ReadChannelContent", exception.message)
    }

    private fun itemJson(status: String) = """
        {
          "id": 123,
          "status": "$status"
        }
    """.trimIndent()

    private fun validatingHttpClient(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
        configure: OpenApiValidationConfig.() -> Unit,
    ) = HttpClient(MockEngine(handler)) {
        install(OpenAPIValidation) {
            configure()
        }
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
    ): suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData = { request ->
        suspend fun OutgoingContent.consume() {
            when (this) {
                is OutgoingContent.ReadChannelContent -> readFrom().readRemaining().readText()
                is OutgoingContent.WriteChannelContent -> ByteChannel().also { writeTo(it) }.readRemaining().readText()
                is OutgoingContent.ContentWrapper -> delegate().consume()
                else -> Unit
            }
        }

        request.body.consume()

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
