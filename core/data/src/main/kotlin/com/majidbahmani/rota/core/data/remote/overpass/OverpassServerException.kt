package com.majidbahmani.rota.core.data.remote.overpass

/** Overpass answered, but the query failed on the server (e.g. timeout); reported in `remark`. */
class OverpassServerException(remark: String) : Exception(remark)
