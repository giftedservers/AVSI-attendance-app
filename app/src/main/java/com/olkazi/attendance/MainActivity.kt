package com.olkazi.attendance

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.olkazi.attendance.network.AttendanceRecord
import com.olkazi.attendance.network.SessionManager
import kotlinx.coroutines.launch

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
        Text("Olkazi HRMS", fontSize = 26.sp, fontWeight = FontWeight.Bold)
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
    var tab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

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

    LaunchedEffect(Unit) { refreshStatus(); refreshHistory() }

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
                }
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
