package com.majidbahmani.rota.core.data.fake

import com.majidbahmani.rota.core.data.remote.google.GooglePlacesApi
import com.majidbahmani.rota.core.data.remote.google.dto.SearchNearbyRequestDto
import com.majidbahmani.rota.core.data.remote.google.dto.SearchNearbyResponseDto

/** Records requests and field masks and returns [response], or throws [error] when set. */
class FakeGooglePlacesApi(
    var response: SearchNearbyResponseDto = SearchNearbyResponseDto(),
    var error: Throwable? = null,
) : GooglePlacesApi {

    val requests = mutableListOf<SearchNearbyRequestDto>()
    val fieldMasks = mutableListOf<String>()

    override suspend fun searchNearby(request: SearchNearbyRequestDto, fieldMask: String): SearchNearbyResponseDto {
        requests += request
        fieldMasks += fieldMask
        error?.let { throw it }
        return response
    }
}
