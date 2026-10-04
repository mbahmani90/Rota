package com.majidbahmani.rota.core.data.remote.google

import com.majidbahmani.rota.core.data.remote.google.dto.SearchNearbyRequestDto
import com.majidbahmani.rota.core.data.remote.google.dto.SearchNearbyResponseDto
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/** Places API (New). The API key and Android app headers are added by an interceptor. */
interface GooglePlacesApi {

    /** Up to 20 places; [fieldMask] selects the returned fields and so the price tier. */
    @POST("v1/places:searchNearby")
    suspend fun searchNearby(
        @Body request: SearchNearbyRequestDto,
        @Header("X-Goog-FieldMask") fieldMask: String = PRO_FIELD_MASK,
    ): SearchNearbyResponseDto

    companion object {
        const val BASE_URL = "https://places.googleapis.com/"

        /** Pro tier only: adding an Enterprise or Atmosphere field changes the price of every request. */
        const val PRO_FIELD_MASK =
            "places.id,places.displayName,places.formattedAddress,places.location," +
                "places.primaryType,places.types"

        const val MAX_RESULT_COUNT = 20
        const val RANK_BY_DISTANCE = "DISTANCE"
    }
}
