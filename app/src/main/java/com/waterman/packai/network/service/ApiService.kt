package com.waterman.packai.network.service

import com.waterman.packai.network.OcrResponse
import com.waterman.packai.network.response.ApiResponse
import com.waterman.packai.network.response.BrandListResponse
import com.waterman.packai.network.response.LoginResponse
import com.waterman.packai.network.response.PhotoResponse
import com.waterman.packai.network.response.ProductListResponse
import com.waterman.packai.network.response.SrNoDataResponse
import com.waterman.packai.utils.URLFactory
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    /*========================================================================================*/
    /*================================= Authentication ========================================*/
    @POST(URLFactory.Url.CHECK_USER_API)
    suspend fun checkUser(@Body request: MultipartBody): Response<LoginResponse>
    @POST(URLFactory.Url.API_LOGIN_WITH_FCM_ID)
    suspend fun loginWithFcmId(@Body request: MultipartBody): Response<LoginResponse>
    @POST(URLFactory.Url.API_LOG_OUT_WITH_FCM_ID)
    suspend fun logOutWithFcmId(@Body request: MultipartBody): Response<LoginResponse>
    @POST(URLFactory.Url.GET_SR_NO_DATA)
    suspend fun getSrNoData(@Body request: MultipartBody): Response<SrNoDataResponse>
    @POST(URLFactory.Url.SAVE_PACK_AI)
    suspend fun savePackAiData(@Body request: MultipartBody): Response<ApiResponse>
    @POST(URLFactory.Url.PRODUCT_LISTING)
    suspend fun productListing(@Body request: MultipartBody): Response<ProductListResponse>
    @POST(URLFactory.Url.UPLOADED_PHOTO)
    suspend fun getUploadedPhoto(@Body request: MultipartBody): Response<PhotoResponse>

    @POST(URLFactory.Url.SAVE_LIST_PHOTO)
    suspend fun saveMultiplePhoto(@Body request: MultipartBody): Response<ApiResponse>

    @POST(URLFactory.Url.DELETE_MULTIPLE_PHOTO)
    suspend fun deleteMultiplePhoto(@Body request: MultipartBody): Response<ApiResponse>

    @POST(URLFactory.Url.UPDATE_PACK_AI)
    suspend fun updatePackAIData(@Body request: MultipartBody): Response<ApiResponse>

    @POST(URLFactory.Url.GET_BRAND_LIST)
    suspend fun getBrandList(): Response<BrandListResponse>

    @POST(URLFactory.Url.GET_SR_NO_LIST)
    suspend fun getSrNoList(@Body request: MultipartBody): Response<ApiResponse>



    /*========================================================================================*/
    /*================================= Dashboard ============================================*/
    @Multipart
    @Headers("Accept: application/json")
    @POST("api/ocr")
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<com.waterman.packai.network.response.OcrResponse>

}