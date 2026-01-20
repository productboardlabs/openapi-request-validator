package com.atlassian.oai.validator.ktor.client

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.SimpleRequest
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.report.SimpleValidationReportFormat
import com.atlassian.oai.validator.report.ValidationReport
import com.atlassian.oai.validator.report.ValidationReportFormat
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.request
import io.ktor.http.content.OutgoingContent

val OpenAPIValidation = createClientPlugin("OpenAPIValidation", ::OpenApiValidationConfig) {
    val config = pluginConfig
    val validator = OpenApiInteractionValidator.Builder()
        .let { config.validator(it) }
        .build()

    onResponse { response ->
        val request = response.request
        val report = validator.validate(
            SimpleRequest
                .Builder(request.method.value, request.url.encodedPath)
                .also { builder ->
                    request.url.parameters.forEach { key, values -> builder.withQueryParam(key, values) }
                    request.headers.forEach { key, values -> builder.withHeader(key, values) }
                    request.content.contentType?.let { builder.withHeader("Content-Type", it.toString()) }
                    request.content.contentLength?.let { builder.withHeader("Content-Length", it.toString()) }
                    when (val content = request.content) {
                        is OutgoingContent.NoContent -> Unit

                        is OutgoingContent.ByteArrayContent -> builder.withBody(
                            content.bytes().decodeToString(),
                        )

                        else -> error("Unhandled request content body ${content::class.simpleName}")
                    }
                }.build(),
            SimpleResponse.Builder
                .status(response.status.value)
                .also { builder ->
                    response.headers.forEach { key, values -> builder.withHeader(key, values) }
                    builder.withBody(response.bodyAsBytes())
                }.build(),
        )

        if (report.hasErrors()) throw OpenApiValidationException(report, config.reportFormat)
    }
}

class OpenApiValidationConfig {
    private var buildValidator: (OpenApiInteractionValidator.Builder) -> OpenApiInteractionValidator.Builder = { it }
    var reportFormat: ValidationReportFormat = SimpleValidationReportFormat.getInstance()

    val validator get() = buildValidator

    fun validator(build: (OpenApiInteractionValidator.Builder) -> OpenApiInteractionValidator.Builder) {
        buildValidator = build
    }
}

class OpenApiValidationException(val report: ValidationReport, format: ValidationReportFormat) :
    RuntimeException(format.apply(report))
