package com.project.weathertec.ui.hourly

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.weathertec.data.utils.StatsUtils
import com.project.weathertec.ui.shared.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HourlyAverageScreen(vm: WeatherViewModel = viewModel()) {
    val recordsState by vm.records.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showTemp  by remember { mutableStateOf(true) }
    var showHum   by remember { mutableStateOf(true) }
    var showWind  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadDashboard() }

    val tabs = listOf("Por variable", "Comparativa", "Tabla")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⏰ Promedio por Hora") },
                actions = {
                    IconButton(onClick = { vm.loadDashboard() }) {
                        Icon(Icons.Default.Refresh, "Refrescar")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = recordsState) {
            is UiState.Loading -> LoadingScreen()
            is UiState.Empty   -> EmptyScreen("Sin datos disponibles para hoy")
            is UiState.Error   -> ErrorScreen(state.message) { vm.loadDashboard() }
            is UiState.Success -> {
                val records = state.data
                val hourlyTemp  = StatsUtils.groupByHour(records, "temperature")
                val hourlyHumid = StatsUtils.groupByHour(records, "humidity")
                val hourlyWind  = StatsUtils.groupByHour(records, "windSpeed")

                val tempPoints  = hourlyTemp.map  { it.hour.takeLast(5) to (it.stats?.avg ?: 0.0) }
                val humPoints   = hourlyHumid.map { it.hour.takeLast(5) to (it.stats?.avg ?: 0.0) }
                val windPoints  = hourlyWind.map  { it.hour.takeLast(5) to (it.stats?.avg ?: 0.0) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    TabRow(selectedTabIndex = selectedTab) {
                        tabs.forEachIndexed { i, title ->
                            Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) })
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SectionTitle("Hoy — ${vm.today} (${hourlyTemp.size} horas)")

                        when (selectedTab) {
                            // Tab 0: gráfica por variable seleccionada
                            0 -> {
                                var varTab by remember { mutableIntStateOf(0) }
                                TabRow(selectedTabIndex = varTab, modifier = Modifier.fillMaxWidth()) {
                                    listOf("Temperatura", "Humedad", "Viento").forEachIndexed { i, t ->
                                        Tab(selected = varTab == i, onClick = { varTab = i }, text = { Text(t) })
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                when (varTab) {
                                    0 -> SingleLineChart(
                                        values = tempPoints,
                                        color = Color(0xFFE57373),
                                        title = "Temperatura promedio (°C) por hora"
                                    )
                                    1 -> SingleLineChart(
                                        values = humPoints,
                                        color = Color(0xFF64B5F6),
                                        title = "Humedad promedio (%) por hora"
                                    )
                                    2 -> SingleLineChart(
                                        values = windPoints,
                                        color = Color(0xFF81C784),
                                        title = "Viento promedio (km/h) por hora"
                                    )
                                }
                            }

                            // Tab 1: gráfica comparativa con checkboxes
                            1 -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CheckboxLabel("Temp", showTemp, Color(0xFFE57373)) { showTemp = it }
                                    CheckboxLabel("Humedad", showHum, Color(0xFF64B5F6)) { showHum = it }
                                    CheckboxLabel("Viento", showWind, Color(0xFF81C784)) { showWind = it }
                                }
                                MultiLineChart(
                                    tempValues = tempPoints,
                                    humValues = humPoints,
                                    windValues = windPoints,
                                    showTemp = showTemp,
                                    showHum = showHum,
                                    showWind = showWind
                                )
                            }

                            // Tab 2: tabla original
                            2 -> {
                                hourlyTemp.forEach { group ->
                                    val humid = hourlyHumid.find { it.hour == group.hour }
                                    val wind  = hourlyWind.find { it.hour == group.hour }
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(group.hour, style = MaterialTheme.typography.titleMedium)
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                DataCell("Temp", StatsUtils.formatValue(group.stats?.avg) + "°C")
                                                DataCell("Humedad", StatsUtils.formatValue(humid?.stats?.avg, 0) + "%")
                                                DataCell("Viento", StatsUtils.formatValue(wind?.stats?.avg) + " km/h")
                                                DataCell("Reg.", "${group.stats?.count ?: 0}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckboxLabel(label: String, checked: Boolean, color: Color, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChecked,
            colors = CheckboxDefaults.colors(checkedColor = color))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun DataCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}
