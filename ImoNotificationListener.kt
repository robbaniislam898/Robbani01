package com.example.imoautoreply

import android.app.Notification
import android.app.RemoteInput
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ImoNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .callTimeout(25, TimeUnit.SECONDS)
        .build()
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }
    private val handled = object : LinkedHashMap<String, Long>(128, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean =
            size > 300
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!prefs.getBoolean("enabled", false)) return
        val filter = prefs.getString("package_filter", "imo").orEmpty()
        if (filter.isNotBlank() && !sbn.packageName.contains(filter, ignoreCase = true)) return

        val n = sbn.notification ?: return
        if ((n.flags and Notification.FLAG_GROUP_SUMMARY) != 0) return
        val extras = n.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val message = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        if (message.isBlank() || title.isBlank()) return
        if (message.length > 3000) return

        val actions = n.actions ?: return
        val replyAction = actions.firstOrNull { action ->
            action.remoteInputs?.any { it.allowFreeFormInput } == true
        } ?: return

        val key = "${sbn.packageName}|${sbn.key}|$message"
        synchronized(handled) {
            val now = System.currentTimeMillis()
            if (handled[key]?.let { now - it < 10 * 60 * 1000 } == true) return
            handled[key] = now
        }

        scope.launch {
            val reply = try {
                generateReply(title, message)
            } catch (_: Exception) {
                prefs.getString("fallback", "আসসালামু আলাইকুম, আপনার মেসেজ পেয়েছি। একটু পরে রিপ্লাই দিচ্ছি।").orEmpty()
            }
            if (reply.isNotBlank()) sendInlineReply(sbn, replyAction, reply)
        }
    }

    private fun generateReply(sender: String, incoming: String): String {
        val url = prefs.getString("server_url", "").orEmpty()
        if (!url.startsWith("https://")) throw IllegalStateException("HTTPS server URL required")
        val json = JSONObject()
            .put("sender", sender)
            .put("message", incoming)
            .put("instructions", prefs.getString("instructions", "").orEmpty())
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder().url(url).post(body).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("AI server error")
            val result = JSONObject(response.body?.string().orEmpty())
            return result.optString("reply").trim().take(1000)
        }
    }

    private fun sendInlineReply(
        sbn: StatusBarNotification,
        action: Notification.Action,
        text: String
    ) {
        try {
            val remoteInputs = action.remoteInputs ?: return
            val fillIn = android.content.Intent()
            val results = android.os.Bundle()
            remoteInputs.filter { it.allowFreeFormInput }.forEach {
                results.putCharSequence(it.resultKey, text)
            }
            RemoteInput.addResultsToIntent(remoteInputs, fillIn, results)
            action.actionIntent.send(this, 0, fillIn)
        } catch (_: Exception) {
            // Some apps/notification versions do not permit programmatic inline reply.
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
