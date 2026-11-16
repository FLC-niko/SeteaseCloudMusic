package com.example.seteasecloudmusic.core.network

import com.google.gson.JsonParser
import java.io.IOException

/**
 * Structured HTTP error carrying optional Netease API error code and message parsed from the error body.
 */
class ApiHttpException(
    val httpStatusCode: Int,
    val apiErrorCode: Int? = null,
    val apiMessage: String? = null,
    message: String
) : IOException(message)

internal object HttpErrorParser {
    fun parse(
        httpStatusCode: Int,
        httpMessage: String?,
        responseBody: String?
    ): ApiHttpException {
        val statusDesc = httpMessage?.trim()?.takeIf { it.isNotEmpty() } ?: "Unknown error"
        val (apiErrorCode, apiMessage) = extractApiError(responseBody)

        val formattedMessage = when {
            apiErrorCode != null && apiMessage != null ->
                "HTTP $httpStatusCode (code=$apiErrorCode): $apiMessage"
            apiMessage != null ->
                "HTTP $httpStatusCode: $apiMessage"
            apiErrorCode != null ->
                "HTTP $httpStatusCode (code=$apiErrorCode) $statusDesc"
            else ->
                "HTTP $httpStatusCode $statusDesc"
        }

        return ApiHttpException(
            httpStatusCode = httpStatusCode,
            apiErrorCode = apiErrorCode,
            apiMessage = apiMessage,
            message = formattedMessage
        )
    }

    private fun extractApiError(responseBody: String?): Pair<Int?, String?> {
        val trimmed = responseBody?.trim()?.takeIf { it.isNotEmpty() } ?: return null to null
        return try {
            val element = JsonParser.parseString(trimmed)
            if (!element.isJsonObject) return null to null
            val obj = element.asJsonObject
            val code = obj.get("code")?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isNumber }?.asInt
            val rawMessage = sequenceOf("message", "msg")
                .mapNotNull { key ->
                    obj.get(key)?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }?.asString?.trim()
                }
                .firstOrNull { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
            code to rawMessage
        } catch (_: Exception) {
            null to null
        }
    }
}
