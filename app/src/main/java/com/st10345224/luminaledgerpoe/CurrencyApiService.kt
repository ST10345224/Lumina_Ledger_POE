package com.st10345224.luminaledgerpoe

import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

// Data class to parse the API response
data class ExchangeRatesResponse(
    val result: String,
    val documentation: String,
    @SerializedName("time_last_update_unix") val timeLastUpdateUnix: Long,
    @SerializedName("time_last_update_utc") val timeLastUpdateUtc: String,
    @SerializedName("time_next_update_unix") val timeNextUpdateUnix: Long,
    @SerializedName("time_next_update_utc") val timeNextUpdateUtc: String,
    @SerializedName("base_code") val baseCode: String,
    @SerializedName("conversion_rates") val conversionRates: Map<String, Double>
)

object CurrencyApiService {

    private const val API_KEY = "f62c1061193998d0fc6580ea"
    private const val BASE_URL = "https://v6.exchangerate-api.com/v6/"
    private val client = OkHttpClient()
    private val gson = Gson()

    /**
     * Fetches the latest conversion rates relative to a given base currency.
     * @param baseCurrency The three-letter currency code (e.g., "ZAR", "USD").
     * @return A Map of currency code to its conversion rate, or null if fetching fails.
     */
    suspend fun getLatestConversionRates(baseCurrency: String): Map<String, Double>? {
        return withContext(Dispatchers.IO) {
            val url = "${BASE_URL}${API_KEY}/latest/${baseCurrency}"
            val request = Request.Builder().url(url).build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonString = response.body?.string()
                    val apiResponse = gson.fromJson(jsonString, ExchangeRatesResponse::class.java)
                    if (apiResponse.result == "success") {
                        apiResponse.conversionRates
                    } else {
                        println("ExchangeRate-API Error: ${apiResponse.result}")
                        null
                    }
                } else {
                    println("HTTP Error: ${response.code} ${response.message}")
                    null
                }
            } catch (e: IOException) {
                println("Network/IO Error fetching rates: ${e.message}")
                null
            } catch (e: Exception) {
                println("JSON Parsing Error fetching rates: ${e.message}")
                null
            }
        }
    }
}
