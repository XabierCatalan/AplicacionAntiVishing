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
        .callTimeout(20, TimeUnit.SECONDS)
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

    suspend fun isReportedInListaSpam(phoneNumber: String): Boolean =
        withContext(Dispatchers.IO) {

            val TAG = "OsintChecker"
            val num = phoneNumber.trim()                         // sin +34, solo dígitos

            /* ---------- 1) WARM-UP (cookies Cloudflare) ------------------- */
            val warm = Request.Builder()
                .url("https://www.listaspam.com/")
                .header("User-Agent", UA)
                .header("Accept", "text/html")
                .header("Accept-Language", "es-ES,es;q=0.9")
                .get()
                .build()

            try {
                httpClient.newCall(warm).execute().use { w ->
                    Log.d(TAG, "ListaSpam ▸ warm-up HTTP ${w.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "ListaSpam ▸ warm-up error: ${e.localizedMessage}")
                return@withContext false
            }

            /* ---------- 2) PETICIÓN REAL ---------------------------------- */
            val queryUrl =
                "https://www.listaspam.com/busca.php?Telefono=${URLEncoder.encode(num, "UTF-8")}"

            Log.d(TAG, "ListaSpam ▸ URL = $queryUrl")

            val req = Request.Builder()
                .url(queryUrl)
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "es-ES,es;q=0.9")
                .header("Referer", "https://www.listaspam.com/")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            return@withContext try {
                httpClient.newCall(req).execute().use { resp ->
                    Log.d(TAG, "ListaSpam ▸ HTTP ${resp.code}")

                    if (resp.code != 200) return@withContext false

                    val html = resp.body?.string().orEmpty()
                    Log.d(TAG, "ListaSpam ▸ bytes = ${html.length}")

                    /* 3) Parser: <div class="n_reports"><span class="result">N</span> */
                    val doc   = Jsoup.parse(html)
                    val span  = doc.selectFirst("div.n_reports span.result")
                    val count = span?.text()?.trim()?.toIntOrNull() ?: 0

                    Log.d(TAG, "ListaSpam ▸ denuncias = $count")

                    count > 0
                }
            } catch (e: Exception) {
                Log.e(TAG, "ListaSpam ▸ error: ${e.localizedMessage}")
                false
            }
        }
    private const val UA =
        "Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.6562.1943 Mobile Safari/537.36"
}
