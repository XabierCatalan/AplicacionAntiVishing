package com.example.aplicacionantivishing.network   // ⬅️ ajusta si tu ruta es distinta

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 *  utilidades OSINT ­– (número reportado, etc.)
 */
object OsintChecker {

    private const val TAG = "OsintChecker"

    /* -------------------------  CLIENTE HTTP GLOBAL  ------------------------ */

    /**
     *  – time-outs cortos
     *  – CookieJar para evitar que Cloudflare/ ReCAPTCHA bloquee la petición
     *  – cabecera Accept-Encoding para que Teledigo no devuelva 403 si falta
     */
    private val httpClient = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()

    /* ----------------------------  TELEDIGO  ---------------------------- */

    /**
     *  @return **true** si Teledigo muestra comentarios para `phoneNumber`
     */
    suspend fun isReportedInTeledigo(phoneNumber: String): Boolean =
        withContext(Dispatchers.IO) {

            val num    = phoneNumber.trim()
            val url    = "https://www.teledigo.com/$num"

            Log.d(TAG, "Teledigo ▸ URL  = $url")

            /* ---------------- PETICIÓN HTTP ---------------- */
            val resp   = httpClient.newCall(
                Request.Builder().url(url).get().build()
            ).execute()

            Log.d(TAG, "Teledigo ▸ HTTP ${resp.code}")

            if (!resp.isSuccessful) return@withContext false        // 4xx / 5xx

            val html   = resp.body?.string().orEmpty()
            Log.d(TAG, "Teledigo ▸ bytes = ${html.length}")

            /* ---------------- PARSEO ---------------- */
            val doc = Jsoup.parse(html)

            // 1) Localizamos el <ul class="comment-list">
            val ul = doc.selectFirst("ul.comment-list") ?: run {
                Log.d(TAG, "Teledigo ▸ NO existe <ul.comment-list>")
                return@withContext false
            }

            /* 2) ¿Tiene el formulario "comment_resp"?
                  (solo existe cuando hay comentarios de verdad)          */
            val hasForm   = ul.selectFirst("form[name=comment_resp]") != null

            /* 3) ¿Hay al menos un <li class="comment"> con texto?          */
            val realComments = ul.select("li.comment")
                .map { it.text().trim() }
                .filter { it.isNotEmpty() }

            Log.d(TAG, "Teledigo ▸ formPresent = $hasForm  |  li.comment = ${realComments.size}")

            return@withContext (hasForm && realComments.isNotEmpty())
        }
}
