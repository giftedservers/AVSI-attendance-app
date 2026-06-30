package com.olkazi.attendance.network

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val success: Boolean,
    val error: String? = null,
    val token: String? = null,
    val user: UserDto? = null
)

data class UserDto(
    val id: Int,
    @SerializedName("employee_id") val employeeId: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String,
    val email: String,
    val role: String
)

data class StatusResponse(
    val success: Boolean,
    val error: String? = null,
    val employee: EmployeeDto? = null,
    val today: String? = null,
    @SerializedName("clocked_in") val clockedIn: Boolean = false,
    @SerializedName("clocked_out") val clockedOut: Boolean = false,
    val record: AttendanceRecord? = null,
    val offices: List<OfficeLocation> = emptyList()
)

data class EmployeeDto(
    val id: Int,
    @SerializedName("employee_id") val employeeId: String,
    @SerializedName("first_name") val firstName: String,
    @SerializedName("last_name") val lastName: String
)

data class OfficeLocation(
    val id: Int,
    val name: String,
    val latitude: String,
    val longitude: String,
    @SerializedName("radius_meters") val radiusMeters: Int
)

data class AttendanceRecord(
    val id: Int,
    val date: String,
    @SerializedName("clock_in") val clockIn: String?,
    @SerializedName("clock_out") val clockOut: String?,
    @SerializedName("total_hours") val totalHours: String?,
    val status: String?
)

data class ClockActionResponse(
    val success: Boolean,
    val error: String? = null,
    val message: String? = null,
    val status: String? = null,
    @SerializedName("total_hours") val totalHours: Double? = null
)

data class HistoryResponse(
    val success: Boolean,
    val error: String? = null,
    val records: List<AttendanceRecord> = emptyList()
)
