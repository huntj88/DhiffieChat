package me.jameshunt.privatechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler


data class Name(var name: String = "")
data class Response(var message: String = "")

class HelloName : RequestHandler<Name, Response> {
    override fun handleRequest(name: Name, context: Context): Response {
        return Response("Hello ${name.name}")
    }
}