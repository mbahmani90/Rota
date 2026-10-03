package com.majidbahmani.rota.core.data.fake

import com.majidbahmani.rota.core.data.remote.OverpassApi
import com.majidbahmani.rota.core.data.remote.dto.OverpassResponseDto

/** Records queries and returns [response], or throws [error] when set. */
class FakeOverpassApi(
    var response: OverpassResponseDto = OverpassResponseDto(),
    var error: Throwable? = null,
) : OverpassApi {

    val queries = mutableListOf<String>()

    override suspend fun interpreter(query: String): OverpassResponseDto {
        queries += query
        error?.let { throw it }
        return response
    }
}
