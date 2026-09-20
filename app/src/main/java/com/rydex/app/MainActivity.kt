package com.rydex.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.ComposeMapColorScheme
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private object RydexColors {
    val background = Color(0xFF070A0D)
    val surface = Color(0xFF0E1318)
    val surfaceHigh = Color(0xFF151C22)
    val onBackground = Color(0xFFF3F7FA)
    val onSurface = Color(0xFFF3F7FA)
    val onSurfaceVariant = Color(0xFFB8C3CC)
    val outline = Color(0xFF3A4650)
    val primary = Color(0xFF64D8EE)
    val onPrimary = Color(0xFF00262D)
    val secondary = Color(0xFFA8B9FF)
    val success = Color(0xFF6FE0A8)
    val warning = Color(0xFFFFD166)
    val error = Color(0xFFFF8C8C)
    val info = Color(0xFF79BFFF)

    val lightBackground = Color(0xFFF5F7F9)
    val lightSurface = Color(0xFFFFFFFF)
    val lightSurfaceHigh = Color(0xFFEDF2F5)
    val lightOnBackground = Color(0xFF101419)
    val lightOnSurface = Color(0xFF101419)
    val lightOnSurfaceVariant = Color(0xFF4D5963)
    val lightOutline = Color(0xFF7A8791)
    val lightPrimary = Color(0xFF00687A)
}

private enum class Screen { HOME, PLAN, REVIEW, RIDE }

private fun isMapsConfigured(): Boolean = BuildConfig.MAPS_API_KEY.isNotBlank()

private fun isEmulatorBackendUrl(): Boolean =
    BuildConfig.BACKEND_URL.contains("10.0.2.2")

private data class RydexWindowInfo(
    val widthDp: Int,
    val heightDp: Int,
) {
    val isCompactWidth: Boolean get() = widthDp < 600
    val isMediumWidth: Boolean get() = widthDp in 600..839
    val isExpandedWidth: Boolean get() = widthDp >= 840
    val isLandscape: Boolean get() = widthDp > heightDp
    val isCompactHeight: Boolean get() = heightDp < 480
    val useSplitLayout: Boolean get() = widthDp >= 600
}

@Composable
private fun rememberRydexWindowInfo(): RydexWindowInfo {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp
    val height = configuration.screenHeightDp
    return remember(width, height) {
        RydexWindowInfo(width, height)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RydexTheme {
                RydexApp()
            }
        }
    }
}

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
    val window = rememberRydexWindowInfo()

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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            RydexTopBar(
                title = "RYDEX",
                subtitle = "Motorcycle ride planner",
            )
        },
    ) { padding ->
        if (window.useSplitLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .widthIn(max = 1200.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                HomePlanPane(
                    vm = vm,
                    onPlan = onPlan,
                    modifier = Modifier.weight(1.15f),
                )
                HomeStatusPane(
                    vm = vm,
                    modifier = Modifier.weight(0.85f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = 18.dp,
                    end = 18.dp,
                    bottom = 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { HomeHero() }
                item {
                    DestinationField(
                        vm = vm,
                        onPlan = {
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
                    )
                }
                item { QuickDestinations(vm) }
                item { HomeStatusPane(vm) }
            }
        }
    }
}

@Composable
private fun HomePlanPane(
    vm: RydexViewModel,
    onPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        HomeHero()
        DestinationField(vm, onPlan)
        QuickDestinations(vm)
    }
}

@Composable
private fun HomeHero() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow("RIDE PLANNER")
        Text(
            "Plan a ride that\nfeels effortless.",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            lineHeight = 42.sp,
        )
        Text(
            "RYDEX builds the route around you — including fuel, food and weather.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 560.dp),
        )
    }
}

@Composable
private fun DestinationField(
    vm: RydexViewModel,
    onPlan: () -> Unit,
) {
    RydexCard(
        modifier = Modifier.fillMaxWidth(),
        tone = CardTone.High,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Where are you going?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )

            OutlinedTextField(
                value = vm.destination,
                onValueChange = { vm.destination = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Destination") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = "Destination",
                    )
                },
                trailingIcon = {
                    if (vm.destination.isNotBlank()) {
                        TextButton(onClick = { vm.destination = "" }) {
                            Text("Clear")
                        }
                    }
                },
                supportingText = {
                    Text(
                        "Search uses the RYDEX planning backend.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
            )

            Button(
                onClick = onPlan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Icon(Icons.Default.Route, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Customize ride", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickDestinations(vm: RydexViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Quick destinations",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("Pune", "Nashik", "Mumbai").forEach { destination ->
                AssistChip(
                    onClick = { vm.destination = destination },
                    label = { Text(destination) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun HomeStatusPane(
    vm: RydexViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RydexCard(
            modifier = Modifier.fillMaxWidth(),
            tone = CardTone.High,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Eyebrow("LIVE STATUS")
                        Text(
                            "Bike-ready cockpit",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    StatusPill(
                        text = if (vm.hasLocationPermission) "GPS ready" else "GPS off",
                        icon = Icons.Default.GpsFixed,
                        tint = if (vm.hasLocationPermission) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            RydexColors.warning
                        },
                    )
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DashboardMetric(
                        label = "Speed",
                        value = vm.currentSpeedKmh,
                        icon = Icons.Default.Speed,
                        modifier = Modifier.weight(1f),
                    )
                    DashboardMetric(
                        label = "Location",
                        value = vm.locationLabel,
                        icon = Icons.Default.LocationOn,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        if (!isMapsConfigured() || isEmulatorBackendUrl()) {
            RydexCard(
                modifier = Modifier.fillMaxWidth(),
                tone = CardTone.High,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("DEVICE SETUP", color = RydexColors.warning)
                    if (!isMapsConfigured()) {
                        Text(
                            "Google Maps key missing",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "This APK was built without a Google Maps Android key. Map screens cannot display Google Maps until the key is supplied.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (isEmulatorBackendUrl()) {
                        Text(
                            "Planner URL is emulator-only",
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Text(
                            "10.0.2.2 points to the Android emulator host. A physical phone needs your computer LAN address or a deployed HTTPS backend.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        RydexCard(
            modifier = Modifier.fillMaxWidth(),
            tone = CardTone.Low,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = RydexColors.info,
                )
                Column {
                    Text("Tip", fontWeight = FontWeight.Bold)
                    Text(
                        "Start planning from a location with GPS permission enabled for the best route.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureLine(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(
            description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun DashboardMetric(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            value,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PreferenceScreen(
    vm: RydexViewModel,
    onBack: () -> Unit,
    onPlanned: () -> Unit,
) {
    val window = rememberRydexWindowInfo()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            RydexTopBar(
                title = "Ride preferences",
                subtitle = "Build your route profile",
                onBack = onBack,
            )
        },
    ) { padding ->
        if (window.useSplitLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 18.dp)
                    .widthIn(max = 1200.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                PreferenceOptions(
                    vm = vm,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                )
                PreferenceSummary(
                    vm = vm,
                    modifier = Modifier.weight(0.75f),
                    onPlanned = onPlanned,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(18.dp, 4.dp, 18.dp, 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { PreferenceIntro() }
                item { PreferenceOptions(vm) }
                item { PreferenceSummary(vm, onPlanned = onPlanned) }
            }
        }
    }
}

@Composable
private fun PreferenceIntro() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Eyebrow("ROUTE PROFILE")
        Text(
            "Tell RYDEX what matters.",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "These choices shape the stops and weather checks added to your route.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreferenceOptions(
    vm: RydexViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PreferenceCard(
            icon = Icons.Default.LocalGasStation,
            title = "Fuel stop",
            description = "Add a refuelling stop based on your selected priority.",
            selected = vm.needsFuel,
            tint = RydexColors.warning,
            onChange = { vm.needsFuel = it },
        )

        if (vm.needsFuel) {
            RydexCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Fuel priority", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("HIGH", "MEDIUM", "LOW").forEach { priority ->
                            FilterChip(
                                selected = vm.fuelPriority == priority,
                                onClick = { vm.fuelPriority = priority },
                                label = {
                                    Text(
                                        priority.lowercase()
                                            .replaceFirstChar { it.uppercase() },
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        PreferenceCard(
            icon = Icons.Default.Fastfood,
            title = "Food stop",
            description = "Add a food stop close to your preferred time.",
            selected = vm.needsFood,
            tint = RydexColors.success,
            onChange = { vm.needsFood = it },
        )

        if (vm.needsFood) {
            RydexCard {
                OutlinedTextField(
                    value = vm.foodTime,
                    onValueChange = { vm.foodTime = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Preferred food time") },
                    supportingText = {
                        Text("24-hour format, for example 13:30.")
                    },
                    singleLine = true,
                )
            }
        }

        PreferenceCard(
            icon = Icons.Default.Cloud,
            title = "Weather checkpoints",
            description = "Check conditions along the route and flag rain risk.",
            selected = vm.needsWeather,
            tint = RydexColors.info,
            onChange = { vm.needsWeather = it },
        )
    }
}

@Composable
private fun PreferenceCard(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    tint: Color,
    onChange: (Boolean) -> Unit,
) {
    RydexCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(!selected) },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = tint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    if (selected) "Included" else "Skipped",
                    color = if (selected) tint else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = selected,
                onCheckedChange = onChange,
                modifier = Modifier.size(48.dp),
            )
        }
    }
}

@Composable
private fun PreferenceSummary(
    vm: RydexViewModel,
    modifier: Modifier = Modifier,
    onPlanned: () -> Unit,
) {
    RydexCard(
        modifier = modifier.fillMaxWidth(),
        tone = CardTone.High,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Eyebrow("AT A GLANCE")
            Text(
                "Your route profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            SummaryLine(
                icon = Icons.Default.LocalGasStation,
                title = "Fuel",
                value = if (vm.needsFuel) {
                    vm.fuelPriority.lowercase()
                        .replaceFirstChar { it.uppercase() }
                } else {
                    "Off"
                },
                tint = RydexColors.warning,
            )
            SummaryLine(
                icon = Icons.Default.Fastfood,
                title = "Food",
                value = if (vm.needsFood) vm.foodTime else "Off",
                tint = RydexColors.success,
            )
            SummaryLine(
                icon = Icons.Default.Cloud,
                title = "Weather",
                value = if (vm.needsWeather) "Every ~30 km" else "Off",
                tint = RydexColors.info,
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
            )

            BuildRouteButton(vm, onPlanned)
        }
    }
}

@Composable
private fun SummaryLine(
    icon: ImageVector,
    title: String,
    value: String,
    tint: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BuildRouteButton(
    vm: RydexViewModel,
    onPlanned: () -> Unit,
) {
    Button(
        onClick = { vm.planTrip(onPlanned) },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = !vm.planning,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(
            if (vm.planning) "Building route…" else "Build my route",
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ReviewScreen(
    vm: RydexViewModel,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val plan = vm.plan
    val window = rememberRydexWindowInfo()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            RydexTopBar(
                title = "Trip preview",
                subtitle = plan?.destinationName ?: "Route overview",
                onBack = onBack,
            )
        },
    ) { padding ->
        if (window.useSplitLayout) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp, vertical = 18.dp)
                    .widthIn(max = 1280.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                ReviewMainPane(
                    vm = vm,
                    plan = plan,
                    onStart = onStart,
                    modifier = Modifier.weight(1.2f),
                )
                ReviewDetailPane(
                    plan = plan,
                    modifier = Modifier.weight(0.8f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(18.dp, 6.dp, 18.dp, 30.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { TripSummaryCard(vm, plan) }
                item {
                    TripMapPreview(
                        plan,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                    )
                }
                item { StopSection(plan) }
                item { WeatherSection(plan) }
                item { StartNavigationButton(onStart) }
            }
        }
    }
}

@Composable
private fun ReviewMainPane(
    vm: RydexViewModel,
    plan: TripPlan?,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TripSummaryCard(vm, plan)
        TripMapPreview(
            plan,
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp),
        )
        StartNavigationButton(onStart)
    }
}

@Composable
private fun ReviewDetailPane(
    plan: TripPlan?,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { StopSection(plan) }
        item { WeatherSection(plan) }
    }
}

@Composable
private fun TripSummaryCard(
    vm: RydexViewModel,
    plan: TripPlan?,
) {
    RydexCard(
        modifier = Modifier.fillMaxWidth(),
        tone = CardTone.High,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Eyebrow("TRIP PREVIEW")
            Text(
                plan?.destinationName ?: vm.destination,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                plan?.destinationAddress
                    ?: "Route generated from your current location",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RideStat(
                    label = "Distance",
                    value = plan?.distanceMeters?.let(::formatDistance) ?: "—",
                    modifier = Modifier.weight(1f),
                )
                RideStat(
                    label = "Ride time",
                    value = plan?.durationSeconds?.let(::formatDuration) ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RideStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                value,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun TripMapPreview(
    plan: TripPlan?,
    modifier: Modifier = Modifier,
) {
    val route = remember(plan?.encodedPolyline) {
        plan?.encodedPolyline?.let(PolylineDecoder::decode).orEmpty()
    }

    if (route.isEmpty()) {
        RydexCard(modifier = modifier) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Map preview unavailable",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        return
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(route.first(), 10.5f)
    }

    RydexCard(
        modifier = modifier,
        contentPadding = 0.dp,
    ) {
        Box(Modifier.fillMaxSize()) {
            if (isMapsConfigured()) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    contentDescription = "Trip route map",
                    mapColorScheme = ComposeMapColorScheme.FOLLOW_SYSTEM,
                    properties = MapProperties(),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        compassEnabled = false,
                        myLocationButtonEnabled = false,
                        mapToolbarEnabled = false,
                    ),
                ) {
                    Polyline(
                        points = route,
                        width = 11f,
                        color = Color(0x66000000),
                    )
                    Polyline(
                        points = route,
                        width = 7f,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    plan?.stops?.forEach { stop ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(stop.lat, stop.lng),
                            ),
                            title = stop.name,
                            snippet = stop.address,
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            tint = RydexColors.warning,
                        )
                        Text(
                            "Google Maps setup required",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Add an Android-restricted Maps API key to the build.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 9.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        formatDistance(plan?.distanceMeters ?: 0),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun StopSection(plan: TripPlan?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            title = "Stops",
            supporting = "Automatically added to your ride",
        )

        if (plan?.stops.isNullOrEmpty()) {
            RydexCard {
                Text(
                    "No automatic stops required.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            plan?.stops.orEmpty().forEach { stop ->
                StopCard(stop)
            }
        }
    }
}

@Composable
private fun WeatherSection(plan: TripPlan?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionTitle(
            title = "Weather",
            supporting = "Conditions along the route",
        )

        if (plan?.weather.isNullOrEmpty()) {
            RydexCard {
                Text(
                    "Weather checks are off.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            plan?.weather.orEmpty().forEach { weather ->
                WeatherCard(weather)
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    supporting: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            supporting,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StopCard(stop: Stop) {
    val tint = if (stop.type == "fuel") {
        RydexColors.warning
    } else {
        RydexColors.success
    }

    RydexCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = tint.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (stop.type == "fuel") {
                            Icons.Default.LocalGasStation
                        } else {
                            Icons.Default.Fastfood
                        },
                        contentDescription = if (stop.type == "fuel") {
                            "Fuel stop"
                        } else {
                            "Food stop"
                        },
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    stop.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stop.address ?: "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                stop.reason?.let {
                    Text(
                        it,
                        color = tint,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            stop.rating?.let {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "★",
                        color = RydexColors.warning,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "%.1f".format(it),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(item: WeatherCheckpoint) {
    RydexCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                color = RydexColors.info.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = "Weather",
                        tint = RydexColors.info,
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    "${item.kmFromStart.toInt()} km ahead",
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    item.recommendation,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Arrival ${item.arrivalTime.take(16).replace('T', ' ')}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            item.precipitationProbability?.let {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = RydexColors.info,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        "$it%",
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun StartNavigationButton(onStart: () -> Unit) {
    Button(
        onClick = onStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Icon(
            Icons.Default.Navigation,
            contentDescription = null,
        )
        Spacer(Modifier.width(8.dp))
        Text("Start RYDEX navigation", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun RideScreen(
    vm: RydexViewModel,
    onStop: () -> Unit,
) {
    val plan = vm.plan
    val window = rememberRydexWindowInfo()
    val route = remember(plan?.encodedPolyline) {
        plan?.encodedPolyline?.let(PolylineDecoder::decode).orEmpty()
    }
    val coroutineScope = rememberCoroutineScope()

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
        if (isMapsConfigured()) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                contentDescription = "RYDEX live navigation map",
                mapColorScheme = ComposeMapColorScheme.DARK,
                properties = MapProperties(
                    isMyLocationEnabled = vm.hasLocationPermission,
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    compassEnabled = false,
                    myLocationButtonEnabled = false,
                    mapToolbarEnabled = false,
                ),
            ) {
                if (route.isNotEmpty()) {
                    Polyline(
                        points = route,
                        width = 12f,
                        color = Color(0x77000000),
                    )
                    Polyline(
                        points = route,
                        width = 8f,
                        color = RydexColors.primary,
                    )
                }

                plan?.stops?.forEach { stop ->
                    Marker(
                        state = MarkerState(
                            position = LatLng(stop.lat, stop.lng),
                        ),
                        title = stop.name,
                        snippet = stop.address,
                    )
                }
            }
        } else {
            Surface(
                color = Color(0xF20D1318),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        tint = RydexColors.warning,
                    )
                    Text(
                        "Google Maps setup required",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Add an Android-restricted Maps API key to the build.",
                        color = Color(0xFFB9C3CB),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        if (window.isLandscape && window.widthDp >= 600) {
            Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(0.68f))
                RideSidePanel(
                    vm = vm,
                    plan = plan,
                    onStop = onStop,
                    modifier = Modifier.weight(0.32f),
                )
            }
        } else {
            RideTopOverlay(vm = vm, plan = plan)
            RideBottomPanel(
                vm = vm,
                plan = plan,
                onStop = onStop,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        Surface(
            color = Color(0xE61A2025),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 14.dp),
        ) {
            IconButton(
                onClick = {
                    vm.currentLocation?.let { location ->
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(location.lat, location.lng),
                                    14f,
                                ),
                            )
                        }
                    }
                },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "Center on my location",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun RideTopOverlay(
    vm: RydexViewModel,
    plan: TripPlan?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RideTopPill(
            label = "RYDEX",
            value = plan?.destinationName ?: "Navigation",
        )
        Spacer(Modifier.weight(1f))
        RideTopPill(
            label = "SPEED",
            value = vm.currentSpeedKmh,
        )
    }
}

@Composable
private fun RideTopPill(
    label: String,
    value: String,
) {
    Surface(
        color = Color(0xEA10161B),
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 13.dp,
                vertical = 9.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                label,
                color = Color(0xFFB9C3CB),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                value,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RideBottomPanel(
    vm: RydexViewModel,
    plan: TripPlan?,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(12.dp),
        color = Color(0xF20D1318),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                vm.nextInstruction ?: "Follow the highlighted route",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RideOverlayMetric(
                    label = "Speed",
                    value = vm.currentSpeedKmh,
                    modifier = Modifier.weight(1f),
                )
                RideOverlayMetric(
                    label = "Distance",
                    value = plan?.distanceMeters?.let(::formatDistance) ?: "—",
                    modifier = Modifier.weight(1f),
                )
                RideOverlayMetric(
                    label = "ETA",
                    value = plan?.durationSeconds?.let(::formatDuration) ?: "—",
                    modifier = Modifier.weight(1f),
                )
            }

            RideContextChips(vm)

            Button(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF24303A),
                    contentColor = Color.White,
                ),
            ) {
                Text("End ride", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RideSidePanel(
    vm: RydexViewModel,
    plan: TripPlan?,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = Color(0xF20C1217),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Eyebrow("LIVE RIDE", color = Color(0xFFB9C3CB))
            Text(
                plan?.destinationName ?: "Navigation",
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Surface(
                color = Color(0xE41A2229),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        vm.nextInstruction ?: "Follow the highlighted route",
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Next instruction",
                        color = Color(0xFFB9C3CB),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                RideOverlayMetric(
                    label = "Speed",
                    value = vm.currentSpeedKmh,
                    modifier = Modifier.weight(1f),
                    dark = true,
                )
                RideOverlayMetric(
                    label = "Ride",
                    value = plan?.durationSeconds?.let(::formatDuration) ?: "—",
                    modifier = Modifier.weight(1f),
                    dark = true,
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

            Text(
                "On route",
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )

            vm.nextFuel()?.let {
                RideContextRow(
                    icon = Icons.Default.LocalGasStation,
                    title = it.name,
                    supporting = "Fuel stop",
                    tint = RydexColors.warning,
                )
            }

            vm.nextFood()?.let {
                RideContextRow(
                    icon = Icons.Default.Fastfood,
                    title = it.name,
                    supporting = "Food stop",
                    tint = RydexColors.success,
                )
            }

            vm.currentWeatherRecommendation()?.let {
                RideContextRow(
                    icon = Icons.Default.Cloud,
                    title = "Weather checkpoint",
                    supporting = it,
                    tint = RydexColors.info,
                )
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF24303A),
                    contentColor = Color.White,
                ),
            ) {
                Text("End ride", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RideContextChips(vm: RydexViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        vm.nextFuel()?.let {
            AssistChip(
                onClick = {},
                label = { Text("Fuel") },
                leadingIcon = {
                    Icon(
                        Icons.Default.LocalGasStation,
                        contentDescription = null,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0x33303A42),
                    labelColor = Color.White,
                    leadingIconContentColor = RydexColors.warning,
                ),
            )
        }

        vm.nextFood()?.let {
            AssistChip(
                onClick = {},
                label = { Text("Food") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Fastfood,
                        contentDescription = null,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0x33303A42),
                    labelColor = Color.White,
                    leadingIconContentColor = RydexColors.success,
                ),
            )
        }

        if (vm.currentWeatherRecommendation() != null) {
            AssistChip(
                onClick = {},
                label = { Text("Weather") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Cloud,
                        contentDescription = null,
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = Color(0x33303A42),
                    labelColor = Color.White,
                    leadingIconContentColor = RydexColors.info,
                ),
            )
        }
    }
}

@Composable
private fun RideContextRow(
    icon: ImageVector,
    title: String,
    supporting: String,
    tint: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(21.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                supporting,
                color = Color(0xFFB9C3CB),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RideOverlayMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    dark: Boolean = false,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            label,
            color = if (dark) Color(0xFFB9C3CB) else Color(0xFFD4DCE1),
            style = MaterialTheme.typography.labelSmall,
        )
        Text(
            value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private enum class CardTone {
    Low,
    High,
}

@Composable
private fun RydexCard(
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.Low,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val container = if (tone == CardTone.High) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.36f),
        ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(contentPadding)) {
            content()
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    icon: ImageVector,
    tint: Color,
) {
    Surface(
        color = tint.copy(alpha = 0.12f),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 7.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text,
                color = tint,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun Eyebrow(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RydexTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
) {
    TopAppBar(
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, fontWeight = FontWeight.Bold)
                subtitle?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            } else {
                IconButton(
                    onClick = {},
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = "RYDEX",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun RydexTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        androidx.compose.material3.darkColorScheme(
            primary = RydexColors.primary,
            onPrimary = RydexColors.onPrimary,
            background = RydexColors.background,
            onBackground = RydexColors.onBackground,
            surface = RydexColors.surface,
            onSurface = RydexColors.onSurface,
            surfaceVariant = RydexColors.surfaceHigh,
            onSurfaceVariant = RydexColors.onSurfaceVariant,
            outline = RydexColors.outline,
            secondary = RydexColors.secondary,
            error = RydexColors.error,
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = RydexColors.lightPrimary,
            onPrimary = Color.White,
            background = RydexColors.lightBackground,
            onBackground = RydexColors.lightOnBackground,
            surface = RydexColors.lightSurface,
            onSurface = RydexColors.lightOnSurface,
            surfaceVariant = RydexColors.lightSurfaceHigh,
            onSurfaceVariant = RydexColors.lightOnSurfaceVariant,
            outline = RydexColors.lightOutline,
            secondary = RydexColors.secondary,
            error = RydexColors.error,
        )
    }

    val view = LocalView.current
    val context = LocalContext.current
    SideEffect {
        context.findActivity()?.let { activity ->
            val window = activity.window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

private fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}

private fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun formatDistance(meters: Int): String {
    val km = meters / 1000.0
    return if (km >= 10) {
        "${"%.0f".format(km)} km"
    } else {
        "${"%.1f".format(km)} km"
    }
}
