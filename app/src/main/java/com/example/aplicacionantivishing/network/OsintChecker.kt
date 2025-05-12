package com.example.aplicacionantivishing.network   // ⬅️ ajusta si tu ruta es distinta

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.net.SocketTimeoutException

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
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(40, TimeUnit.SECONDS)
        .build()

    /* ----------------------------  TELEDIGO  ---------------------------- */

    /**
     *  @return **true** si Teledigo muestra comentarios para `phoneNumber`
     */
    suspend fun isReportedInTeledigo(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val num = phoneNumber.trim()
            val url = "https://www.teledigo.com/$num"
            Log.d(TAG, "Teledigo ▸ URL = $url")

            val resp = httpClient.newCall(
                Request.Builder().url(url).get().build()
            ).execute()

            Log.d(TAG, "Teledigo ▸ HTTP ${resp.code}")

            if (!resp.isSuccessful) return@withContext false

            val html = resp.body?.string().orEmpty()
            Log.d(TAG, "Teledigo ▸ bytes = ${html.length}")

            val doc = Jsoup.parse(html)
            val ul = doc.selectFirst("ul.comment-list") ?: run {
                Log.d(TAG, "Teledigo ▸ NO existe <ul.comment-list>")
                return@withContext false
            }

            val hasForm = ul.selectFirst("form[name=comment_resp]") != null
            val realComments = ul.select("li.comment")
                .map { it.text().trim() }
                .filter { it.isNotEmpty() }

            Log.d(TAG, "Teledigo ▸ formPresent = $hasForm  |  li.comment = ${realComments.size}")

            return@withContext (hasForm && realComments.isNotEmpty())

        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Teledigo ▸ Timeout: ${e.localizedMessage}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Teledigo ▸ Error: ${e.localizedMessage}")
            false
        }
    }

    suspend fun isReportedInListaSpam(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        val num = phoneNumber.trim()
        val queryUrl = "https://www.listaspam.com/busca.php?Telefono=${URLEncoder.encode(num, "UTF-8")}"
        Log.d(TAG, "ListaSpam ▸ URL = $queryUrl")

        try {
            val warm = Request.Builder()
                .url("https://www.listaspam.com/")
                .header("User-Agent", UA)
                .header("Accept", "text/html")
                .header("Accept-Language", "es-ES,es;q=0.9")
                .get()
                .build()

            httpClient.newCall(warm).execute().use { w ->
                Log.d(TAG, "ListaSpam ▸ warm-up HTTP ${w.code}")
            }

            val req = Request.Builder()
                .url(queryUrl)
                .header("User-Agent", UA)
                .header("Accept", "text/html,application/xhtml+xml")
                .header("Accept-Language", "es-ES,es;q=0.9")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                Log.d(TAG, "ListaSpam ▸ HTTP ${resp.code}")

                if (!resp.isSuccessful) return@withContext false

                val html = resp.body?.string().orEmpty()
                Log.d(TAG, "ListaSpam ▸ bytes = ${html.length}")

                val doc = Jsoup.parse(html)
                val span = doc.selectFirst("div.n_reports span.result")
                val count = span?.text()?.trim()?.toIntOrNull() ?: 0

                Log.d(TAG, "ListaSpam ▸ denuncias = $count")

                return@withContext count > 0
            }
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "ListaSpam ▸ Timeout: ${e.localizedMessage}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "ListaSpam ▸ Error: ${e.localizedMessage}")
            false
        }
    }

    private const val UA =
        "Mozilla/5.0 (Linux; Android 8.0; Pixel 2 Build/OPD3.170816.012) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.6562.1943 Mobile Safari/537.36"
}
