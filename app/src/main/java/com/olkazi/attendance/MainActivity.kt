package com.olkazi.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.olkazi.attendance.data.LocationHelper
import com.olkazi.attendance.network.ApiClient
import com.olkazi.attendance.network.Announcement
import com.olkazi.attendance.network.AttendanceRecord
import com.olkazi.attendance.network.Birthday
import com.olkazi.attendance.network.LeaveRequest
import com.olkazi.attendance.network.LeaveType
import com.olkazi.attendance.network.SessionManager
import com.olkazi.attendance.network.TimesheetSummary
import com.olkazi.attendance.network.TimesheetFormData
import com.olkazi.attendance.network.TsProject
import kotlinx.coroutines.launch
import java.util.Calendar

/** Formats a leave amount without a trailing ".0" for whole numbers (e.g. 21 not 21.0, but 2.5 stays 2.5). */
fun formatAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

/** Short label for a balance card, depending on the leave type's unit/period. */
fun balanceLabel(lt: LeaveType): String = when {
    lt.unit == "hour" -> "${formatAmount(lt.remaining)} hrs left"
    lt.periodType == "rolling" -> "${formatAmount(lt.remaining)}/${formatAmount(lt.allocated)} in last ${lt.periodWeeks ?: 0}wk"
    else -> "${formatAmount(lt.remaining)} days left"
}

fun balanceSubLabel(lt: LeaveType): String = when {
    lt.unit == "hour" -> "${formatAmount(lt.used)} hrs used of ${formatAmount(lt.allocated)} hrs/yr"
    lt.periodType == "rolling" -> "${formatAmount(lt.used)} used this window"
    else -> "${formatAmount(lt.used)} used of ${formatAmount(lt.allocated)}"
}

class MainActivity : ComponentActivity() {

    private val api by lazy { ApiClient.create(BuildConfig.API_BASE_URL) }
    private lateinit var session: SessionManager
    private lateinit var locationHelper: LocationHelper

    private var onPermissionResult: ((Boolean) -> Unit)? = null
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        onPermissionResult?.invoke(granted)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission(callback: (Boolean) -> Unit) {
        if (hasLocationPermission()) { callback(true); return }
        onPermissionResult = callback
        permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        locationHelper = LocationHelper(this)

        setContent {
            MaterialTheme(colorScheme = olkaziColors()) {
                AppRoot(
                    session = session,
                    api = api,
                    locationHelper = locationHelper,
                    ensureLocationPermission = { cb -> requestLocationPermission(cb) }
                )
            }
        }
    }
}

@Composable
fun olkaziColors() = lightColorScheme(
    primary = Color(0xFF1B5E3A),
    secondary = Color(0xFFE6A817)
)

@Composable
fun AppRoot(
    session: SessionManager,
    api: com.olkazi.attendance.network.OlkaziApi,
    locationHelper: LocationHelper,
    ensureLocationPermission: ((Boolean) -> Unit) -> Unit
) {
    var loggedIn by remember { mutableStateOf(session.isLoggedIn) }

    if (!loggedIn) {
        LoginScreen(api = api, session = session, onLoggedIn = { loggedIn = true })
    } else {
        ClockHomeScreen(
            api = api,
            session = session,
            locationHelper = locationHelper,
            ensureLocationPermission = ensureLocationPermission,
            onLoggedOut = { loggedIn = false }
        )
    }
}

@Composable
fun LoginScreen(
    api: com.olkazi.attendance.network.OlkaziApi,
    session: SessionManager,
    onLoggedIn: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Badge, contentDescription = null, tint = Color(0xFF1B5E3A), modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text("AVSI HRMS", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Staff Attendance", fontSize = 15.sp, color = Color.Gray)
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Work Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
        )

        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    error = "Enter your email and password"
                    return@Button
                }
                loading = true
                error = null
                scope.launch {
                    try {
                        val deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL
                        val resp = api.login(email = email.trim(), password = password, deviceName = deviceName)
                        if (resp.success && resp.token != null) {
                            session.token = resp.token
                            session.userName = "${resp.user?.firstName} ${resp.user?.lastName}"
                            session.employeeId = resp.user?.employeeId
                            onLoggedIn()
                        } else {
                            error = resp.error ?: "Login failed"
                        }
                    } catch (e: Exception) {
                        error = "Couldn't reach the server. Check your internet connection."
                    } finally {
                        loading = false
                    }
                }
            },
            enabled = !loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            else Text("Log In")
        }
    }
}

@Composable
fun ClockHomeScreen(
    api: com.olkazi.attendance.network.OlkaziApi,
    session: SessionManager,
    locationHelper: LocationHelper,
    ensureLocationPermission: ((Boolean) -> Unit) -> Unit,
    onLoggedOut: () -> Unit
) {
    var clockedIn by remember { mutableStateOf(false) }
    var clockedOut by remember { mutableStateOf(false) }
    var record by remember { mutableStateOf<AttendanceRecord?>(null) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var birthdaysToday by remember { mutableStateOf<List<Birthday>>(emptyList()) }
    var tab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    suspend fun refreshAnnouncements() {
        try {
            val resp = api.announcements(session.authHeader())
            if (resp.success) announcements = resp.announcements
        } catch (_: Exception) { }
    }

    suspend fun refreshBirthdays() {
        try {
            val resp = api.birthdaysToday(session.authHeader())
            if (resp.success) birthdaysToday = resp.birthdays
        } catch (_: Exception) { /* birthdays are a nice-to-have — never block the home screen on this */ }
    }

    suspend fun refreshStatus() {
        try {
            val resp = api.status(session.authHeader())
            if (resp.success) {
                clockedIn = resp.clockedIn
                clockedOut = resp.clockedOut
                record = resp.record
            }
        } catch (_: Exception) { /* silent refresh failure */ }
    }

    suspend fun refreshHistory() {
        try {
            val resp = api.history(session.authHeader())
            if (resp.success) history = resp.records
        } catch (_: Exception) { }
    }

    LaunchedEffect(Unit) { refreshStatus(); refreshHistory(); refreshAnnouncements(); refreshBirthdays() }

    fun doClockAction(isIn: Boolean) {
        statusMsg = null
        ensureLocationPermission { granted ->
            if (!granted) {
                statusMsg = "Location permission is required to clock ${if (isIn) "in" else "out"}."
                isError = true
                return@ensureLocationPermission
            }
            busy = true
            scope.launch {
                try {
                    val loc = locationHelper.getCurrentLocation()
                    val resp = if (isIn) {
                        api.clockIn(session.authHeader(), latitude = loc.latitude, longitude = loc.longitude)
                    } else {
                        api.clockOut(session.authHeader(), latitude = loc.latitude, longitude = loc.longitude)
                    }
                    if (resp.success) {
                        statusMsg = resp.message
                        isError = false
                        refreshStatus()
                        refreshHistory()
                    } else {
                        statusMsg = resp.error ?: "Something went wrong"
                        isError = true
                    }
                } catch (e: Exception) {
                    statusMsg = e.message ?: "Couldn't get your location or reach the server."
                    isError = true
                } finally {
                    busy = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hi, ${session.userName ?: "there"}") },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            try { api.logout(session.authHeader()) } catch (_: Exception) {}
                            session.clear()
                            onLoggedOut()
                        }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Log out")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = null) },
                    label = { Text("Clock") }
                )
                NavigationBarItem(
                    selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.EventNote, contentDescription = null) },
                    label = { Text("Leave") }
                )
                NavigationBarItem(
                    selected = tab == 2, onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Description, contentDescription = null) },
                    label = { Text("Timesheet") }
                )
                NavigationBarItem(
                    selected = tab == 3, onClick = { tab = 3 },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    label = { Text("History") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            if (tab == 0) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(12.dp))
                    when {
                        clockedOut -> {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF1B5E3A), modifier = Modifier.size(72.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Day complete!", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            record?.totalHours?.let { Text("$it hours worked", color = Color.Gray) }
                        }
                        clockedIn -> {
                            Icon(Icons.Default.SignLanguage, contentDescription = null, tint = Color(0xFFE6A817), modifier = Modifier.size(72.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Clocked in", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            record?.clockIn?.let { Text("Since $it", color = Color.Gray) }
                        }
                        else -> {
                            Icon(Icons.Default.WavingHand, contentDescription = null, tint = Color(0xFF1B5E3A), modifier = Modifier.size(72.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Ready to start your day?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    if (!clockedOut) {
                        Button(
                            onClick = { doClockAction(isIn = !clockedIn) },
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!clockedIn) Color(0xFF1B5E3A) else Color(0xFFC0392B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            if (busy) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("Getting your location…")
                            } else {
                                Icon(if (!clockedIn) Icons.Default.Login else Icons.Default.Logout, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (!clockedIn) "Clock In" else "Clock Out", fontSize = 17.sp)
                            }
                        }
                    }

                    statusMsg?.let {
                        Spacer(Modifier.height(16.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = if (isError) Color(0xFFFDECEA) else Color(0xFFE8F5E9))) {
                            Text(
                                it,
                                modifier = Modifier.padding(14.dp),
                                color = if (isError) Color(0xFFC0392B) else Color(0xFF1B5E3A)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your location is only checked at the moment you tap Clock In/Out — you must be within your office's geofence.",
                        fontSize = 12.sp, color = Color.Gray
                    )

                    if (announcements.isNotEmpty()) {
                        Spacer(Modifier.height(28.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFE6A817), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Announcements", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        announcements.forEach { ann ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(ann.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(Modifier.height(2.dp))
                                    Text(ann.content, fontSize = 13.sp, color = Color.DarkGray)
                                    Spacer(Modifier.height(4.dp))
                                    Text(ann.createdAt, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                    if (birthdaysToday.isNotEmpty()) {
                        Spacer(Modifier.height(28.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Icon(Icons.Default.Cake, contentDescription = null, tint = Color(0xFFE0245E), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Today's Birthdays", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        birthdaysToday.forEach { b ->
                            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Cake, contentDescription = null, tint = Color(0xFFE0245E), modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${b.firstName} ${b.lastName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            if (b.isSelf) {
                                                Spacer(Modifier.width(6.dp))
                                                Box(
                                                    Modifier
                                                        .background(Color(0xFFFFE58F), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                                ) { Text("You!", fontSize = 10.sp) }
                                            }
                                        }
                                        b.department?.let { Text(it, fontSize = 12.sp, color = Color.Gray) }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (tab == 1) {
                LeaveScreen(api = api, session = session)
            } else if (tab == 2) {
                TimesheetScreen(api = api, session = session)
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    items(history) { rec ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(rec.date, fontWeight = FontWeight.Bold)
                                    Text(rec.status ?: "", color = Color.Gray)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("In: ${rec.clockIn ?: "—"}   Out: ${rec.clockOut ?: "—"}")
                                rec.totalHours?.let { Text("$it hrs", color = Color(0xFF1B5E3A)) }
                            }
                        }
                    }
                    if (history.isEmpty()) {
                        item { Text("No attendance records yet.", modifier = Modifier.padding(20.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaveScreen(
    api: com.olkazi.attendance.network.OlkaziApi,
    session: SessionManager
) {
    var leaveTypes by remember { mutableStateOf<List<LeaveType>>(emptyList()) }
    var requests by remember { mutableStateOf<List<LeaveRequest>>(emptyList()) }
    var showApplySheet by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        try {
            val t = api.leaveTypes(session.authHeader())
            if (t.success) leaveTypes = t.leaveTypes
            val r = api.myLeaveRequests(session.authHeader())
            if (r.success) requests = r.requests
        } catch (_: Exception) { }
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    Box(Modifier.fillMaxSize()) {
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    Text("Leave Balances", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                }
                items(leaveTypes) { lt ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(lt.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(balanceSubLabel(lt), fontSize = 12.sp, color = Color.Gray)
                            }
                            Text(balanceLabel(lt), color = Color(0xFF1B5E3A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("My Requests", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Button(onClick = { showApplySheet = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Apply")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                items(requests) { req ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(req.leaveName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                StatusChip(req.status)
                            }
                            Spacer(Modifier.height(4.dp))
                            val amountLabel = if (req.unit == "hour")
                                "${req.startDate}  ${req.startTime?.take(5) ?: ""}–${req.endTime?.take(5) ?: ""}  (${formatAmount(req.hoursRequested ?: 0.0)}h)"
                            else
                                "${req.startDate} → ${req.endDate}  (${req.daysRequested}d)"
                            Text(amountLabel, fontSize = 13.sp)
                            if (req.status != "cancelled" && (req.managerStatus != null || req.crStatus != null)) {
                                Spacer(Modifier.height(4.dp))
                                ApprovalStageRow(req.managerStatus ?: "pending", req.crStatus ?: "pending")
                            }
                            req.reason?.let { if (it.isNotBlank()) Text(it, fontSize = 12.sp, color = Color.Gray) }
                            req.reviewerNotes?.let {
                                if (it.isNotBlank()) Text("Note: $it", fontSize = 12.sp, color = Color(0xFFC0392B))
                            }
                        }
                    }
                }
                if (requests.isEmpty()) {
                    item { Text("No leave requests yet.", modifier = Modifier.padding(top = 8.dp), color = Color.Gray) }
                }
            }
        }
    }

    if (showApplySheet) {
        ApplyLeaveDialog(
            leaveTypes = leaveTypes,
            onDismiss = { showApplySheet = false },
            onSubmit = { submission, onResult ->
                scope.launch {
                    try {
                        val resp = api.applyLeave(
                            session.authHeader(),
                            leaveTypeId = submission.leaveTypeId,
                            startDate = submission.startDate,
                            endDate = submission.endDate,
                            spaDate = submission.spaDate,
                            startTime = submission.startTime,
                            endTime = submission.endTime,
                            reason = submission.reason
                        )
                        if (resp.success) {
                            onResult(true, resp.message ?: "Submitted")
                            refresh()
                        } else {
                            onResult(false, resp.error ?: "Something went wrong")
                        }
                    } catch (e: Exception) {
                        onResult(false, "Couldn't reach the server. Check your connection.")
                    }
                }
            }
        )
    }
}

@Composable
fun StatusChip(status: String) {
    val color = when (status) {
        "approved" -> Color(0xFF1B5E3A)
        "rejected" -> Color(0xFFC0392B)
        "cancelled" -> Color.Gray
        else -> Color(0xFFE6A817)
    }
    Box(
        Modifier
            .background(color.copy(alpha = 0.15f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(status.uppercase(), color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Small "Manager: approved → Final: pending" pill row shown on each request card. */
@Composable
fun ApprovalStageRow(managerStatus: String, crStatus: String) {
    fun stageColor(s: String) = when (s) {
        "approved" -> Color(0xFF1B5E3A)
        "rejected" -> Color(0xFFC0392B)
        else -> Color(0xFFE6A817)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Mgr: ${managerStatus.replaceFirstChar { it.uppercase() }}", fontSize = 11.sp, color = stageColor(managerStatus), fontWeight = FontWeight.Medium)
        Text("  →  ", fontSize = 11.sp, color = Color.LightGray)
        Text("Final: ${crStatus.replaceFirstChar { it.uppercase() }}", fontSize = 11.sp, color = stageColor(crStatus), fontWeight = FontWeight.Medium)
    }
}

/** What ApplyLeaveDialog hands back to the caller — only the fields relevant
 *  to the selected leave type's unit are populated (day vs hour). */
data class LeaveSubmission(
    val leaveTypeId: Int,
    val startDate: String? = null,
    val endDate: String? = null,
    val spaDate: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val reason: String
)

@Composable
fun ApplyLeaveDialog(
    leaveTypes: List<LeaveType>,
    onDismiss: () -> Unit,
    onSubmit: (LeaveSubmission, (Boolean, String) -> Unit) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedType by remember { mutableStateOf<LeaveType?>(leaveTypes.firstOrNull()) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var spaDate by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("") }
    var endTime by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf<String?>(null) }
    var resultIsError by remember { mutableStateOf(false) }

    val isHourly = selectedType?.unit == "hour"

    fun pickDate(onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                onPicked(String.format("%04d-%02d-%02d", year, month + 1, day))
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).also { it.datePicker.minDate = cal.timeInMillis }.show()
    }

    fun pickTime(onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        android.app.TimePickerDialog(
            context,
            { _, hour, minute ->
                onPicked(String.format("%02d:%02d", hour, minute))
            },
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
        ).show()
    }

    // Live hour preview for SPA — recomputed whenever the times change.
    val spaHours: Double? = remember(startTime, endTime) {
        if (startTime.isBlank() || endTime.isBlank()) return@remember null
        try {
            val (sh, sm) = startTime.split(":").map { it.toInt() }
            val (eh, em) = endTime.split(":").map { it.toInt() }
            val diffMin = (eh * 60 + em) - (sh * 60 + sm)
            if (diffMin <= 0) null else Math.round(diffMin / 6.0) / 10.0 // round to nearest 0.1h
        } catch (e: Exception) { null }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply for Leave") },
        text = {
            Column {
                // ── Leave type dropdown ────────────────────────────────────
                Text("Leave Type", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                Box {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .clickable { typeMenuExpanded = true }
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedType?.let { "${it.name} (${balanceLabel(it)})" } ?: "Select leave type")
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                        leaveTypes.forEach { lt ->
                            DropdownMenuItem(
                                text = { Text("${lt.name} (${balanceLabel(lt)})") },
                                onClick = {
                                    selectedType = lt
                                    typeMenuExpanded = false
                                    // Reset fields not relevant to the newly selected type's unit
                                    startDate = ""; endDate = ""; spaDate = ""; startTime = ""; endTime = ""
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (isHourly) {
                    // ── SPA: single date + start/end time ───────────────────
                    Text("Date", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .clickable { pickDate { spaDate = it } }
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (spaDate.isBlank()) "Pick date" else spaDate, color = if (spaDate.isBlank()) Color.Gray else Color.Unspecified)
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("Start Time", fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.Gray, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                    .clickable { pickTime { startTime = it } }
                                    .padding(horizontal = 10.dp, vertical = 14.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (startTime.isBlank()) "--:--" else startTime, color = if (startTime.isBlank()) Color.Gray else Color.Unspecified)
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("End Time", fontSize = 12.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color.Gray, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                    .clickable { pickTime { endTime = it } }
                                    .padding(horizontal = 10.dp, vertical = 14.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (endTime.isBlank()) "--:--" else endTime, color = if (endTime.isBlank()) Color.Gray else Color.Unspecified)
                                    Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    spaHours?.let { h ->
                        Spacer(Modifier.height(8.dp))
                        val maxAllowed = selectedType?.maxPerRequest
                        val overLimit = maxAllowed != null && h > maxAllowed
                        Text(
                            if (overLimit) "⚠ ${formatAmount(h)} hrs — exceeds the ${formatAmount(maxAllowed!!)}-hour limit per request"
                            else "⏱ ${formatAmount(h)} hour(s)" + (maxAllowed?.let { " · max ${formatAmount(it)} hrs/request" } ?: ""),
                            fontSize = 12.sp,
                            color = if (overLimit) Color(0xFFC0392B) else Color(0xFF1B5E3A)
                        )
                    }
                } else {
                    // ── Day-based: start/end date range ──────────────────────
                    Text("Start Date", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .clickable { pickDate { startDate = it } }
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (startDate.isBlank()) "Pick start date" else startDate, color = if (startDate.isBlank()) Color.Gray else Color.Unspecified)
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text("End Date", fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                            .clickable { pickDate { endDate = it } }
                            .padding(horizontal = 12.dp, vertical = 14.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(if (endDate.isBlank()) "Pick end date" else endDate, color = if (endDate.isBlank()) Color.Gray else Color.Unspecified)
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (selectedType?.periodType == "rolling") {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Rolling allowance: ${formatAmount(selectedType?.allocated ?: 0.0)} day(s) per ${selectedType?.periodWeeks ?: 0} weeks.",
                            fontSize = 11.sp, color = Color.Gray
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Reason ────────────────────────────────────────────────
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                resultMsg?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = if (resultIsError) Color(0xFFC0392B) else Color(0xFF1B5E3A), fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !submitting,
                onClick = {
                    val type = selectedType
                    if (type == null || reason.isBlank()) {
                        resultMsg = "Please fill in all fields"
                        resultIsError = true
                        return@Button
                    }
                    val submission = if (isHourly) {
                        if (spaDate.isBlank() || startTime.isBlank() || endTime.isBlank()) {
                            resultMsg = "Please fill in all fields"
                            resultIsError = true
                            return@Button
                        }
                        LeaveSubmission(
                            leaveTypeId = type.id,
                            spaDate = spaDate, startTime = startTime, endTime = endTime,
                            reason = reason
                        )
                    } else {
                        if (startDate.isBlank() || endDate.isBlank()) {
                            resultMsg = "Please fill in all fields"
                            resultIsError = true
                            return@Button
                        }
                        LeaveSubmission(
                            leaveTypeId = type.id,
                            startDate = startDate, endDate = endDate,
                            reason = reason
                        )
                    }
                    submitting = true
                    onSubmit(submission) { ok, msg ->
                        submitting = false
                        resultMsg = msg
                        resultIsError = !ok
                        if (ok) onDismiss()
                    }
                }
            ) {
                if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                else Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ══════════════════════════════════════════════════════════════════════
//  Timesheet — NEW
// ══════════════════════════════════════════════════════════════════════

/** Returns true if (year, month 1-12, day) falls on a Monday–Friday. */
fun isWorkingDay(year: Int, month: Int, day: Int): Boolean {
    val cal = Calendar.getInstance()
    cal.set(year, month - 1, day, 0, 0, 0)
    val dow = cal.get(Calendar.DAY_OF_WEEK)
    return dow != Calendar.SATURDAY && dow != Calendar.SUNDAY
}

fun monthName(month: Int): String =
    listOf("", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December").getOrElse(month) { "" }

@Composable
fun TimesheetScreen(
    api: com.olkazi.attendance.network.OlkaziApi,
    session: SessionManager
) {
    var timesheets by remember { mutableStateOf<List<TimesheetSummary>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showEditor by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        try {
            val r = api.myTimesheets(session.authHeader())
            if (r.success) timesheets = r.timesheets
        } catch (_: Exception) { }
        loading = false
    }

    LaunchedEffect(Unit) { refresh() }

    if (showEditor) {
        TimesheetGridEditor(
            api = api,
            session = session,
            onDone = {
                showEditor = false
                loading = true
                scope.launch { refresh() }
            }
        )
        return
    }

    Box(Modifier.fillMaxSize()) {
        if (loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("My Timesheets", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Button(onClick = { showEditor = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Submit")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                items(timesheets) { ts ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${monthName(ts.month)} ${ts.year}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                StatusChip(ts.status)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("${formatAmount(ts.totalHours)} hrs total", fontSize = 13.sp, color = Color(0xFF1B5E3A))
                            ts.supervisorName?.let { Text("Supervisor: $it", fontSize = 12.sp, color = Color.Gray) }
                            ts.location?.let { if (it.isNotBlank()) Text("Location: $it", fontSize = 12.sp, color = Color.Gray) }
                        }
                    }
                }
                if (timesheets.isEmpty()) {
                    item { Text("No timesheets submitted yet.", modifier = Modifier.padding(top = 8.dp), color = Color.Gray) }
                }
            }
        }
    }
}

// ── Grid editor — mirrors timesheet/timesheet.php's grid exactly: one row
// per active project + one row per active leave type, one column per day
// of the month, every cell freely editable. Project-hour cells are
// disabled on days blocked by a public holiday or full-day approved leave
// (leave cells are never blocked). ─────────────────────────────────────

private const val GRID_LABEL_WIDTH_DP = 118
private const val GRID_CELL_WIDTH_DP = 42
private const val GRID_CELL_HEIGHT_DP = 40

@Composable
fun TimesheetGridEditor(
    api: com.olkazi.attendance.network.OlkaziApi,
    session: SessionManager,
    onDone: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val now = remember { Calendar.getInstance() }
    val month = now.get(Calendar.MONTH) + 1
    val year = now.get(Calendar.YEAR)
    val hScroll = rememberScrollState()

    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var formData by remember { mutableStateOf<TimesheetFormData?>(null) }
    val cellValues = remember { mutableStateMapOf<String, String>() }

    var location by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var resultMsg by remember { mutableStateOf<String?>(null) }
    var resultIsError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val fd = api.timesheetFormData(session.authHeader(), month = month, year = year)
            if (fd.success) {
                formData = fd
                location = fd.location ?: ""
                title = fd.title ?: fd.jobTitle ?: ""
                fd.projects.forEach { p ->
                    (fd.savedProjectHours[p.id.toString()] ?: emptyMap()).forEach { (day, hrs) ->
                        cellValues["p:${p.id}:$day"] = formatAmount(hrs)
                    }
                }
                fd.leaveTypes.forEach { lt ->
                    val saved = fd.savedLeaveHours[lt.key] ?: emptyMap()
                    val suggested = fd.suggestedLeaveHours[lt.key] ?: emptyMap()
                    (saved.keys + suggested.keys).toSet().forEach { day ->
                        val v = saved[day] ?: suggested[day]
                        if (v != null) cellValues["l:${lt.key}:$day"] = formatAmount(v)
                    }
                }
            } else {
                loadError = fd.error ?: "Could not load timesheet form."
            }
        } catch (e: Exception) {
            loadError = "Couldn't reach the server. Check your connection."
        }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDone) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
            Text("Submit — ${monthName(month)} $year", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        HorizontalDivider()

        val fd = formData
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            loadError != null -> Box(Modifier.fillMaxSize().padding(20.dp)) {
                Text(loadError!!, color = Color(0xFFC0392B))
            }
            fd == null -> { /* unreachable — success without form data */ }
            fd.status == "approved" -> Box(Modifier.fillMaxSize().padding(20.dp)) {
                Text(
                    "This month's timesheet has already been approved and can't be changed from the app. Contact HR if it needs correcting.",
                    color = Color(0xFFC0392B)
                )
            }
            else -> {
                val days = 1..fd.daysInMonth

                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (fd.status != null) {
                        Text(
                            "You already have a ${fd.status} submission for this month — submitting again will replace it.",
                            fontSize = 12.sp, color = Color(0xFFE6A817)
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    OutlinedTextField(
                        value = location, onValueChange = { location = it },
                        label = { Text("Location") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = title, onValueChange = { title = it },
                        label = { Text("Title / Role") }, modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }

                if (fd.projects.isEmpty() && fd.leaveTypes.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(20.dp)) {
                        Text("No active projects or leave types are configured — contact HR.", color = Color(0xFFC0392B))
                    }
                }

                LazyColumn(Modifier.weight(1f)) {
                    item {
                        Row(Modifier.fillMaxWidth()) {
                            Box(Modifier.width(GRID_LABEL_WIDTH_DP.dp).height(GRID_CELL_HEIGHT_DP.dp))
                            Row(Modifier.horizontalScroll(hScroll)) {
                                days.forEach { d ->
                                    val blocked = fd.blockedDays.containsKey(d.toString())
                                    val holidayName = fd.publicHolidays[d.toString()]
                                    val weekend = !isWorkingDay(year, month, d)
                                    GridDayHeaderCell(day = d, blocked = blocked, weekend = weekend, holidayName = holidayName)
                                }
                            }
                        }
                    }
                    item { HorizontalDivider() }
                    items(fd.projects) { p ->
                        GridRow(
                            label = p.code,
                            days = days,
                            hScroll = hScroll,
                            cellEnabled = { d -> !fd.blockedDays.containsKey(d.toString()) },
                            valueFor = { d -> cellValues["p:${p.id}:$d"] ?: "" },
                            onValueChange = { d, v -> cellValues["p:${p.id}:$d"] = v }
                        )
                    }
                    if (fd.projects.isNotEmpty() && fd.leaveTypes.isNotEmpty()) {
                        item { HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp)) }
                    }
                    items(fd.leaveTypes) { lt ->
                        GridRow(
                            label = lt.label,
                            days = days,
                            hScroll = hScroll,
                            cellEnabled = { true },
                            valueFor = { d -> cellValues["l:${lt.key}:$d"] ?: "" },
                            onValueChange = { d, v -> cellValues["l:${lt.key}:$d"] = v }
                        )
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }

                val grandTotal = cellValues.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
                HorizontalDivider()
                resultMsg?.let {
                    Text(
                        it, color = if (resultIsError) Color(0xFFC0392B) else Color(0xFF1B5E3A),
                        fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total: ${formatAmount(grandTotal)} hrs", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Button(
                        enabled = !submitting,
                        onClick = {
                            if (location.isBlank() || title.isBlank()) {
                                resultMsg = "Location and title are required."
                                resultIsError = true
                                return@Button
                            }
                            val entries = org.json.JSONArray()
                            cellValues.forEach { (cellKey, valStr) ->
                                val hrs = valStr.toDoubleOrNull() ?: return@forEach
                                if (hrs <= 0) return@forEach
                                val parts = cellKey.split(":")
                                if (parts.size != 3) return@forEach
                                val (kind, idOrKey, dayStr) = parts
                                val day = dayStr.toIntOrNull() ?: return@forEach
                                entries.put(org.json.JSONObject().apply {
                                    if (kind == "p") {
                                        put("type", "project")
                                        put("project_id", idOrKey.toIntOrNull() ?: 0)
                                    } else {
                                        put("type", "leave")
                                        put("leave_key", idOrKey)
                                    }
                                    put("day", day)
                                    put("hours", hrs)
                                })
                            }
                            if (entries.length() == 0) {
                                resultMsg = "Please enter at least some hours before submitting."
                                resultIsError = true
                                return@Button
                            }
                            submitting = true
                            scope.launch {
                                try {
                                    val resp = api.submitTimesheet(
                                        session.authHeader(),
                                        month = month, year = year,
                                        location = location, title = title,
                                        entriesJson = entries.toString()
                                    )
                                    if (resp.success) {
                                        resultMsg = resp.message ?: "Submitted"
                                        resultIsError = false
                                        onDone()
                                    } else {
                                        resultMsg = resp.error ?: "Something went wrong"
                                        resultIsError = true
                                    }
                                } catch (e: Exception) {
                                    resultMsg = "Couldn't reach the server. Check your connection."
                                    resultIsError = true
                                } finally {
                                    submitting = false
                                }
                            }
                        }
                    ) {
                        if (submitting) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        else Text("Submit")
                    }
                }
            }
        }
    }
}

@Composable
fun GridDayHeaderCell(day: Int, blocked: Boolean, weekend: Boolean, holidayName: String?) {
    val bg = when {
        blocked -> Color(0xFFFFE0E0)
        weekend -> Color(0xFFEDEDED)
        else -> Color(0xFF1B5E3A)
    }
    val fg = if (blocked || weekend) Color.DarkGray else Color.White
    Box(
        Modifier
            .width(GRID_CELL_WIDTH_DP.dp)
            .height(GRID_CELL_HEIGHT_DP.dp)
            .background(bg)
            .border(0.5.dp, Color(0xFFDDDDDD)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
            if (holidayName != null) {
                Icon(Icons.Default.Info, contentDescription = holidayName, tint = fg, modifier = Modifier.size(9.dp))
            }
        }
    }
}

@Composable
fun GridRow(
    label: String,
    days: IntRange,
    hScroll: androidx.compose.foundation.ScrollState,
    cellEnabled: (Int) -> Boolean,
    valueFor: (Int) -> String,
    onValueChange: (Int, String) -> Unit
) {
    Row(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .width(GRID_LABEL_WIDTH_DP.dp)
                .height(GRID_CELL_HEIGHT_DP.dp)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(label, fontSize = 12.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        Row(Modifier.horizontalScroll(hScroll)) {
            days.forEach { d ->
                GridCell(
                    value = valueFor(d),
                    enabled = cellEnabled(d),
                    onValueChange = { onValueChange(d, it) }
                )
            }
        }
    }
}

@Composable
fun GridCell(value: String, enabled: Boolean, onValueChange: (String) -> Unit) {
    Box(
        Modifier
            .width(GRID_CELL_WIDTH_DP.dp)
            .height(GRID_CELL_HEIGHT_DP.dp)
            .background(if (!enabled) Color(0xFFF2F2F2) else Color.White)
            .border(0.5.dp, Color(0xFFE0E0E0)),
        contentAlignment = Alignment.Center
    ) {
        if (enabled) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = { new ->
                    if (new.length <= 5 && new.all { it.isDigit() || it == '.' }) onValueChange(new)
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color(0xFF1B5E3A)
                ),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)
            )
        } else {
            Text("—", fontSize = 12.sp, color = Color.LightGray)
        }
    }
}
