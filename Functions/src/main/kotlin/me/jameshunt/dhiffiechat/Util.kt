package me.jameshunt.dhiffiechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.readValue
import me.jameshunt.dhiffiechat.crypto.toIv
import java.security.MessageDigest
import java.util.*

data class GatewayResponse(
    val isBase64Encoded: Boolean = false,
    val statusCode: Int = 200,
    val headers: Map<String, String> = emptyMap(),
    val body: String
)

inline fun <reified Body, reified Params, Out> awsTransform(
    request: Map<String, Any?>,
    context: Context,
    handle: (Body, Params) -> Out
): GatewayResponse {
    // TODO: truncate large non human readable bodies when logging
    context.logger.log(Singletons.objectMapper.writeValueAsBytes(request))

    val bodyResult = getBody<Body>(request, context.logger)
    val body = bodyResult.right() ?: return bodyResult.left()

    val queryParamsResult = getQueryParams<Params>(request, context.logger)
    val queryParams = queryParamsResult.right() ?: return queryParamsResult.left()

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
    // TODO: truncate large non human readable bodies when logging
    context.logger.log(Singletons.objectMapper.writeValueAsBytes(request))

    val identity = validateAndGetIdentity(request)

    val bodyResult = getBody<Body>(request, context.logger)
    val body = bodyResult.right() ?: return bodyResult.left()

    val queryParamsResult = getQueryParams<Params>(request, context.logger)
    val queryParams = queryParamsResult.right() ?: return queryParamsResult.left()

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

sealed class Either<out A, out B> {
    class Left<A>(val value: A) : Either<A, Nothing>()
    class Right<B>(val value: B) : Either<Nothing, B>() // success

    fun right(): B? = when (this) {
        is Left -> null
        is Right -> this.value
    }

    fun left(): A = when (this) {
        is Left -> this.value
        is Right -> null ?: throw IllegalAccessError("Successful result is ready to be used")
    }
}


inline fun <reified Body> getBody(request: Map<String, Any?>, logger: LambdaLogger): Either<GatewayResponse, Body> {
    return try {
        when {
            Body::class == Unit::class -> Unit as Body
            Body::class == ByteArray::class -> Base64.getDecoder().decode(request["body"]!!.toString()) as Body
            else -> Singletons.objectMapper.readValue(request["body"]!!.toString())
        }.let { Either.Right(it) }
    } catch (e: JsonProcessingException) {
        logger.log(e.stackTraceToString())
        return Either.Left(GatewayResponse(body = "bad request: Json Body", statusCode = 400))
    } catch (e: Exception) {
        logger.log(e.stackTraceToString())
        return Either.Left(GatewayResponse(body = "bad request: Body", statusCode = 400))
    }
}

inline fun <reified Params> getQueryParams(request: Map<String, Any?>, logger: LambdaLogger): Either<GatewayResponse, Params> {
    return try {
        when (Params::class == Unit::class) {
            false -> {
                val map = request["queryStringParameters"]!! as Map<String, String>
                val paramJson = Singletons.objectMapper.writeValueAsString(map)
                println("queryParam json: $paramJson")
                Singletons.objectMapper.readValue(paramJson)
            }
            true -> Unit as Params
        }.let { Either.Right(it) }
    } catch (e: Exception) {
        logger.log(e.stackTraceToString())
        return Either.Left(GatewayResponse(body = "bad request: query params", statusCode = 400))
    }
}

fun validateAndGetIdentity(request: Map<String, Any?>): Identity {
    val headers = request["headers"]!!.let { it as Map<String, String> }
    val userId = headers["userid"]!!
    val userServerIv = headers["userserveriv"]!!.toIv()
    val userServerEncryptedToken = headers["userserverencryptedtoken"]!!

    return validateAndGetIdentity(
        userId = userId,
        iv = userServerIv,
        encryptedToken = userServerEncryptedToken
    )
}

class Unauthorized : Exception()

