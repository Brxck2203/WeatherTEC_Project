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
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                }
            )
        }
    ) { padding ->
        when (val state = recordsState) {
            is UiState.Loading -> LoadingScreen()
            is UiState.Empty -> EmptyScreen("Sin datos para hoy")
            is UiState.Error -> ErrorScreen(state.message) { vm.loadDashboard() }
            is UiState.Success -> {
                // Group by hour
                val hourlyGroups = state.data
                    .groupBy { it.time.take(2) }
                    .toSortedMap()

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    item {
                        SectionTitle("Hoy — ${vm.today} (${hourlyGroups.size} horas)")
                    }
                    items(hourlyGroups.entries.toList()) { (hour, recs) ->
                        val temps = recs.mapNotNull { it.temperature }
                        val humids = recs.mapNotNull { it.humidity }
                        val winds = recs.mapNotNull { it.windSpeed }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "${hour}:00",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    DataCell("Temp", temps.average().let { "%.1f°C".format(it) })
                                    DataCell("Humedad", humids.average().let { "%.1f%%".format(it) })
                                    DataCell("Viento", winds.average().let { "%.1f km/h".format(it) })
                                    DataCell("Registros", "${recs.size}")
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
