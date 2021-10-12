package me.jameshunt.dhiffiechat.service

import io.reactivex.rxjava3.core.Single

/**
 * overwrite as needed in test implementation to mock server
 */
open class TestLambdaApi: LambdaApi {
    override fun initSingleEndpoint(type: String): Single<ResponseMessage> {
        TODO("Not yet implemented")
    }

    override fun getUserPublicKey(
        type: String,
        body: LambdaApi.GetUserPublicKey
    ): Single<LambdaApi.GetUserPublicKeyResponse> {
        TODO("Not yet implemented")
    }

    override fun getMessageSummaries(type: String): Single<List<LambdaApi.MessageSummary>> {
        TODO("Not yet implemented")
    }

    override fun getUserRelationships(type: String): Single<LambdaApi.UserRelationships> {
        TODO("Not yet implemented")
    }

    override fun createIdentity(
        type: String,
        body: LambdaApi.CreateIdentity
    ): Single<ResponseMessage> {
        TODO("Not yet implemented")
    }

    override fun scanQR(type: String, body: LambdaApi.ScanQR): Single<ResponseMessage> {
        TODO("Not yet implemented")
    }

    override fun sendMessage(
        type: String,
        body: LambdaApi.SendMessage
    ): Single<LambdaApi.SendMessageResponse> {
        TODO("Not yet implemented")
    }

    override fun consumeMessage(
        type: String,
        body: LambdaApi.ConsumeMessage
    ): Single<LambdaApi.ConsumeMessageResponse> {
        TODO("Not yet implemented")
    }

    override fun remainingEphemeralReceiveKeys(type: String): Single<LambdaApi.RemainingEphemeralReceiveKeysResponse> {
        TODO("Not yet implemented")
    }

    override fun uploadEphemeralReceiveKeys(
        type: String,
        body: LambdaApi.UploadReceiveKeys
    ): Single<ResponseMessage> {
        TODO("Not yet implemented")
    }

    override fun getEphemeralPublicKey(
        type: String,
        body: LambdaApi.EphemeralPublicKeyRequest
    ): Single<LambdaApi.SignedKey> {
        TODO("Not yet implemented")
    }
}