package com.atlassian.oai.validator.ktor.client

import com.atlassian.oai.validator.OpenApiInteractionValidator
import com.atlassian.oai.validator.model.SimpleRequest
import com.atlassian.oai.validator.model.SimpleResponse
import com.atlassian.oai.validator.report.SimpleValidationReportFormat
import com.atlassian.oai.validator.report.ValidationReport
import com.atlassian.oai.validator.report.ValidationReportFormat
import io.ktor.client.plugins.api.ClientHook
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.util.pipeline.PipelinePhase
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.readText

val OpenAPIValidation = createClientPlugin("OpenAPIValidation", ::OpenApiValidationConfig) {
    val config = pluginConfig
    val validator = OpenApiInteractionValidator.Builder()
        .apply(config.validator)
        .build()

    if (!config.disableReplayableOutgoingContentMapping) {
        on(MapOutgoingContent) { content ->
            when (content) {
                is OutgoingContent.ReadChannelContent,
                is OutgoingContent.WriteChannelContent,
                -> content.toReplayableContent()

                else -> content
            }
        }
    }

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
                        is OutgoingContent.ByteArrayContent -> builder.withBody(content.bytes())
                        is OutgoingContent.ReadChannelContent -> unsupportedOutgoingContent("ReadChannelContent")
                        is OutgoingContent.ContentWrapper -> unsupportedOutgoingContent("ContentWrapper")
                        is OutgoingContent.ProtocolUpgrade -> unsupportedOutgoingContent("ProtocolUpgrade")
                        is OutgoingContent.WriteChannelContent -> unsupportedOutgoingContent("WriteChannelContent")
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
    /**
     * Configure the builder for top level OpenApiInteractionValidator
     */
    fun validator(build: OpenApiInteractionValidator.Builder.() -> Unit) {
        configureValidator = build
    }

    /**
     * Report formatter used when formatting validation exceptions
     */
    var reportFormat: ValidationReportFormat = SimpleValidationReportFormat.getInstance()

    /**
     * By default, non-replayable OutgoingContent for request bodies are transformed into a replayable version (like ByteArrayContent).
     * Disabling this will prevent the ability to properly validate read/write channel request bodies.
     */
    var disableReplayableOutgoingContentMapping = false

    internal val validator get() = configureValidator
    private var configureValidator: OpenApiInteractionValidator.Builder.() -> Unit = {}
}

class OpenApiValidationException(val report: ValidationReport, format: ValidationReportFormat) :
    RuntimeException(format.apply(report))

private fun unsupportedOutgoingContent(type: String): Nothing = error("OutgoingContent type is not supported: $type")

private object MapOutgoingContent : ClientHook<suspend (OutgoingContent) -> OutgoingContent> {
    private val phase = PipelinePhase("OpenAPIValidatorAfterRender")

    override fun install(client: io.ktor.client.HttpClient, handler: suspend (OutgoingContent) -> OutgoingContent) {
        client.requestPipeline.insertPhaseAfter(HttpRequestPipeline.Render, phase)
        client.requestPipeline.intercept(phase) {
            val content = subject as? OutgoingContent ?: return@intercept
            proceedWith(handler(content))
        }
    }
}

private suspend fun OutgoingContent.toReplayableContent(): OutgoingContent = when (this) {
    is OutgoingContent.ReadChannelContent -> readFrom().toTextContent(this)
    is OutgoingContent.WriteChannelContent -> ByteChannel().also { writeTo(it) }.toTextContent(this)
    is OutgoingContent.ContentWrapper -> delegate().toReplayableContent()
    else -> this
}

private suspend fun ByteReadChannel.toTextContent(content: OutgoingContent) = readRemaining().readText().let {
    TextContent(it, content.contentType ?: ContentType.Any)
}
