package com.project.weathertec.ui.historical

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.weathertec.data.model.WeatherRecord
import com.project.weathertec.data.utils.StatsUtils
import com.project.weathertec.ui.shared.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalScreen(vm: WeatherViewModel = viewModel()) {
    var selectedDate by remember { mutableStateOf("") }
    val recordsState by vm.records.collectAsState()
    var searched by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("📊 Histórico") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("Registros por fecha")
            OutlinedTextField(
                value = selectedDate,
                onValueChange = { selectedDate = it },
                label = { Text("Fecha (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (selectedDate.length == 10) {
                        searched = true
                        vm.loadByDate(selectedDate)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedDate.length == 10
            ) {
                Text("Buscar")
            }

            if (searched) {
                when (val state = recordsState) {
                    is UiState.Loading -> LoadingScreen()
                    is UiState.Empty   -> EmptyScreen("Sin registros para ${StatsUtils.formatDate(selectedDate)}")
                    is UiState.Error   -> ErrorScreen(state.message)
                    is UiState.Success -> {
                        SectionTitle("${state.data.size} registro(s) — ${StatsUtils.formatDate(selectedDate)}")
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf("Hora", "Temp.", "Hum.", "Viento").forEach {
                                        Text(it, style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                }
                            }
                            items(state.data) { record -> HistoricalRow(record) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoricalRow(record: WeatherRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(record.time, style = MaterialTheme.typography.bodyMedium)
            Text(
                StatsUtils.formatValue(record.temperature) + "°C",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                StatsUtils.formatValue(record.humidity, 0) + "%",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                StatsUtils.formatValue(record.windSpeed) + " km/h",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
