package me.jameshunt.dhiffiechat.service

import io.reactivex.rxjava3.core.Single

class InitService(
    private val api: LambdaApi,
    private val userService: UserService,
    private val ephemeralKeySyncService: EphemeralKeySyncService
) {
    fun init(): Single<Unit> {
        return api.initSingleEndpoint()
            .flatMap { userService.createIdentity() }
            .flatMap { ephemeralKeySyncService.populateEphemeralReceiveKeys() }
    }
}
