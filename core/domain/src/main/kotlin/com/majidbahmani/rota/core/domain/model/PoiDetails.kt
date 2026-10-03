package com.majidbahmani.rota.core.domain.model

/** Category-specific information. */
sealed interface PoiDetails {

    data class Charging(
        val connectors: List<ChargingConnector>,
        val network: String?,
    ) : PoiDetails

    data class Fuel(
        val fuelTypes: Set<FuelType>,
    ) : PoiDetails

    data class Parking(
        val type: ParkingType?,
        val capacity: Int?,
    ) : PoiDetails
}
