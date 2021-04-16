package me.jameshunt.privatechat

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import java.util.*

class GetServerPublicKey : RequestHandler<Name, Response> {
    override fun handleRequest(name: Name, context: Context): Response {
        val serverPublic = Base64.getEncoder().encodeToString(getServerKeyPair().private.encoded)
        return Response(serverPublic)
    }
}