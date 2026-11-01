package com.carribu.parentnative.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carribu.parentnative.BuildConfig
import com.carribu.parentnative.data.model.ModelLogic
import com.carribu.parentnative.data.model.User
import com.carribu.parentnative.ui.ProfileViewModel
import com.carribu.parentnative.ui.components.ErrorCard
import com.carribu.parentnative.ui.components.SectionCard
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(viewModel: ProfileViewModel, user: User, padding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editPickup by rememberSaveable { mutableStateOf(false) }
    var editPassword by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.load() }
    state.notice?.let {
        AlertDialog(
            onDismissRequest = viewModel::clearNotice,
            confirmButton = { TextButton(onClick = viewModel::clearNotice) { Text("OK") } },
            text = { Text(it) }
        )
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.secondary) {
                        Text(
                            "${user.firstName.firstOrNull()?.toString().orEmpty()}${user.lastName.firstOrNull()?.toString().orEmpty()}",
                            modifier = Modifier.padding(20.dp),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(user.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(user.parentSchools.ifEmpty { listOfNotNull(user.school) }.joinToString(" • ") { it.name })
                }
            }
        }
        item {
            state.error?.let { ErrorCard(it, viewModel::load) }
        }
        item {
            SectionCard("Contact & pickup") {
                ProfileLine(Icons.Default.Email, user.email ?: "Email not set")
                ProfileLine(Icons.Default.Phone, user.phone ?: "Phone not set")
                ProfileLine(Icons.Default.LocationOn, user.pickupAddress ?: "Pickup not set")
                TextButton(onClick = { editPickup = !editPickup }) {
                    Text(if (editPickup) "Close editor" else "Edit pickup location")
                }
                if (editPickup) PickupEditor(user, state.saving, viewModel::updatePickup)
            }
        }
        item {
            SectionCard("My children") {
                if (state.loading) CircularProgressIndicator()
                else if (state.children.isEmpty()) Text("No children linked to this parent account.")
                else state.children.forEach { child ->
                    ListItem(
                        headlineContent = { Text(child.fullName, fontWeight = FontWeight.Bold) },
                        supportingContent = {
                            Text(
                                "${child.grade ?: "Grade not set"} • ${child.school?.name ?: "School"}\n" +
                                    (child.routes.joinToString { it.name }.ifBlank { "No route assigned" })
                            )
                        },
                        leadingContent = { Icon(Icons.Default.School, null) }
                    )
                }
            }
        }
        item {
            SectionCard("Security") {
                TextButton(onClick = { editPassword = !editPassword }) {
                    Icon(Icons.Default.Lock, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (editPassword) "Cancel password change" else "Change password")
                }
                if (editPassword) PasswordEditor(state.saving, viewModel::changePassword)
            }
        }
        item {
            SectionCard("App version") {
                Text("Carribu Parent Native ${BuildConfig.VERSION_NAME}")
                val latest = state.latestVersion
                if (latest != null && viewModel.updateAvailable()) {
                    Text(latest.releaseNotes.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(latest.downloadUrl)))
                    }) {
                        Icon(Icons.Default.SystemUpdate, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Download ${latest.version}")
                    }
                } else {
                    Text("You’re using the latest published parent version.")
                }
            }
        }
        item {
            OutlinedButton(
                onClick = viewModel::logout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Logout, null)
                Spacer(Modifier.width(8.dp))
                Text("Log out")
            }
        }
    }
}

@Composable
private fun ProfileLine(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(value)
    }
}

@Composable
private fun PickupEditor(
    user: User,
    saving: Boolean,
    onSave: (String, Double?, Double?) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initial: LatLng? = if (ModelLogic.validCoordinate(user.pickupLat, user.pickupLng)) {
        LatLng(user.pickupLat!!, user.pickupLng!!)
    } else {
        null
    }
    var address by rememberSaveable { mutableStateOf(user.pickupAddress.orEmpty()) }
    var hasMarker by rememberSaveable { mutableStateOf(initial != null) }
    var mapLoaded by remember { mutableStateOf(false) }
    var locationError by rememberSaveable { mutableStateOf<String?>(null) }
    val marker = rememberUpdatedMarkerState(position = initial ?: LatLng(0.0, 0.0))
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initial ?: LatLng(0.0, 0.0), if (initial == null) 1f else 16f)
    }
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val mapReadiness = rememberMapReadiness()

    fun locate() {
        scope.launch {
            try {
                locationError = null
                val tokenSource = CancellationTokenSource()
                val location = locationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    tokenSource.token
                ).await()
                if (location == null) {
                    locationError = "Your current location is unavailable. Turn on location services and try again."
                    return@launch
                }
                val target = LatLng(location.latitude, location.longitude)
                marker.position = target
                hasMarker = true
                if (mapLoaded) {
                    camera.animate(CameraUpdateFactory.newLatLngZoom(target, 17f))
                } else {
                    camera.position = CameraPosition.fromLatLngZoom(target, 17f)
                }
            } catch (_: SecurityException) {
                locationError = "Location permission is required to use your current position."
            } catch (error: Exception) {
                locationError = "Current location could not be loaded. Update Google Play Services and try again."
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) locate()
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        locationError?.let { ErrorCard(it, { locationError = null }) }
        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Pickup address") },
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.fillMaxWidth().height(260.dp)) {
            if (mapReadiness.available) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = camera,
                    onMapLoaded = { mapLoaded = true },
                    onMapClick = {
                        marker.position = it
                        hasMarker = true
                    },
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                ) {
                    if (hasMarker) Marker(state = marker, title = "Pickup", draggable = true)
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            mapReadiness.message.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (mapReadiness.available) {
                SmallFloatingActionButton(
                    onClick = {
                        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED
                        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED
                        if (fine || coarse) locate()
                        else permissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)
                ) { Icon(Icons.Default.MyLocation, "Use current location") }
            }
        }
        Text(
            if (hasMarker) "%.6f, %.6f".format(marker.position.latitude, marker.position.longitude)
            else "Tap the map or use your current location to place the pickup pin.",
            style = MaterialTheme.typography.labelMedium
        )
        Button(
            onClick = {
                onSave(
                    address,
                    marker.position.latitude.takeIf { hasMarker },
                    marker.position.longitude.takeIf { hasMarker }
                )
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (saving) "Saving…" else "Save pickup location") }
    }
}

@Composable
private fun PasswordEditor(
    saving: Boolean,
    onSave: (String, String, String) -> Unit
) {
    var current by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmation by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            current, { current = it }, label = { Text("Current password") },
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            password, { password = it }, label = { Text("New password") },
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            confirmation, { confirmation = it }, label = { Text("Confirm password") },
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { onSave(current, password, confirmation) },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (saving) "Updating…" else "Update password") }
    }
}
