package com.majidbahmani.rota.shared

import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.validation.HostValidator
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MyCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return MyCarAppSession()
    }
}