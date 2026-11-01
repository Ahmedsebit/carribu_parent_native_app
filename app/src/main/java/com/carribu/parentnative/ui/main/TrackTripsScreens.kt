package com.carribu.parentnative.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carribu.parentnative.data.model.*
import com.carribu.parentnative.realtime.RealtimeTopic
import com.carribu.parentnative.realtime.RealtimeUpdates
import com.carribu.parentnative.ui.TrackViewModel
import com.carribu.parentnative.ui.TripsViewModel
import com.carribu.parentnative.ui.components.*
import com.carribu.parentnative.ui.theme.CarribuAmber
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TrackScreen(
    viewModel: TrackViewModel,
    realtime: RealtimeUpdates,
    padding: PaddingValues
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showMap by rememberSaveable { mutableStateOf(false) }
    LifecyclePolling(realtime, RealtimeTopic.LOCATION) { viewModel.refresh() }

    Column(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Live bus tracking", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text(
                    if (state.buses.isEmpty()) "No active buses" else "${state.buses.size} active bus${if (state.buses.size == 1) "" else "es"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { viewModel.refresh(true) }) {
                if (state.refreshing) CircularProgressIndicator(Modifier.size(22.dp))
                else Icon(Icons.Default.Refresh, "Refresh")
            }
            FilledTonalIconButton(
                onClick = { showMap = !showMap },
                enabled = state.buses.isNotEmpty()
            ) {
                Icon(if (showMap) Icons.Default.ViewList else Icons.Default.Map, "Change view")
            }
        }
        Spacer(Modifier.height(10.dp))
        state.error?.let { ErrorCard(it, { viewModel.refresh(true) }, Modifier.padding(bottom = 10.dp)) }
        if (state.loading) {
            LoadingState("Finding your child’s bus…")
        } else if (state.buses.isEmpty()) {
            EmptyState("🚌", "No active buses", "Trips appear here after the driver starts a route.")
        } else if (showMap) {
            TrackMap(state.buses, state.selectedTripId, viewModel::select)
        } else {
            BusList(state.buses)
        }
    }
}

@Composable
private fun TrackMap(buses: List<ActiveBus>, selectedTripId: Int?, onSelect: (Int) -> Unit) {
    val mapReadiness = rememberMapReadiness()
    if (!mapReadiness.available) {
        EmptyState("🗺️", "Map unavailable", mapReadiness.message.orEmpty())
        return
    }
    val bus = buses.firstOrNull { it.tripId == selectedTripId } ?: buses.first()
    val validLocation = bus.location?.takeIf { ModelLogic.validCoordinate(it.lat, it.lng) }
    if (validLocation == null) {
        EmptyState("📡", "Waiting for GPS", "No valid authoritative GPS fix has arrived for this active trip yet.")
        return
    }
    val busPosition = LatLng(validLocation.lat, validLocation.lng)
    val camera = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(busPosition, 14f)
    }
    var mapLoaded by remember { mutableStateOf(false) }
    var mapError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(bus.tripId, busPosition, mapLoaded) {
        if (!mapLoaded) return@LaunchedEffect
        try {
            camera.animate(CameraUpdateFactory.newLatLngZoom(busPosition, 14f))
        } catch (error: IllegalStateException) {
            mapError = "The map could not move to the bus location. Please reopen the map."
        }
    }
    Column(Modifier.fillMaxSize()) {
        if (buses.size > 1) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(buses, key = { it.tripId }) { item ->
                    FilterChip(
                        selected = item.tripId == bus.tripId,
                        onClick = { onSelect(item.tripId) },
                        label = { Text(item.routeName) },
                        leadingIcon = { Icon(Icons.Default.DirectionsBus, null) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        mapError?.let { ErrorCard(it, { mapError = null }, Modifier.padding(bottom = 8.dp)) }
        Card(Modifier.weight(1f).fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camera,
                onMapLoaded = {
                    mapLoaded = true
                    mapError = null
                },
                uiSettings = MapUiSettings(zoomControlsEnabled = false, compassEnabled = true)
            ) {
                Marker(
                    state = rememberUpdatedMarkerState(busPosition),
                    title = bus.routeName,
                    snippet = bus.vehicle?.plateNumber ?: "Bus",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
                val stops = bus.pendingStops
                    .filter { ModelLogic.validCoordinate(it.lat, it.lng) }
                    .sortedBy { it.stopOrder }
                stops.forEach { stop ->
                    Marker(
                        state = rememberUpdatedMarkerState(LatLng(stop.lat, stop.lng)),
                        title = if (stop.isMyChild) "${stop.firstName ?: "Child"}’s stop" else "Stop ${stop.stopOrder}",
                        snippet = "Stop ${stop.stopOrder}",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (stop.isMyChild) BitmapDescriptorFactory.HUE_GREEN else BitmapDescriptorFactory.HUE_ORANGE
                        )
                    )
                }
                val line = listOf(busPosition) + stops.map { LatLng(it.lat, it.lng) }
                if (line.size > 1) {
                    Polyline(points = line, color = MaterialTheme.colorScheme.primary, width = 9f)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        BusSummary(bus)
    }
}

@Composable
private fun BusSummary(bus: ActiveBus) {
    val context = LocalContext.current
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(bus.routeName, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (bus.tripType == "morning_pickup") "Morning pickup" else "Afternoon drop-off",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    bus.routeSchool?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                }
                StatusPill(if (ModelLogic.freshLocation(bus.location)) "LIVE" else "LAST KNOWN", MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(10.dp))
            Text("${bus.driver?.displayName ?: "Driver"} • ${bus.vehicle?.plateNumber ?: "Vehicle pending"}")
            Text("Phase: ${bus.phase.orEmpty().replace('_', ' ')}")
            Text("GPS fix: ${bus.location?.recordedAt ?: "unavailable"}", style = MaterialTheme.typography.labelSmall)
            if (!ModelLogic.freshLocation(bus.location)) Text("Location is not fresh. Arrival estimates are unavailable.")
            bus.children.forEach { child ->
                child.eta?.takeIf { ModelLogic.freshLocation(bus.location) }?.let { eta ->
                    Text(
                        if (eta.alreadyPickedUp) "✓ ${child.firstName} is already picked up"
                        else "⏱ ${child.firstName}: about ${eta.totalMinutes ?: "—"} min • ${eta.distanceKm ?: "—"} km",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            bus.driver?.phone?.let { phone ->
                TextButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                }) {
                    Icon(Icons.Default.Call, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Call driver")
                }
            }
        }
    }
}

@Composable
private fun BusList(buses: List<ActiveBus>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(buses, key = { it.tripId }) { bus ->
            BusSummary(bus)
        }
    }
}

@Composable
fun TripsScreen(viewModel: TripsViewModel, padding: PaddingValues) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("Trips & history", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(10.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !state.showingHistory,
                onClick = { viewModel.setHistory(false) },
                shape = SegmentedButtonDefaults.itemShape(0, 2)
            ) { Text("Upcoming") }
            SegmentedButton(
                selected = state.showingHistory,
                onClick = { viewModel.setHistory(true) },
                shape = SegmentedButtonDefaults.itemShape(1, 2)
            ) { Text("History") }
        }
        TripFiltersRow(state.filters, state.schoolId, state.studentId, viewModel)
        state.error?.let { ErrorCard(it, viewModel::load, Modifier.padding(vertical = 8.dp)) }
        if (state.loading) {
            LoadingState("Loading trips…")
        } else {
            val trips = if (state.showingHistory) state.history else state.upcoming
            if (trips.isEmpty()) {
                EmptyState("🗓️", "Nothing scheduled", if (state.showingHistory) "No completed trips in the last 30 days." else "No trips in the next 7 days.")
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(trips, key = { it.id }) { trip ->
                        TripCard(trip, state.showingHistory)
                    }
                }
            }
        }
    }
}

@Composable
private fun TripFiltersRow(
    filters: TripFilters,
    schoolId: Int?,
    studentId: Int?,
    viewModel: TripsViewModel
) {
    Text("School", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 12.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterChip(schoolId == null, { viewModel.selectSchool(null) }, { Text("All") }) }
        items(filters.schools, key = { it.id }) { school ->
            FilterChip(schoolId == school.id, { viewModel.selectSchool(school.id) }, { Text(school.name) })
        }
    }
    Text("Child", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
    val students = ModelLogic.studentsForSchool(filters.students, schoolId)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterChip(studentId == null, { viewModel.selectStudent(null) }, { Text("All") }) }
        items(students, key = { it.id }) { student ->
            FilterChip(studentId == student.id, { viewModel.selectStudent(student.id) }, { Text(student.fullName) })
        }
    }
}

@Composable
private fun TripCard(trip: Trip, history: Boolean) {
    var expanded by rememberSaveable(trip.id) { mutableStateOf(false) }
    Card(
        onClick = { if (history) expanded = !expanded },
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(trip.route?.name ?: "Route", fontWeight = FontWeight.ExtraBold)
                    trip.school?.let { Text(it.name, color = MaterialTheme.colorScheme.primary) }
                }
                StatusPill(
                    trip.status?.replaceFirstChar(Char::uppercase) ?: "Scheduled",
                    if (trip.status == "delayed") CarribuAmber else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("${formatDate(trip.scheduledDate)} • ${tripType(trip.type)}")
            val time = if (history) "${formatTime(trip.startedAt)}–${formatTime(trip.endedAt)}"
            else trip.scheduledTime?.take(5) ?: "Time pending"
            Text("$time • ${trip.driver?.displayName ?: "Driver TBD"}")
            Text("${trip.vehicle?.plateNumber ?: "Vehicle TBD"} • ${trip.children.joinToString { it.studentName }}")
            if (expanded) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp))
                trip.children.forEach { child ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text(child.studentName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text(child.status?.replace('_', ' ') ?: "No record")
                    }
                    child.waitSeconds?.let { Text("Waited ${formatWait(it)}", style = MaterialTheme.typography.labelSmall) }
                }
            }
        }
    }
}

private fun tripType(type: String?) =
    if (type == "morning_pickup") "Morning pickup" else "Afternoon drop-off"

private fun formatDate(value: String?): String = try {
    value?.let { LocalDate.parse(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) } ?: "Date pending"
} catch (_: DateTimeException) {
    value ?: "Date pending"
}

private fun formatTime(value: String?): String = try {
    value?.let { OffsetDateTime.parse(it).toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")) } ?: "—"
} catch (_: DateTimeException) {
    try {
        value?.let { Instant.parse(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) } ?: "—"
    } catch (_: DateTimeException) {
        "—"
    }
}

private fun formatWait(seconds: Int): String =
    if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s"
