package com.majidbahmani.rota.core.data.remote.overpass

import com.majidbahmani.rota.core.data.remote.overpass.dto.OverpassResponseDto
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface OverpassApi {

    /**
     * Runs an Overpass QL [query] (e.g. `[out:json];nwr["amenity"="fuel"](around:…);out center tags;`).
     * POST form, so long queries don't hit URL length limits. Not paginated.
     */
    @FormUrlEncoded
    @POST("interpreter")
    suspend fun interpreter(@Field("data") query: String): OverpassResponseDto

    companion object {
        const val BASE_URL = "https://overpass-api.de/api/"

        /** Server-side query limit; OkHttp's read timeout must be longer (see NetworkModule). */
        const val QUERY_TIMEOUT_SECONDS = 25
    }
}
