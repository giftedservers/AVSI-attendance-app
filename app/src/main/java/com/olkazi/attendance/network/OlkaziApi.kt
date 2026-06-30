package com.olkazi.attendance.network

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface OlkaziApi {

    @FormUrlEncoded
    @POST("auth.php")
    suspend fun login(
        @Field("action") action: String = "login",
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("device_name") deviceName: String
    ): LoginResponse

    @FormUrlEncoded
    @POST("auth.php")
    suspend fun logout(
        @Header("Authorization") authHeader: String,
        @Field("action") action: String = "logout"
    ): LoginResponse

    @GET("clock.php")
    suspend fun status(
        @Header("Authorization") authHeader: String,
        @Query("action") action: String = "status"
    ): StatusResponse

    @FormUrlEncoded
    @POST("clock.php")
    suspend fun clockIn(
        @Header("Authorization") authHeader: String,
        @Field("action") action: String = "clock_in",
        @Field("latitude") latitude: Double,
        @Field("longitude") longitude: Double,
        @Field("notes") notes: String = ""
    ): ClockActionResponse

    @FormUrlEncoded
    @POST("clock.php")
    suspend fun clockOut(
        @Header("Authorization") authHeader: String,
        @Field("action") action: String = "clock_out",
        @Field("latitude") latitude: Double,
        @Field("longitude") longitude: Double,
        @Field("notes") notes: String = ""
    ): ClockActionResponse

    @GET("clock.php")
    suspend fun history(
        @Header("Authorization") authHeader: String,
        @Query("action") action: String = "history"
    ): HistoryResponse
}
