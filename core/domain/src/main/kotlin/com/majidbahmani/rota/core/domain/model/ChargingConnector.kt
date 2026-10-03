package com.majidbahmani.rota.core.domain.model

data class ChargingConnector(
    val type: ConnectorType,
    /** Null when the source only says the connector exists. */
    val count: Int?,
    val maxPowerKw: Double?,
)
