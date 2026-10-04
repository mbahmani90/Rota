package com.majidbahmani.rota.core.data.remote.google

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Adds the API key and, for a key restricted to Android apps, the app's package name and
 * signing certificate. [certificateSha1] is read on first use, off the main thread.
 */
class GooglePlacesHeadersInterceptor(
    private val apiKey: String,
    private val packageName: String,
    certificateSha1: () -> String?,
) : Interceptor {

    private val certificateSha1 by lazy(certificateSha1)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-Goog-Api-Key", apiKey)
            .header("X-Android-Package", packageName)
            .apply { certificateSha1?.let { header("X-Android-Cert", it) } }
            .build()
        return chain.proceed(request)
    }
}
