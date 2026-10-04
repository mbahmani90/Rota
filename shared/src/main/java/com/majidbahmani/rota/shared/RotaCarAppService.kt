package com.majidbahmani.rota.shared

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import com.majidbahmani.rota.core.domain.usecase.GetNearbyPoisUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Entry point for Android Auto and AAOS. Hilt can inject Services but not the Car App
 * Library's Session or Screens, so dependencies are injected here and passed down.
 */
@AndroidEntryPoint
class RotaCarAppService : CarAppService() {

    @Inject
    lateinit var getNearbyPois: GetNearbyPoisUseCase

    override fun createHostValidator(): HostValidator =
        HostValidator.ALLOW_ALL_HOSTS_VALIDATOR

    override fun onCreateSession(): Session = RotaSession(getNearbyPois)
}
