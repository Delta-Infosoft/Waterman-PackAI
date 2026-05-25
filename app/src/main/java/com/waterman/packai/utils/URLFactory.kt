package com.waterman.packai.utils

object URLFactory {

    object Url {
        /*========================================================================================*/
        /* ================= BASE URL ================= */
        const val baseUrl = "http://103.113.32.126/DeltaiERP/API/" /*THis is dynamic save from login page*/
        const val baseUrlEmbossTextDetaction = "https://gemini-ocr-lemon.vercel.app/"
        /*========================================================================================*/
        /* ================= AUTH ================= */
        const val GET_API_VERSION = "API_Version.aspx"
        const val CHECK_USER_API = "API_UserValidate.aspx"
        const val API_LOGIN_WITH_FCM_ID = "API_LoginWithFCMId.aspx"
        const val API_LOG_OUT_WITH_FCM_ID = "API_LogoutWithFCMId.aspx "
        const val GET_SR_NO_DATA = "API_GetSrNo.aspx"
        const val SAVE_PACK_AI = "API_SavePackAI.aspx"
        const val PRODUCT_LISTING = "API_PackAIList.aspx"
        const val UPLOADED_PHOTO = "API_ListOfPhotos.aspx"
        const val SAVE_LIST_PHOTO = "API_SaveListOfPhotos.aspx"
        const val DELETE_MULTIPLE_PHOTO = "API_DeleteListOfPhotos.aspx"

        const val UPDATE_PACK_AI = "API_UpdatePackAI.aspx"
        const val GET_BRAND_LIST = "API_GetBrand.aspx"
        const val GET_SR_NO_LIST= "API_GetSrNoList.aspx"

    }
}
