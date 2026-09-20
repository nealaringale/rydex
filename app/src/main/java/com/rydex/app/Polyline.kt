package com.rydex.app

import com.google.android.gms.maps.model.LatLng

object PolylineDecoder {
    fun decode(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            val latChunk = decodeChunk(encoded, index)
            lat += latChunk.value
            index = latChunk.nextIndex

            val lngChunk = decodeChunk(encoded, index)
            lng += lngChunk.value
            index = lngChunk.nextIndex

            poly += LatLng(lat / 1e5, lng / 1e5)
        }
        return poly
    }

    private data class Chunk(val value: Int, val nextIndex: Int)

    private fun decodeChunk(encoded: String, start: Int): Chunk {
        var index = start
        var result = 0
        var shift = 0
        var byte: Int

        do {
            if (index >= encoded.length) return Chunk(0, index)
            byte = encoded[index++].code - 63
            result = result or ((byte and 0x1f) shl shift)
            shift += 5
        } while (byte >= 0x20)

        val decoded = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
        return Chunk(decoded, index)
    }
}
