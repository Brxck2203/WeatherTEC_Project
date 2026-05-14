package com.project.weathertec.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.weathertec.data.model.WeatherRecord
import com.project.weathertec.data.utils.StatsUtils
import com.project.weathertec.ui.shared.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: WeatherViewModel = viewModel()) {
    val latestState by vm.latestRecord.collectAsState()
    val recordsState by vm.records.collectAsState()

    LaunchedEffect(Unit) { vm.loadDashboard() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌡️ WeatherTEC", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { vm.loadDashboard() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionTitle("Última Lectura")
            when (val state = latestState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Success -> LatestRecordCard(state.data)
                is UiState.Empty   -> EmptyScreen("Sin datos disponibles para hoy.\nAsegúrate de que el servicio esté activo.")
                is UiState.Error   -> ErrorScreen(state.message) { vm.loadDashboard() }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            SectionTitle("Resumen de Hoy")
            when (val state = recordsState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Success -> TodayStats(state.data)
                is UiState.Empty   -> EmptyScreen("Sin registros para hoy")
                is UiState.Error   -> ErrorScreen(state.message) { vm.loadDashboard() }
            }
        }
    }
}

@Composable
fun LatestRecordCard(record: WeatherRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            WeatherIcon(record.conditions)
            Spacer(Modifier.height(8.dp))
            Text(
                text = record.conditions.ifEmpty { "Sin descripción" },
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Temperatura",
                    value = StatsUtils.formatValue(record.temperature),
                    unit = "°C",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Humedad",
                    value = StatsUtils.formatValue(record.humidity, 0),
                    unit = "%",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            StatCard(
                label = "Viento",
                value = StatsUtils.formatValue(record.windSpeed),
                unit = "km/h"
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "⏱️ ${StatsUtils.formatDate(record.date)}  ${record.time}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TodayStats(records: List<WeatherRecord>) {
    val tempStats = StatsUtils.calcStats(StatsUtils.getValues(records, "temperature"))
    val humStats  = StatsUtils.calcStats(StatsUtils.getValues(records, "humidity"))
    val windStats = StatsUtils.calcStats(StatsUtils.getValues(records, "windSpeed"))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            "Temp. Prom.",
            StatsUtils.formatValue(tempStats?.avg), "°C",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            "Hum. Prom.",
            StatsUtils.formatValue(humStats?.avg, 0), "%",
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            "Temp. Máx",
            StatsUtils.formatValue(tempStats?.max), "°C",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            "Temp. Mín",
            StatsUtils.formatValue(tempStats?.min), "°C",
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(8.dp))
    StatCard(
        "Viento Prom.",
        StatsUtils.formatValue(windStats?.avg), "km/h"
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "${records.size} registro(s) del día",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    )
}
