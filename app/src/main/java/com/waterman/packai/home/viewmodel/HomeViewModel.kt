package com.waterman.packai.home.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.waterman.packai.network.ApiClient
import com.waterman.packai.network.NetworkConnectionInterceptor
import com.waterman.packai.network.request.DeletePhotoRequest
import com.waterman.packai.network.request.GetSrNoDropDownListRequest
import com.waterman.packai.network.request.GetSrNoListRequest
import com.waterman.packai.network.request.GetUploadedPhotoRequest
import com.waterman.packai.network.request.PackAiListRequest
import com.waterman.packai.network.request.SaveMultiplePhotoRequest
import com.waterman.packai.network.request.SavePackAIRequest
import com.waterman.packai.network.request.UpdatePackAIRequest
import com.waterman.packai.network.response.ApiResponse
import com.waterman.packai.network.response.BrandList
import com.waterman.packai.network.response.ProductList
import com.waterman.packai.utils.EncryptedPrefHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val prefHelper: EncryptedPrefHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _srNoState = MutableLiveData<SrNoState>(SrNoState.Idle)
    val srNoState: LiveData<SrNoState> = _srNoState
    fun getSrNoData(request: GetSrNoListRequest) {
        viewModelScope.launch {
            _srNoState.value = SrNoState.Loading
            try {
                val response = repository.getSrNoData(request)
                if (!response.isSuccessful || response.body() == null) {
                    _srNoState.value = SrNoState.Error("Server error")
                    return@launch
                }
                val body = response.body()!!
                val resultList = body.result ?: emptyList()
                when (body.status) {
                    "200" -> {
                        if (resultList.isEmpty()) {
                            _srNoState.value = SrNoState.Empty(body.message ?: "No data found")
                        } else {
                            _srNoState.value = SrNoState.Success(ArrayList(resultList))
                        }
                    }
                    "209" -> {
                        _srNoState.value = SrNoState.Empty(body.message ?: "No Record Found")
                    }
                    else -> {
                        _srNoState.value = SrNoState.Error(body.message ?: "Something went wrong")
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is NetworkConnectionInterceptor.NoInternetException -> {
                        _srNoState.value = SrNoState.Error("No Internet Connection")
                    }
                    is SocketTimeoutException -> {
                        _srNoState.value = SrNoState.Error("Connection Timeout")
                    }
                    is IOException -> {
                        _srNoState.value = SrNoState.Error("Network Error")
                    }
                    else -> {
                        _srNoState.value = SrNoState.Error(e.localizedMessage ?: "Something went wrong")
                    }
                }
            }
        }
    }
    /*===========================================================================================*/
    private val _savePackState = MutableLiveData<SavePackState>(SavePackState.Idle)
    val savePackState: LiveData<SavePackState> = _savePackState
    fun savePack(request: SavePackAIRequest) {
        viewModelScope.launch {
            _savePackState.value = SavePackState.Loading
            try {
                val response = repository.savePackAiData(request)
                if (!response.isSuccessful || response.body() == null) {
                    _savePackState.value = SavePackState.Error("Server error")
                    return@launch
                }
                val body = response.body()!!
                when (body.status) {
                    "200" -> {
                        _savePackState.value = SavePackState.Success(body.message ?: "Data saved successfully")
                    }
                    "209" -> {
                        _savePackState.value = SavePackState.Error(body.message ?: "Already submitted")
                    }
                    else -> {
                        _savePackState.value = SavePackState.Error(body.message ?: "Something went wrong")
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is NetworkConnectionInterceptor.NoInternetException -> {
                        _savePackState.value = SavePackState.Error("No Internet Connection")
                    }
                    is SocketTimeoutException -> {
                        _savePackState.value = SavePackState.Error("Connection Timeout")
                    }
                    is IOException -> {
                        _savePackState.value = SavePackState.Error("Network Error")
                    }
                    else -> {
                        _savePackState.value = SavePackState.Error(e.localizedMessage ?: "Something went wrong")
                    }
                }
            }
        }
    }
    /*===========================================================================================*/
    private val _productListState = MutableLiveData<ProductListState>(ProductListState.Idle)
    val productListState: LiveData<ProductListState> = _productListState
    fun productListingAPI(fromDate: Date, toDate:Date) {
        _productListState.value = ProductListState.Loading
        viewModelScope.launch {
            try {
                val response = repository.productListing(request = PackAiListRequest(fromDate, toDate))
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    _productListState.value = ProductListState.Error("Server error : ${response.code()}")
                    return@launch
                }
                when (body.status) {
                    "200" -> {
                        val list = when (val res = body.result) {
                            is List<*> -> {
                                res.mapNotNull { item ->
                                    (item as? LinkedTreeMap<*, *>)?.let {
                                        Gson().fromJson(
                                            Gson().toJson(it),
                                            ProductList::class.java
                                        )
                                    }
                                }
                            }
                            else -> emptyList()
                        }

                        if (list.isEmpty()) {
                            _productListState.value = ProductListState.Error(body.message ?: "No data found")
                        } else {
                            _productListState.value = ProductListState.Success(list)
                        }
                    }
                    "209" -> {
                        _productListState.value = ProductListState.Empty(body.message ?: "No data available")
                    }
                    else -> {
                        _productListState.value = ProductListState.Error(body.message ?: "Something went wrong")
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is NetworkConnectionInterceptor.NoInternetException -> {
                        _productListState.value = ProductListState.Error("No Internet Connection")
                    }
                    is SocketTimeoutException -> {
                        _productListState.value = ProductListState.Error("Connection Timeout")
                    }
                    is IOException -> {
                        _productListState.value = ProductListState.Error("Network Error")
                    }
                    else -> {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        _productListState.value = ProductListState.Error(e.localizedMessage ?: "Something went wrong")
                    }
                }
            }
        }
    }

    /*"200" -> {
        val list = body.result ?: emptyList()
        if (list.isEmpty()) {
            _productListState.value = ProductListState.Empty("No data found")
            return@launch
        }
        _productListState.value = ProductListState.Success(list)
    }*/
    /*=======================================================================================*/
    private val _photoState = MutableLiveData<GetUploadedPhotoState>(GetUploadedPhotoState.Idle)
    val photoState: LiveData<GetUploadedPhotoState> = _photoState
    fun getUploadedPhotoAPI(recordId: String,formType:String) {
        _photoState.value = GetUploadedPhotoState.Loading(formType)
        viewModelScope.launch {
            try {
                val response = repository.getUploadedPhoto(GetUploadedPhotoRequest(recordId = recordId, formName = formType))
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    _photoState.value = GetUploadedPhotoState.Error(formType, "Server error : ${response.code()}")
                    return@launch
                }
                when (body.status) {
                    "200" -> {
                        val list = body.result ?: emptyList()
                        if (list.isEmpty()) {
                            _photoState.value = GetUploadedPhotoState.Empty(formType, "No photos found")
                            return@launch
                        }
                        _photoState.value = GetUploadedPhotoState.Success(formType, list)
                    }
                    "209" -> {
                        _photoState.value = GetUploadedPhotoState.Empty(formType, body.message ?: "No photos")
                    }
                    else -> {
                        _photoState.value = GetUploadedPhotoState.Error(formType, body.message ?: "Something went wrong")
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is NetworkConnectionInterceptor.NoInternetException -> {
                        _photoState.value = GetUploadedPhotoState.Error(formType,"No Internet Connection")
                    }
                    is SocketTimeoutException -> {
                        _photoState.value = GetUploadedPhotoState.Error(formType,"Connection Timeout")
                    }
                    is IOException -> {
                        _photoState.value = GetUploadedPhotoState.Error(formType,"Network Error")
                    }
                    else -> {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        _photoState.value = GetUploadedPhotoState.Error(formType,e.localizedMessage ?: "Something went wrong")
                    }
                }
            }
        }
    }
    /*=========================================================================================*/
    private val _uploadAttachmentState = MutableLiveData<UploadAttachmentState>(UploadAttachmentState.Idle)
    val uploadAttachmentState: LiveData<UploadAttachmentState> = _uploadAttachmentState
    fun uploadAttachment(request: SaveMultiplePhotoRequest) {
        viewModelScope.launch {
            _uploadAttachmentState.value = UploadAttachmentState.Loading
            try {
                val response = repository.saveMultiplePhoto(request = request)
                if (!response.isSuccessful) {
                    _uploadAttachmentState.value = UploadAttachmentState.Error("Server error (${response.code()})")
                    return@launch
                }
                val body = response.body()
                if (body == null) {
                    _uploadAttachmentState.value = UploadAttachmentState.Error( "Empty response")
                    return@launch
                }
                when (body.status) {
                    "200" -> {
                        _uploadAttachmentState.value = UploadAttachmentState.Success( body.message ?: "Uploaded successfully")
                    }
                    "209" -> {
                        _uploadAttachmentState.value = UploadAttachmentState.Empty(body.message ?: "No Record Found")
                    }
                    else -> {
                        _uploadAttachmentState.value = UploadAttachmentState.Error(body.message ?: "Something went wrong")
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is NetworkConnectionInterceptor.NoInternetException -> {
                        _uploadAttachmentState.value = UploadAttachmentState.Error("No Internet Connection")
                    }
                    is SocketTimeoutException -> {
                        _uploadAttachmentState.value = UploadAttachmentState.Error("Connection Timeout")
                    }
                    is IOException -> {
                        _uploadAttachmentState.value = UploadAttachmentState.Error("Network Error")
                    }
                    else -> {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        _uploadAttachmentState.value = UploadAttachmentState.Error(e.localizedMessage ?: "Something went wrong")
                    }
                }
            }
        }
    }
    /*============================================================================*/
    private val _deletePhotoState = MutableLiveData<DeletePhotoState>(DeletePhotoState.Idle)
    val deletePhotoState: LiveData<DeletePhotoState> = _deletePhotoState
    fun deleteMultiplePhotoAPI(request: DeletePhotoRequest) {
        _deletePhotoState.value = DeletePhotoState.Loading
        viewModelScope.launch {
            try {
                val response = repository.deleteMultiplePhoto(request)
                val body = response.body()
                if (!response.isSuccessful || body == null) {
                    _deletePhotoState.value = DeletePhotoState.Error("Server error : ${response.code()}")
                    return@launch
                }
                when (body.status) {
                    "200" -> {
                        _deletePhotoState.value = DeletePhotoState.Success(body.message)
                    }
                    "209" -> {
                        _deletePhotoState.value = DeletePhotoState.Empty(body.message)
                    }
                    else -> {
                        _deletePhotoState.value = DeletePhotoState.Error(body.message)
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is NetworkConnectionInterceptor.NoInternetException -> {
                        _deletePhotoState.value = DeletePhotoState.Error("No Internet Connection")
                    }
                    is SocketTimeoutException -> {
                        _deletePhotoState.value = DeletePhotoState.Error("Connection Timeout")
                    }
                    is IOException -> {
                        _deletePhotoState.value = DeletePhotoState.Error("Network Error")
                    }
                    else -> {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        _deletePhotoState.value = DeletePhotoState.Error(e.localizedMessage ?: "Something went wrong")
                    }
                }
            }
        }
    }
    /*===========================================================================*/

    private val _updatePackAIState = MutableLiveData<UpdatePackAIState>(UpdatePackAIState.Idle)
    val updatePackAIState: LiveData<UpdatePackAIState> = _updatePackAIState

    fun updatePackAIData(request: UpdatePackAIRequest) {

        _updatePackAIState.value = UpdatePackAIState.Loading

        viewModelScope.launch {
            try {
                val response = repository.updatePackAIData(request)
                val body = response.body()

                // ❌ HTTP error or null body
                if (!response.isSuccessful || body == null) {
                    _updatePackAIState.value =
                        UpdatePackAIState.Error("Server error : ${response.code()}")
                    return@launch
                }

                when (body.status) {
                    "200" -> {
                        _updatePackAIState.value =
                            UpdatePackAIState.Success(body.message)
                    }

                    "209" -> {
                        _updatePackAIState.value =
                            UpdatePackAIState.Empty(body.message)
                    }

                    else -> {
                        _updatePackAIState.value =
                            UpdatePackAIState.Error(body.message)
                    }
                }

            } catch (e: Exception) {

                when (e) {
                    is NetworkConnectionInterceptor.NoInternetException -> {
                        _updatePackAIState.value =
                            UpdatePackAIState.Error("No Internet Connection")
                    }

                    is SocketTimeoutException -> {
                        _updatePackAIState.value =
                            UpdatePackAIState.Error("Connection Timeout")
                    }

                    is IOException -> {
                        _updatePackAIState.value =
                            UpdatePackAIState.Error("Network Error")
                    }

                    else -> {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        _updatePackAIState.value =
                            UpdatePackAIState.Error(e.localizedMessage ?: "Something went wrong")
                    }
                }
            }
        }
    }

    /*===========================================================================*/
    private val _brandListState = MutableLiveData<BrandListState>(BrandListState.Idle)
    val brandListState: LiveData<BrandListState> = _brandListState

    var cachedBrandList: ArrayList<BrandList>? = null

    fun getBrandList() {

        _brandListState.value = BrandListState.Loading

        viewModelScope.launch {
            try {

                val response = repository.getBrandList()
                val body = response.body()

                // ❌ HTTP error or null body
                if (!response.isSuccessful || body == null) {
                    _brandListState.value =
                        BrandListState.Error("Server error : ${response.code()}")
                    return@launch
                }

                when (body.status) {

                    "200" -> {
                        cachedBrandList = body.result
                        _brandListState.value =
                            BrandListState.Success(body.result)
                    }

                    "209" -> {
                        _brandListState.value =
                            BrandListState.Empty(body.message ?: "No data found")
                    }

                    else -> {
                        _brandListState.value =
                            BrandListState.Error(body.message ?: "Something went wrong")
                    }
                }

            } catch (e: Exception) {

                when (e) {

                    is NetworkConnectionInterceptor.NoInternetException -> {
                        _brandListState.value =
                            BrandListState.Error("No Internet Connection")
                    }

                    is SocketTimeoutException -> {
                        _brandListState.value =
                            BrandListState.Error("Connection Timeout")
                    }

                    is IOException -> {
                        _brandListState.value =
                            BrandListState.Error("Network Error")
                    }

                    else -> {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        _brandListState.value =
                            BrandListState.Error(e.localizedMessage ?: "Something went wrong")
                    }
                }
            }
        }
    }

    /*===========================================================================*/
    /*===========================================================================*/
    private val _srNoListState = MutableLiveData<SrNoListState>(SrNoListState.Idle)
    val srNoListState: LiveData<SrNoListState> = _srNoListState

    var cachedSrNoList: ApiResponse? = null

    fun getSrNoList(request: GetSrNoDropDownListRequest) {

        // ✅ Return cached data if available
        cachedSrNoList?.let {
            _srNoListState.value = SrNoListState.Success(it)
            return
        }

        _srNoListState.value = SrNoListState.Loading

        viewModelScope.launch {
            try {

                val response = repository.getSrNoList(request)
                val body = response.body()

                // ❌ HTTP error or null body
                if (!response.isSuccessful || body == null) {
                    _srNoListState.value =
                        SrNoListState.Error("Server error : ${response.code()}")
                    return@launch
                }

                when (body.status) {

                    "200" -> {

                        // ✅ Save in cache
                        cachedSrNoList = body

                        _srNoListState.value =
                            SrNoListState.Success(body)
                    }

                    "209" -> {
                        _srNoListState.value =
                            SrNoListState.Empty(body.message ?: "No data found")
                    }

                    else -> {
                        _srNoListState.value =
                            SrNoListState.Error(body.message ?: "Something went wrong")
                    }
                }

            } catch (e: Exception) {

                when (e) {

                    is NetworkConnectionInterceptor.NoInternetException -> {
                        _srNoListState.value =
                            SrNoListState.Error("No Internet Connection")
                    }

                    is SocketTimeoutException -> {
                        _srNoListState.value =
                            SrNoListState.Error("Connection Timeout")
                    }

                    is IOException -> {
                        _srNoListState.value =
                            SrNoListState.Error("Network Error")
                    }

                    else -> {
                        FirebaseCrashlytics.getInstance().recordException(e)
                        _srNoListState.value =
                            SrNoListState.Error(e.localizedMessage ?: "Something went wrong")
                    }
                }
            }
        }
    }

    /*===========================================================================*/
    private val _ocrState = MutableLiveData<OcrState>(OcrState.Idle)
    val ocrState: LiveData<OcrState> = _ocrState

    fun uploadToOcr(file: File,type:String) {
        viewModelScope.launch {
            _ocrState.value = OcrState.Loading

            try {
                val requestBody = file.asRequestBody("image/jpeg".toMediaType())
                val imagePart = MultipartBody.Part.createFormData(
                    name = "image",
                    filename = file.name,
                    body = requestBody
                )

                val response = withContext(Dispatchers.IO) {
                    ApiClient.api.uploadImage(imagePart)
                }

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.error != null) {
                        _ocrState.value = OcrState.Error(body.error, sourceType = type)
                    } else {
                        _ocrState.value = OcrState.Success(data = body, sourceType = type)
                    }
                } else {
                    _ocrState.value = OcrState.Error("Error code: ${response.code()}")
                }

            } catch (e: Exception) {
                _ocrState.value = OcrState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }
}
