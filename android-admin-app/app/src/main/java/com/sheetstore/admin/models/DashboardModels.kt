package com.sheetstore.admin.models

import com.google.gson.annotations.SerializedName

data class DashboardResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("spreadsheet_url") val spreadsheetUrl: String?,
    @SerializedName("stats") val stats: DashboardStats?,
    @SerializedName("orders") val orders: List<Order>?,
    @SerializedName("settings") val settings: Map<String, String>?,
    @SerializedName("products") val products: List<Product>?
)

data class DashboardStats(
    @SerializedName("totalRevenue") val totalRevenue: Double = 0.0,
    @SerializedName("totalOrders") val totalOrders: Int = 0,
    @SerializedName("paidOrders") val paidOrders: Int = 0,
    @SerializedName("pendingOrders") val pendingOrders: Int = 0,
    @SerializedName("todayRevenue") val todayRevenue: Double = 0.0,
    @SerializedName("todayOrders") val todayOrders: Int = 0
)

data class Order(
    @SerializedName("order_id") val orderId: String,
    @SerializedName("date") val date: String,
    @SerializedName("customer_name") val customerName: String,
    @SerializedName("customer_phone") val customerPhone: String,
    @SerializedName("product") val product: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("utr") val utr: String,
    @SerializedName("status") var status: String,
    @SerializedName("verified") var verified: Boolean
)

data class Product(
    @SerializedName("id") val id: String,
    @SerializedName("title") var title: String,
    @SerializedName("category") var category: String,
    @SerializedName("badge") var badge: String?,
    @SerializedName("price_original") var priceOriginal: String,
    @SerializedName("price_selling") var priceSelling: String,
    @SerializedName("thumbnail") var thumbnail: String?,
    @SerializedName("description") var description: String?,
    @SerializedName("features") var features: String?,
    @SerializedName("is_active") var isActive: String?
)

data class ApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("error") val error: String?,
    @SerializedName("url") val url: String? = null,
    @SerializedName("fileId") val fileId: String? = null
)

data class AuthUser(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("email") val email: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("github_repo_url") val githubRepoUrl: String?,
    @SerializedName("website_live_url") val websiteLiveUrl: String?,
    @SerializedName("sheet_url") val sheetUrl: String?,
    @SerializedName("onboarding_step") val onboardingStep: Int?
)

data class LoginResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("user") val user: AuthUser?,
    @SerializedName("error") val error: String?
)

