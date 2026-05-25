package com.waterman.packai.home.viewmodel

import com.waterman.packai.network.request.DeletePhotoRequest
import com.waterman.packai.network.request.GetSrNoDropDownListRequest
import com.waterman.packai.network.request.GetSrNoListRequest
import com.waterman.packai.network.request.GetUploadedPhotoRequest
import com.waterman.packai.network.request.PackAiListRequest
import com.waterman.packai.network.request.SaveMultiplePhotoRequest
import com.waterman.packai.network.request.SavePackAIRequest
import com.waterman.packai.network.request.UpdatePackAIRequest
import com.waterman.packai.network.response.ApiResponse
import com.waterman.packai.network.response.BrandListResponse
import com.waterman.packai.network.response.PhotoResponse
import com.waterman.packai.network.response.ProductListResponse
import com.waterman.packai.network.response.SrNoDataResponse
import com.waterman.packai.network.service.ApiService
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Named

class HomeRepository @Inject constructor(@Named("DEFAULT")private val apiService: ApiService) {

    suspend fun getSrNoData(request: GetSrNoListRequest): Response<SrNoDataResponse> {
        return apiService.getSrNoData(request.toMultipartBody())
    }

    suspend fun savePackAiData(request: SavePackAIRequest): Response<ApiResponse> {
        return apiService.savePackAiData(request.toMultipartBody())
    }

    suspend fun productListing(request: PackAiListRequest) : Response<ProductListResponse> {
        return apiService.productListing(request.toMultipartBody())
    }

    suspend fun getUploadedPhoto(request: GetUploadedPhotoRequest): Response<PhotoResponse> {
        return apiService.getUploadedPhoto(request.toMultipartBody())
    }

    suspend fun saveMultiplePhoto(request: SaveMultiplePhotoRequest): Response<ApiResponse> {
        return apiService.saveMultiplePhoto(request.toMultipartBody())
    }

    suspend fun deleteMultiplePhoto(request: DeletePhotoRequest): Response<ApiResponse> {
        return apiService.deleteMultiplePhoto(request.toMultipartBody())
    }

    suspend fun updatePackAIData(request: UpdatePackAIRequest): Response<ApiResponse> {
        return apiService.updatePackAIData(request.toMultipartBody())
    }

    suspend fun getBrandList(): Response<BrandListResponse> {
        return apiService.getBrandList()
    }

    suspend fun getSrNoList(request: GetSrNoDropDownListRequest): Response<ApiResponse> {
        return apiService.getSrNoList(request.toMultipartBody())
    }
}