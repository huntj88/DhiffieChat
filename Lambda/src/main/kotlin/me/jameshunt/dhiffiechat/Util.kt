package me.jameshunt.dhiffiechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*

data class GatewayResponse(
    val isBase64Encoded: Boolean = false,
    val statusCode: Int = 200,
    val headers: Map<String, String> = emptyMap(),
    val body: String
)

inline fun <reified Body, Out> awsTransform(
    request: Map<String, Any?>,
    context: Context,
    handle: (Body) -> Out
): GatewayResponse {
    context.logger.log(Singletons.objectMapper.writeValueAsBytes(request))

    val bodyResult = getBody<Body>(request, context.logger)
    val body = bodyResult.right() ?: return bodyResult.left()

    return try {
        val out = Singletons.objectMapper.writeValueAsString(
            when (val result = handle(body)) {
                is Unit -> mapOf("message" to "success")
                else -> result
            }
        )
        GatewayResponse(body = out)
    } catch (e: HandledExceptions.Unauthorized) {
        e.toResponse()
    }
}

inline fun <reified Body, Out> awsTransformAuthed(
    request: Map<String, Any?>,
    context: Context,
    handle: (Body, Identity) -> Out
): GatewayResponse {
    context.logger.log(Singletons.objectMapper.writeValueAsBytes(request))

    val identity = validateAndGetIdentity(request)

    val bodyResult = getBody<Body>(request, context.logger)
    val body = bodyResult.right() ?: return bodyResult.left()

    return try {
        val out = Singletons.objectMapper.writeValueAsString(
            when (val result = handle(body, identity)) {
                is Unit -> mapOf("message" to "success")
                else -> result
            }
        )
        GatewayResponse(body = out)
    } catch (e: HandledExceptions.Unauthorized) {
        e.toResponse()
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
    val token = headers["dhiffie_token"]!!
    val signature = headers["dhiffie_signature"]!!

    return validateAndGetIdentity(
        userId = userId,
        token = token,
        signature = signature
    )
}

sealed class HandledExceptions: Exception() {
    class Unauthorized : HandledExceptions()
    class NotFound : HandledExceptions()
    class Gone: HandledExceptions()

    fun toResponse(): GatewayResponse {
        return when (this) {
            is Unauthorized -> GatewayResponse(
                body = Singletons.objectMapper.writeValueAsString(mapOf("message" to "Authentication error")),
                statusCode = 401
            )
            is NotFound -> GatewayResponse(
                body = Singletons.objectMapper.writeValueAsString(mapOf("message" to "Resource not found")),
                statusCode = 404
            )
            is Gone -> GatewayResponse(
                body = Singletons.objectMapper.writeValueAsString(mapOf("message" to "Resource gone")),
                statusCode = 410
            )
        }
    }
}

fun Instant.format(): String {
    return DateTimeFormatter.ISO_INSTANT.format(this)
}
