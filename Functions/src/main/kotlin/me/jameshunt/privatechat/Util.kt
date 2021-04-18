package me.jameshunt.privatechat

import com.amazonaws.services.lambda.runtime.Context
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()

inline fun <reified In> awsTransformUnit(request: Map<String, Any?>, context: Context, handle: (In) -> Unit): GatewayResponse {
    return awsTransform(request, context, handle)
}

inline fun <reified In, Out> awsTransform(request: Map<String, Any?>, context: Context, handle: (In) -> Out): GatewayResponse {
    context.logger.log(objectMapper.writeValueAsBytes(request))
    val data = try {
        objectMapper.readValue<In>(request["body"]!!.toString())
    } catch (e: JsonProcessingException) {
        return GatewayResponse(body = "bad request", statusCode = 400)
    }

    return try {
        val out = objectMapper.writeValueAsString(when (val result = handle(data)) {
            is Unit -> mapOf("message" to "success")
            else -> result
        })
        GatewayResponse(body = out)
    } catch (e: Unauthorized) {
        GatewayResponse(
            body = objectMapper.writeValueAsString(mapOf("message" to "Authentication error")),
            statusCode = 401
        )
    }
}

class Unauthorized : Exception()