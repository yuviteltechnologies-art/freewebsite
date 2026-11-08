package com.sheetstore.admin.network

import com.sheetstore.admin.models.ApiResponse
import com.sheetstore.admin.models.DashboardResponse
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @GET
    fun getAdminDashboard(
        @Url fullUrl: String
    ): Call<DashboardResponse>

    @POST
    fun updateSettings(
        @Url fullUrl: String,
        @Body body: Map<String, String>
    ): Call<ApiResponse>

    @POST
    fun updateProduct(
        @Url fullUrl: String,
        @Body body: Map<String, String>
    ): Call<ApiResponse>

    @POST
    fun addProduct(
        @Url fullUrl: String,
        @Body body: Map<String, String>
    ): Call<ApiResponse>

    @GET
    fun approveOrder(
        @Url fullUrl: String,
        @Query("action") action: String = "approveOrder",
        @Query("order_id") orderId: String = "",
        @Query("utr") utr: String = ""
    ): Call<ApiResponse>

    @GET
    fun approveOrderByUtr(
        @Url fullUrl: String,
        @Query("action") action: String = "approveOrder",
        @Query("utr") utr: String
    ): Call<ApiResponse>

    @GET
    fun login(
        @Url fullUrl: String,
        @Query("action") action: String = "login",
        @Query("email") email: String,
        @Query("password") pass: String
    ): Call<com.sheetstore.admin.models.LoginResponse>

    @GET
    fun sendBankNotification(
        @Url fullUrl: String,
        @Query("action") action: String = "bankNotification",
        @Query("raw_text") rawText: String,
        @Query("utr") utr: String,
        @Query("amount") amount: String
    ): Call<ApiResponse>
}
