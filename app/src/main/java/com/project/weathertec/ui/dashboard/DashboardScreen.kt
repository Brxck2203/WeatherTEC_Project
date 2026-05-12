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
import com.project.weathertec.ui.shared.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: WeatherViewModel = viewModel()) {
    val liveState by vm.liveConditions.collectAsState()
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
            // Live conditions section
            SectionTitle("Condiciones Actuales")
            when (val state = liveState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Success -> LiveConditionsCard(state.data)
                is UiState.Empty -> EmptyScreen("Sin datos en vivo")
                is UiState.Error -> ErrorScreen(state.message) { vm.loadDashboard() }
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            // Today stats section
            SectionTitle("Resumen de Hoy")
            when (val state = recordsState) {
                is UiState.Loading -> CircularProgressIndicator()
                is UiState.Success -> TodayStats(state.data)
                is UiState.Empty -> EmptyScreen("Sin registros para hoy")
                is UiState.Error -> ErrorScreen(state.message) { vm.loadDashboard() }
            }
        }
    }
}

@Composable
fun LiveConditionsCard(record: WeatherRecord) {
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
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    label = "Temperatura",
                    value = record.temperature?.let { "${it.roundToInt()}" } ?: "--",
                    unit = "°C",
                    modifier = Modifier.weight(1f).padding(end = 4.dp)
                )
                StatCard(
                    label = "Humedad",
                    value = record.humidity?.let { "${it.roundToInt()}" } ?: "--",
                    unit = "%",
                    modifier = Modifier.weight(1f).padding(start = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            StatCard(
                label = "Viento",
                value = record.windSpeed?.let { "${it.roundToInt()}" } ?: "--",
                unit = "km/h"
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "⏱️ ${record.date}  ${record.time}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TodayStats(records: List<WeatherRecord>) {
    val temps = records.mapNotNull { it.temperature }
    val humids = records.mapNotNull { it.humidity }
    val winds = records.mapNotNull { it.windSpeed }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Temp. Prom.",
            value = if (temps.isNotEmpty()) "${(temps.average()).let { "%.1f".format(it) }}" else "--",
            unit = "°C",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Hum. Prom.",
            value = if (humids.isNotEmpty()) "${(humids.average()).let { "%.1f".format(it) }}" else "--",
            unit = "%",
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Temp. Máx",
            value = temps.maxOrNull()?.let { "%.1f".format(it) } ?: "--",
            unit = "°C",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Temp. Mín",
            value = temps.minOrNull()?.let { "%.1f".format(it) } ?: "--",
            unit = "°C",
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(8.dp))
    StatCard(
        label = "Viento Prom.",
        value = if (winds.isNotEmpty()) "%.1f".format(winds.average()) else "--",
        unit = "km/h"
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "${records.size} registro(s) hoy",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    )
}
