package me.jameshunt.privatechat

import com.amazonaws.services.lambda.runtime.Context
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.readValue
import me.jameshunt.privatechat.crypto.toIv

inline fun <reified Body, reified Params, Out> awsTransform(
    request: Map<String, Any?>,
    context: Context,
    handle: (Body, Params) -> Out
): GatewayResponse {
    context.logger.log(Singletons.objectMapper.writeValueAsBytes(request))

    val body: Body = try {
        getBody(request)
    } catch (e: JsonProcessingException) {
        e.printStackTrace()
        return GatewayResponse(body = "bad request", statusCode = 400)
    }

    val queryParams: Params = try {
        getQueryParams(request)
    } catch (e: JsonProcessingException) {
        e.printStackTrace()
        return GatewayResponse(body = "bad request", statusCode = 400)
    }

    return try {
        val out = Singletons.objectMapper.writeValueAsString(
            when (val result = handle(body, queryParams)) {
                is Unit -> mapOf("message" to "success")
                else -> result
            }
        )
        GatewayResponse(body = out)
    } catch (e: Unauthorized) {
        GatewayResponse(
            body = Singletons.objectMapper.writeValueAsString(mapOf("message" to "Authentication error")),
            statusCode = 401
        )
    }
}

inline fun <reified Body, reified Params, Out> awsTransformAuthed(
    request: Map<String, Any?>,
    context: Context,
    handle: (Body, Params, Identity) -> Out
): GatewayResponse {
    context.logger.log(Singletons.objectMapper.writeValueAsBytes(request))

    val identity = validateAndGetIdentity(request)

    val body: Body = try {
        getBody(request)
    } catch (e: JsonProcessingException) {
        e.printStackTrace()
        return GatewayResponse(body = "bad request", statusCode = 400)
    }

    val queryParams: Params = try {
        getQueryParams(request)
    } catch (e: JsonProcessingException) {
        e.printStackTrace()
        return GatewayResponse(body = "bad request", statusCode = 400)
    }

    return try {
        val out = Singletons.objectMapper.writeValueAsString(
            when (val result = handle(body, queryParams, identity)) {
                is Unit -> mapOf("message" to "success")
                else -> result
            }
        )
        GatewayResponse(body = out)
    } catch (e: Unauthorized) {
        GatewayResponse(
            body = Singletons.objectMapper.writeValueAsString(mapOf("message" to "Authentication error")),
            statusCode = 401
        )
    }
}

inline fun <reified Body> getBody(request: Map<String, Any?>): Body {
    return when (Body::class == Unit::class) {
        false -> Singletons.objectMapper.readValue(request["body"]!!.toString())
        true -> Unit as Body
    }
}

inline fun <reified Params> getQueryParams(request: Map<String, Any?>): Params {
    return when (Params::class == Unit::class) {
        false -> {
            val map = request["queryStringParameters"]!! as Map<String, String>
            println("queryParam map: $map")
            val paramJson = Singletons.objectMapper.writeValueAsString(map)
            println("queryParam json: $paramJson")
            Singletons.objectMapper.readValue(paramJson)
        }
        true -> Unit as Params
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