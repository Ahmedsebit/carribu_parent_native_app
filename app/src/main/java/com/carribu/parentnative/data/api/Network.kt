package com.carribu.parentnative.data.api

import com.carribu.parentnative.BuildConfig
import com.carribu.parentnative.data.model.ErrorResponse
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.FromJson
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val message: String, val code: Int? = null, val cause: Throwable? = null) : ApiResult<Nothing>
}

class FlexibleDoubleAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Double? = when (reader.peek()) {
        JsonReader.Token.NULL -> reader.nextNull<Unit>().let { null }
        JsonReader.Token.NUMBER -> reader.nextDouble()
        JsonReader.Token.STRING -> reader.nextString().toDoubleOrNull()
        else -> {
            reader.skipValue()
            null
        }
    }

    @ToJson
    fun toJson(value: Double?): Double? = value
}

object NetworkFactory {
    val moshi: Moshi = Moshi.Builder()
        .add(FlexibleDoubleAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun create(tokenProvider: () -> String?): CarribuApi {
        val authInterceptor = Interceptor { chain ->
            val token = tokenProvider()
            val request = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
            }.build()
            chain.proceed(request)
        }
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(CarribuApi::class.java)
    }
}

suspend inline fun <T> apiCall(moshi: Moshi, crossinline block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (error: HttpException) {
        val body = error.response()?.errorBody()?.string()
        val parsed = body?.let {
            runCatching { moshi.adapter(ErrorResponse::class.java).fromJson(it) }.getOrNull()
        }
        ApiResult.Failure(
            message = parsed?.error ?: parsed?.details ?: "Request failed (${error.code()}).",
            code = error.code(),
            cause = error
        )
    } catch (error: IOException) {
        ApiResult.Failure("Unable to reach Carribu. Check your connection.", cause = error)
    } catch (error: JsonDataException) {
        ApiResult.Failure(
            "Carribu returned trip data this app could not read. Please retry or contact support.",
            cause = error
        )
    } catch (error: IllegalArgumentException) {
        ApiResult.Failure(error.message ?: "Invalid response from server.", cause = error)
    } catch (error: Exception) {
        if (error is CancellationException) throw error
        ApiResult.Failure(
            "Carribu could not refresh this screen. Please retry.",
            cause = error
        )
    }
