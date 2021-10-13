package me.jameshunt.dhiffiechat.service

import io.reactivex.rxjava3.core.Single

fun notNeededForMock(message: String?): Nothing {
    val messageWithExtra = "Mock should not be necessary"
    throw IllegalStateException(message?.let { "$messageWithExtra: $it" } ?: messageWithExtra)
}
fun notNeededForServerMock(message: String? = null): Nothing = notNeededForMock(message)

/**
 * overwrite as needed in test implementation to mock server
 */
open class TestLambdaApi: LambdaApi {
    override fun initSingleEndpoint(type: String): Single<ResponseMessage> {
        notNeededForServerMock()
    }

    override fun getUserPublicKey(
        type: String,
        body: LambdaApi.GetUserPublicKey
    ): Single<LambdaApi.GetUserPublicKeyResponse> {
        notNeededForServerMock()
    }

    override fun getMessageSummaries(type: String): Single<List<LambdaApi.MessageSummary>> {
        notNeededForServerMock()
    }

    override fun getUserRelationships(type: String): Single<LambdaApi.UserRelationships> {
        notNeededForServerMock()
    }

    override fun createIdentity(
        type: String,
        body: LambdaApi.CreateIdentity
    ): Single<ResponseMessage> {
        notNeededForServerMock()
    }

    override fun scanQR(type: String, body: LambdaApi.ScanQR): Single<ResponseMessage> {
        notNeededForServerMock()
    }

    override fun sendMessage(
        type: String,
        body: LambdaApi.SendMessage
    ): Single<LambdaApi.SendMessageResponse> {
        notNeededForServerMock()
    }

    override fun consumeMessage(
        type: String,
        body: LambdaApi.ConsumeMessage
    ): Single<LambdaApi.ConsumeMessageResponse> {
        notNeededForServerMock()
    }

    override fun remainingEphemeralReceiveKeys(type: String): Single<LambdaApi.RemainingEphemeralReceiveKeysResponse> {
        notNeededForServerMock()
    }

    override fun uploadEphemeralReceiveKeys(
        type: String,
        body: LambdaApi.UploadReceiveKeys
    ): Single<ResponseMessage> {
        notNeededForServerMock()
    }

    override fun getEphemeralPublicKey(
        type: String,
        body: LambdaApi.EphemeralPublicKeyRequest
    ): Single<LambdaApi.SignedKey> {
        notNeededForServerMock()
    }
}