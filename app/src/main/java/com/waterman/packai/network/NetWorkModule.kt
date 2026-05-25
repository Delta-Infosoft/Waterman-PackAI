package com.waterman.packai.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.waterman.packai.network.service.ApiService
import com.waterman.packai.utils.URLFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetWorkModule {

    /* -------------------------------- */
    /* INTERCEPTORS                     */
    /* -------------------------------- */

    @Singleton
    @Provides
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

    @Singleton
    @Provides
    fun provideNetworkConnectionInterceptor(
        @ApplicationContext context: Context
    ): NetworkConnectionInterceptor =
        NetworkConnectionInterceptor(context)

    /* -------------------------------- */
    /* OKHTTP                           */
    /* -------------------------------- */

    @Singleton
    @Provides
    fun provideGson(): Gson =
        GsonBuilder()
            .registerTypeHierarchyAdapter(
                List::class.java,
                SafeListDeserializer<Any>()
            )
            .create()

    @Singleton
    @Provides
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        networkConnectionInterceptor: NetworkConnectionInterceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(networkConnectionInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HtmlStripInterceptor()) // ⭐ ADD HERE
            .addInterceptor(loggingInterceptor)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

    /* -------------------------------- */
    /* RETROFIT (DEFAULT BASE URL)      */
    /* -------------------------------- */

    @Singleton
    @Provides
    @Named("DEFAULT")
    fun provideDefaultRetrofit(
        okHttpClient: OkHttpClient, gson: Gson
    ): Retrofit =
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(URLFactory.Url.baseUrl)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    /* -------------------------------- */
    /* RETROFIT (WATERMAN BASE URL)     */
    /* -------------------------------- */

    @Singleton
    @Provides
    @Named("WATERMAN")
    fun provideWatermanRetrofit(
        okHttpClient: OkHttpClient,gson: Gson
    ): Retrofit =
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(URLFactory.Url.baseUrlEmbossTextDetaction)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    /* -------------------------------- */
    /* API SERVICES                     */
    /* -------------------------------- */

    @Singleton
    @Provides
    @Named("DEFAULT")
    fun provideDefaultApiService(
        @Named("DEFAULT") retrofit: Retrofit
    ): ApiService =
        retrofit.create(ApiService::class.java)

    @Singleton
    @Provides
    @Named("WATERMAN")
    fun provideWatermanApiService(
        @Named("WATERMAN") retrofit: Retrofit
    ): ApiService =
        retrofit.create(ApiService::class.java)
}