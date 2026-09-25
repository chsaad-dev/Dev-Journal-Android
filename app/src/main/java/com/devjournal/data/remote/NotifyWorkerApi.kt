package com.devjournal.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotifyWorkerApi @Inject constructor(
    private val client: OkHttpClient
) {
    companion object {
        private const val WORKER_URL = "https://blog-notify-worker.yourname.workers.dev"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun sendNotification(
        type: String,
        targetUid: String,
        title: String,
        body: String,
        data: Map<String, String>? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("type", type)
                put("targetUid", targetUid)
                put("title", title)
                put("body", body)
                if (!data.isNullOrEmpty()) {
                    val dataJson = JSONObject()
                    data.forEach { (key, value) ->
                        dataJson.put(key, value)
                    }
                    put("data", dataJson)
                }
            }.toString()

            val requestBody = jsonPayload.toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(WORKER_URL)
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(
                        IOException("Notification dispatch failed with HTTP ${response.code}: ${response.message}")
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
