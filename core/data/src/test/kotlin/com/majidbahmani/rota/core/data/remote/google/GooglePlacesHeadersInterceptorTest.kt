package com.majidbahmani.rota.core.data.remote.google

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GooglePlacesHeadersInterceptorTest {

    /** Runs a request through the interceptor and returns what would be sent, without a network. */
    private fun sentRequest(interceptor: GooglePlacesHeadersInterceptor): Request {
        var sent: Request? = null
        val capture = Interceptor { chain ->
            sent = chain.request()
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("{}".toResponseBody())
                .build()
        }
        OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor(capture)
            .build()
            .newCall(Request.Builder().url("https://places.googleapis.com/v1/places:searchNearby").build())
            .execute()
            .close()
        return sent!!
    }

    @Test
    fun `adds the key, package and certificate`() {
        val request = sentRequest(
            GooglePlacesHeadersInterceptor("test-key", "com.majidbahmani.rota") { "ABCDEF" },
        )

        assertEquals("test-key", request.header("X-Goog-Api-Key"))
        assertEquals("com.majidbahmani.rota", request.header("X-Android-Package"))
        assertEquals("ABCDEF", request.header("X-Android-Cert"))
    }

    @Test
    fun `no certificate header when the certificate can't be read`() {
        val request = sentRequest(GooglePlacesHeadersInterceptor("test-key", "com.majidbahmani.rota") { null })

        assertNull(request.header("X-Android-Cert"))
    }

    @Test
    fun `sha1 is uppercase hex without colons`() {
        // Known SHA-1 test vector for "abc".
        assertEquals("A9993E364706816ABA3E25717850C26C9CD0D89D", "abc".toByteArray().sha1Hex())
    }
}
