package com.majidbahmani.rota.core.domain.model

/** Where POIs come from. */
enum class PoiDataSource {
    /** OpenStreetMap through the public Overpass API: free, no key. */
    OVERPASS,

    /** Google Places API (New): needs an API key. */
    GOOGLE_PLACES,
}
