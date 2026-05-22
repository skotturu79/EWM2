package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ---------------------------------------------------------------------------
// SAP EWM OData API Service (Retrofit)
//
// This interface mirrors SAP EWM OData v4 endpoints commonly available on
// SAP S/4HANA Cloud or on-premise systems with EWM activated.
//
// To activate real API calls:
// 1. Set BASE_URL to your SAP system's host
// 2. Replace Basic auth header with your authentication mechanism
//    (OAuth2 via SAP BTP Connectivity Service is recommended for cloud)
// 3. Remove MockEwmDataSource usage in EwmRepository and switch to this service
// ---------------------------------------------------------------------------

private const val BASE_URL = "https://your-sap-system.example.com/"

// --- OData Response Wrappers ---

@JsonClass(generateAdapter = true)
data class ODataListResponse<T>(
    @Json(name = "value") val value: List<T>
)

// --- Dock Door DTOs ---

@JsonClass(generateAdapter = true)
data class DockDoorDto(
    @Json(name = "DockDoorId")        val id: String,
    @Json(name = "DockDoorName")      val name: String,
    @Json(name = "DockDoorStatus")    val status: String,       // "AVAILABLE" | "DOCKED" | "LOADING" | "BLOCKED"
    @Json(name = "CarrierName")       val carrier: String = "",
    @Json(name = "LicensePlate")      val licensePlate: String = "",
    @Json(name = "LoadingProgress")   val progress: Int = 0,
    @Json(name = "TotalPallets")      val totalPallets: Int = 0,
    @Json(name = "LoadedPallets")     val loadedPallets: Int = 0,
    @Json(name = "IsLocked")          val lockState: Boolean = true,
    @Json(name = "IsTempControlled")  val temperatureControl: Boolean = false,
    @Json(name = "TargetTempF")       val temperatureF: Double = 68.0,
    @Json(name = "SecuritySeal")      val securitySeal: String = "",
    @Json(name = "ArrivalTime")       val arrivalTime: String = "",
    @Json(name = "Priority")          val priority: String = "Medium"
)

// --- Inventory DTOs ---

@JsonClass(generateAdapter = true)
data class InventoryItemDto(
    @Json(name = "ProductCode")       val sku: String,
    @Json(name = "ProductName")       val name: String,
    @Json(name = "BinLocation")       val binLocation: String,
    @Json(name = "StockQuantity")     val quantity: Int,
    @Json(name = "ReorderPoint")      val reorderPoint: Int,
    @Json(name = "UnitOfMeasure")     val unit: String = "PC",
    @Json(name = "ProductCategory")   val category: String = "Standard",
    @Json(name = "HazardClass")       val safetyClass: String = "Non-Haz",
    @Json(name = "WeightLb")          val weightLb: Double = 0.0
)

// --- Zone DTOs ---

@JsonClass(generateAdapter = true)
data class WarehouseZoneDto(
    @Json(name = "ZoneName")          val name: String,
    @Json(name = "OccupiedBins")      val occupied: Int,
    @Json(name = "TotalBins")         val total: Int,
    @Json(name = "FillPercentage")    val fillPercentage: Int
)

// --- Retrofit Interface ---

interface EwmApiService {

    /**
     * GET /sap/opu/odata4/sap/ewm_dockdoor_srv/srvd/sap/dock_door/0001/DockDoor
     * Returns all dock doors for the warehouse number.
     */
    @GET("sap/opu/odata4/sap/ewm_dockdoor_srv/srvd/sap/dock_door/0001/DockDoor")
    suspend fun getDockDoors(
        @Header("Authorization") authorization: String,
        @Query("\$format") format: String = "json",
        @Query("WarehouseNumber") warehouseNumber: String = "EWM1"
    ): ODataListResponse<DockDoorDto>

    /**
     * GET /sap/opu/odata4/sap/ewm_stock_srv/srvd/sap/warehouse_stock/0001/WarehouseStock
     * Returns warehouse stock / inventory items.
     */
    @GET("sap/opu/odata4/sap/ewm_stock_srv/srvd/sap/warehouse_stock/0001/WarehouseStock")
    suspend fun getInventory(
        @Header("Authorization") authorization: String,
        @Query("\$format") format: String = "json",
        @Query("WarehouseNumber") warehouseNumber: String = "EWM1"
    ): ODataListResponse<InventoryItemDto>

    /**
     * GET /sap/opu/odata4/sap/ewm_storagetype_srv/srvd/sap/storage_type/0001/WarehouseZone
     * Returns storage zone capacity utilization.
     */
    @GET("sap/opu/odata4/sap/ewm_storagetype_srv/srvd/sap/storage_type/0001/WarehouseZone")
    suspend fun getZones(
        @Header("Authorization") authorization: String,
        @Query("\$format") format: String = "json",
        @Query("WarehouseNumber") warehouseNumber: String = "EWM1"
    ): ODataListResponse<WarehouseZoneDto>

    companion object {
        /**
         * Creates a configured Retrofit instance.
         * For production, replace basic auth with OAuth2 token exchange via SAP BTP.
         */
        fun create(): EwmApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create())
                .build()
                .create(EwmApiService::class.java)
        }
    }
}
