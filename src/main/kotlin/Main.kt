package com.trillica

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.trillica.client.ApiKeyClient
import com.trillica.client.SearchClient
import com.trillica.models.AssetResponseDto
import okhttp3.Headers
import okhttp3.OkHttpClient

fun main(args: Array<String>) {

    val apiKey = args[0]

    val ctx = Ctx(
        ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .registerModule(JavaTimeModule()),
        "http://192.168.1.15:2283/api",
        OkHttpClient().newBuilder().addInterceptor {
            val newHeaders = Headers.Builder()
                .addAll(it.request().headers)
                .add("x-api-key", apiKey).build()
            val request = it.request().newBuilder().headers(newHeaders).build()
            it.proceed(request)
        }.build())

    val dedupers = listOf(MyOrbthumbs())

    dedupers.forEach { deduper ->
        deduper.candidates(ctx).forEach { candidate ->
            if(deduper.verify(candidate)) {
                println("Delete $candidate")
            }
        }
    }

}

class Ctx(private val objectMapper: ObjectMapper,
          private val baseUrl: String,
          private val client: OkHttpClient
) {
    val searchClient: SearchClient get() = SearchClient(objectMapper, baseUrl, client)
}

typealias Candidate = AssetResponseDto

interface Deduper {
    fun candidates(ctx: Ctx) : List<Candidate>
    fun verify(c: Candidate) : Boolean
}

class MyOrbthumbs() : Deduper {
    override fun candidates(ctx: Ctx): List<Candidate> {
        return ctx.searchClient.search(q = "my-orbthumb").data?.assets?.items!!
    }

    override fun verify(c: Candidate): Boolean {
        return true
    }
}
