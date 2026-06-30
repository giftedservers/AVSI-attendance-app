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

data class LeaveType(
    val id: Int,
    val name: String,
    val color: String?,
    val description: String?,
    @SerializedName("count_calendar_days") val countCalendarDays: Int,
    val allocated: Int,
    val used: Int,
    val remaining: Int
)

data class LeaveTypesResponse(
    val success: Boolean,
    val error: String? = null,
    @SerializedName("leave_types") val leaveTypes: List<LeaveType> = emptyList()
)

data class LeaveRequest(
    val id: Int,
    @SerializedName("leave_name") val leaveName: String,
    val color: String?,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("end_date") val endDate: String,
    @SerializedName("days_requested") val daysRequested: Int,
    val reason: String?,
    val status: String,
    @SerializedName("reviewer_notes") val reviewerNotes: String?
)

data class LeaveRequestsResponse(
    val success: Boolean,
    val error: String? = null,
    val requests: List<LeaveRequest> = emptyList()
)

data class LeaveApplyResponse(
    val success: Boolean,
    val error: String? = null,
    val message: String? = null,
    val days: Int? = null
)

data class Announcement(
    val id: Int,
    val title: String,
    val content: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("posted_by_name") val postedByName: String?
)

data class AnnouncementsResponse(
    val success: Boolean,
    val error: String? = null,
    val announcements: List<Announcement> = emptyList()
)
