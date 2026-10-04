package com.majidbahmani.rota.fake

import com.majidbahmani.rota.core.domain.model.GeoPoint
import com.majidbahmani.rota.core.domain.model.Poi
import com.majidbahmani.rota.core.domain.model.PoiCategory
import com.majidbahmani.rota.core.domain.repository.PoiRepository
import kotlinx.coroutines.CompletableDeferred

/** Each request suspends until the test completes its answer, so tests control the timing. */
class FakePoiRepository : PoiRepository {

    data class Request(val center: GeoPoint, val radiusMeters: Int, val categories: Set<PoiCategory>)

    val requests = mutableListOf<Request>()
    private val answers = mutableListOf<CompletableDeferred<Result<List<Poi>>>>()

    override suspend fun getNearbyPois(
        center: GeoPoint,
        radiusMeters: Int,
        categories: Set<PoiCategory>,
    ): Result<List<Poi>> {
        requests += Request(center, radiusMeters, categories)
        val answer = CompletableDeferred<Result<List<Poi>>>()
        answers += answer
        return answer.await()
    }

    /** Answers request number [index] (0-based). */
    fun answer(index: Int, result: Result<List<Poi>>) {
        answers[index].complete(result)
    }
}
