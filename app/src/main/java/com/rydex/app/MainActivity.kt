package com.rydex.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RydexTheme { RydexApp() } }
    }
}

private enum class Screen { HOME, PLAN, REVIEW, RIDE }

@Composable
private fun RydexApp(vm: RydexViewModel = viewModel()) {
    var screen by remember { mutableStateOf(Screen.HOME) }

    when (screen) {
        Screen.HOME -> HomeScreen(vm) { screen = Screen.PLAN }
        Screen.PLAN -> PreferenceScreen(
            vm = vm,
            onBack = { screen = Screen.HOME },
            onPlanned = { screen = Screen.REVIEW },
        )
        Screen.REVIEW -> ReviewScreen(
            vm = vm,
            onBack = { screen = Screen.PLAN },
            onStart = { screen = Screen.RIDE },
        )
        Screen.RIDE -> RideScreen(vm) { screen = Screen.HOME }
    }
}

@Composable
private fun HomeScreen(vm: RydexViewModel, onPlan: () -> Unit) {
    val context = LocalContext.current

    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result ->
            if (result.values.any { it }) vm.startLocationUpdates()
        }

    LaunchedEffect(Unit) {
        val granted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            vm.startLocationUpdates()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    Scaffold(topBar = { RydexTopBar() }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B0D))
                .padding(padding)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "PLAN YOUR RIDE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Text(
                "Build the route around you — fuel, food and weather.",
                color = Color(0xFFB8B8BF),
            )

            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFFFC857),
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "LIVE LOCATION",
                            color = Color(0xFF92929A),
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            vm.locationLabel,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = vm.destination,
                onValueChange = { vm.destination = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Where are you going?") },
                leadingIcon = {
                    Icon(Icons.Default.Place, contentDescription = null)
                },
                singleLine = true,
                supportingText = {
                    Text("Google Places search is used by the backend.")
                },
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AssistChip(onClick = { vm.destination = "Mumbai" }, label = { Text("Mumbai") })
                AssistChip(onClick = { vm.destination = "Pune" }, label = { Text("Pune") })
                AssistChip(onClick = { vm.destination = "Nashik" }, label = { Text("Nashik") })
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    if (vm.destination.isBlank()) {
                        Toast.makeText(
                            context,
                            "Enter a destination first",
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        onPlan()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Navigation, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("CUSTOMIZE RIDE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PreferenceScreen(
    vm: RydexViewModel,
    onBack: () -> Unit,
    onPlanned: () -> Unit,
) {
    Scaffold(topBar = { RydexTopBar("Ride Preferences", onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B0D))
                .padding(padding)
                .padding(18.dp),
        ) {
            Text(
                "Make the route work for you.",
                color = Color(0xFFB8B8BF),
            )

            Spacer(Modifier.height(18.dp))

            PreferenceToggle(
                icon = "⛽",
                title = "Need fuel?",
                selected = vm.needsFuel,
                onChange = { vm.needsFuel = it },
            )

            if (vm.needsFuel) {
                Text(
                    "Fuel priority",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("HIGH", "MEDIUM", "LOW").forEach { priority ->
                        FilterChip(
                            selected = vm.fuelPriority == priority,
                            onClick = { vm.fuelPriority = priority },
                            label = { Text(priority) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            PreferenceToggle(
                icon = "🍴",
                title = "Need food?",
                selected = vm.needsFood,
                onChange = { vm.needsFood = it },
            )

            if (vm.needsFood) {
                OutlinedTextField(
                    value = vm.foodTime,
                    onValueChange = { vm.foodTime = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    label = { Text("Preferred food time (HH:mm)") },
                    singleLine = true,
                )
            }

            Spacer(Modifier.height(16.dp))

            PreferenceToggle(
                icon = "🌦️",
                title = "Check weather every ~30 km",
                selected = vm.needsWeather,
                onChange = { vm.needsWeather = it },
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { vm.planTrip(onPlanned) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !vm.planning,
            ) {
                Text(
                    if (vm.planning) "PLANNING…" else "BUILD MY ROUTE",
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun ReviewScreen(
    vm: RydexViewModel,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val plan = vm.plan

    Scaffold(topBar = { RydexTopBar("Trip Preview", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B0D))
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                GlassCard {
                    Text(
                        plan?.destinationName ?: vm.destination,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        plan?.destinationAddress
                            ?: "Route generated from your current location",
                        color = Color(0xFF9C9CA4),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        Stat(
                            "DISTANCE",
                            plan?.distanceMeters?.let { "${it / 1000} km" } ?: "—",
                        )
                        Stat(
                            "RIDE TIME",
                            plan?.durationSeconds?.let { formatDuration(it) } ?: "—",
                        )
                    }
                }
            }

            item {
                Text(
                    "YOUR STOPS",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (plan?.stops.isNullOrEmpty()) {
                item {
                    GlassCard {
                        Text(
                            "No automatic stops required.",
                            color = Color(0xFFB8B8BF),
                        )
                    }
                }
            }

            items(plan?.stops.orEmpty()) { stop ->
                StopCard(stop)
            }

            item {
                Text(
                    "WEATHER",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            if (plan?.weather.isNullOrEmpty()) {
                item {
                    GlassCard {
                        Text(
                            "Weather checks are off.",
                            color = Color(0xFFB8B8BF),
                        )
                    }
                }
            }

            items(plan?.weather.orEmpty()) { weather ->
                WeatherCard(weather)
            }

            item {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "START RYDEX NAVIGATION",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun RideScreen(vm: RydexViewModel, onStop: () -> Unit) {
    val plan = vm.plan
    val route = remember(plan?.encodedPolyline) {
        plan?.encodedPolyline?.let(PolylineDecoder::decode).orEmpty()
    }

    LaunchedEffect(Unit) {
        vm.startLocationUpdates()
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            route.firstOrNull() ?: LatLng(19.9, 73.8),
            11f,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = vm.hasLocationPermission),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                compassEnabled = false,
                myLocationButtonEnabled = true,
            ),
        ) {
            if (route.isNotEmpty()) {
                Polyline(points = route, width = 10f)
            }

            plan?.stops?.forEach { stop ->
                Marker(
                    state = MarkerState(position = LatLng(stop.lat, stop.lng)),
                    title = stop.name,
                    snippet = stop.address,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CockpitPill("RYDEX")
                CockpitPill(vm.currentSpeedKmh)
            }

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                Text(
                    vm.nextInstruction ?: "Follow the highlighted route",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    SmallMetric("SPEED", vm.currentSpeedKmh)
                    SmallMetric("DEST", plan?.destinationName ?: "—")
                }

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    vm.nextFuel()?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text("⛽ ${it.name}") },
                        )
                    }

                    vm.nextFood()?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text("🍴 ${it.name}") },
                        )
                    }

                    vm.currentWeatherRecommendation()?.let {
                        AssistChip(
                            onClick = {},
                            label = { Text("🌧️ Weather") },
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("END RIDE")
                }
            }
        }
    }
}

@Composable
private fun PreferenceToggle(
    icon: String,
    title: String,
    selected: Boolean,
    onChange: (Boolean) -> Unit,
) {
    GlassCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (selected) "Included in trip planning" else "Skipped",
                    color = Color(0xFF8F8F98),
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            androidx.compose.material3.Switch(
                checked = selected,
                onCheckedChange = onChange,
            )
        }
    }
}

@Composable
private fun StopCard(stop: Stop) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (stop.type == "fuel") {
                    Icons.Default.LocalGasStation
                } else {
                    Icons.Default.Fastfood
                },
                contentDescription = null,
                tint = if (stop.type == "fuel") Color(0xFFFFC857) else Color(0xFF8ED081),
            )

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    stop.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stop.address ?: "",
                    color = Color(0xFF92929A),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                stop.reason?.let {
                    Text(
                        it,
                        color = Color(0xFF72727A),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }

            stop.rating?.let {
                Text(
                    "★ ${"%.1f".format(it)}",
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun WeatherCard(item: WeatherCheckpoint) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Cloud,
                contentDescription = null,
                tint = Color(0xFF89CFF0),
            )

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "${item.kmFromStart.toInt()} km ahead",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    item.recommendation,
                    color = Color(0xFFB8B8BF),
                )
                Text(
                    "Arrival ${item.arrivalTime.take(16).replace('T', ' ')}",
                    color = Color(0xFF777781),
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            item.precipitationProbability?.let {
                Text(
                    "$it%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(
            label,
            color = Color(0xFF777781),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SmallMetric(label: String, value: String) {
    Column {
        Text(
            label,
            color = Color(0xFF73737B),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            value,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CockpitPill(text: String) {
    Surface(
        color = Color(0xCC101014),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(
            text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 9.dp,
            ),
        )
    }
}

@Composable
private fun RydexTopBar(title: String = "RYDEX", onBack: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (onBack != null) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier
                        .clickable { onBack() }
                        .padding(14.dp),
                )
            } else {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp),
                )
            }
        },
    )
}

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF151519),
        ),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun RydexTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return "${h}h ${m}m"
}
