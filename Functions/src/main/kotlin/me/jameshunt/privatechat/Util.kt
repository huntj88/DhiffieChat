package me.jameshunt.privatechat

import com.amazonaws.services.lambda.runtime.Context
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.readValue
import me.jameshunt.privatechat.crypto.toIv
import javax.crypto.spec.IvParameterSpec

inline fun <reified In, Out> awsTransform(
    request: Map<String, Any?>,
    context: Context,
    handle: (In) -> Out
): GatewayResponse {
    context.logger.log(Singletons.objectMapper.writeValueAsBytes(request))

    val data = try {
        Singletons.objectMapper.readValue<In>(request["body"]!!.toString())
    } catch (e: JsonProcessingException) {
        return GatewayResponse(body = "bad request", statusCode = 400)
    }

    return try {
        val out = Singletons.objectMapper.writeValueAsString(when (val result = handle(data)) {
            is Unit -> mapOf("message" to "success")
            else -> result
        })
        GatewayResponse(body = out)
    } catch (e: Unauthorized) {
        GatewayResponse(
            body = Singletons.objectMapper.writeValueAsString(mapOf("message" to "Authentication error")),
            statusCode = 401
        )
    }
}

inline fun <reified In, Out> awsTransformAuthed(
    request: Map<String, Any?>,
    context: Context,
    handle: (In, Identity) -> Out
): GatewayResponse {
    context.logger.log(Singletons.objectMapper.writeValueAsBytes(request))

    val identity = validateAndGetIdentity(request)

    val data = try {
        Singletons.objectMapper.readValue<In>(request["body"]!!.toString())
    } catch (e: JsonProcessingException) {
        return GatewayResponse(body = "bad request", statusCode = 400)
    }

    return try {
        val out = Singletons.objectMapper.writeValueAsString(when (val result = handle(data, identity)) {
            is Unit -> mapOf("message" to "success")
            else -> result
        })
        GatewayResponse(body = out)
    } catch (e: Unauthorized) {
        GatewayResponse(
            body = Singletons.objectMapper.writeValueAsString(mapOf("message" to "Authentication error")),
            statusCode = 401
        )
    }
}

fun validateAndGetIdentity(request: Map<String, Any?>): Identity {
    val headers = request["headers"]!!.let { it as Map<String, String> }
    val hashedIdentity = headers["hashedidentity"]!!
    val userServerIv = headers["userserveriv"]!!.toIv()
    val userServerEncryptedToken = headers["userserverencryptedtoken"]!!

    return validateAndGetIdentity(
        hashedIdentity = hashedIdentity,
        iv = userServerIv,
        encryptedToken = userServerEncryptedToken
    )
}

class Unauthorized : Exception()