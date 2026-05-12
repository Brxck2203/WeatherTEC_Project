package com.project.weathertec.ui.hourly

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.weathertec.data.utils.StatsUtils
import com.project.weathertec.ui.shared.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HourlyAverageScreen(vm: WeatherViewModel = viewModel()) {
    val recordsState by vm.records.collectAsState()

    LaunchedEffect(Unit) { vm.loadDashboard() }

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
            is UiState.Empty   -> EmptyScreen("Sin datos para hoy en Firebase")
            is UiState.Error   -> ErrorScreen(state.message) { vm.loadDashboard() }
            is UiState.Success -> {
                val records = state.data
                val hourlyTemp  = StatsUtils.groupByHour(records, "temperature")
                val hourlyHumid = StatsUtils.groupByHour(records, "humidity")
                val hourlyWind  = StatsUtils.groupByHour(records, "windSpeed")

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    item {
                        SectionTitle("Hoy — ${vm.today} (${hourlyTemp.size} horas)")
                    }
                    items(hourlyTemp) { group ->
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
                                    DataCell("Temp",
                                        StatsUtils.formatValue(group.stats?.avg) + "°C")
                                    DataCell("Humedad",
                                        StatsUtils.formatValue(humid?.stats?.avg, 0) + "%")
                                    DataCell("Viento",
                                        StatsUtils.formatValue(wind?.stats?.avg) + " km/h")
                                    DataCell("Registros", "${group.stats?.count ?: 0}")
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
