package com.project.weathertec.ui.range

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.weathertec.ui.shared.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeAverageScreen(vm: WeatherViewModel = viewModel()) {
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    val recordsState by vm.records.collectAsState()
    var searched by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("📅 Promedio por Rango") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("Selecciona un rango de fechas")
            Text(
                text = "Formato: YYYY-MM-DD",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("Fecha inicio") },
                placeholder = { Text("2025-05-01") },
                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it },
                label = { Text("Fecha fin") },
                placeholder = { Text("2025-05-10") },
                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (startDate.length == 10 && endDate.length == 10) {
                        searched = true
                        vm.loadByDate(startDate, endDate)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = startDate.length == 10 && endDate.length == 10
            ) {
                Text("Calcular Promedio")
            }

            if (searched) {
                when (val state = recordsState) {
                    is UiState.Loading -> LoadingScreen()
                    is UiState.Empty -> EmptyScreen("Sin datos para ese rango")
                    is UiState.Error -> ErrorScreen(state.message)
                    is UiState.Success -> {
                        val records = state.data
                        val temps = records.mapNotNull { it.temperature }
                        val humids = records.mapNotNull { it.humidity }
                        val winds = records.mapNotNull { it.windSpeed }

                        Divider()
                        SectionTitle("Resultados: $startDate → $endDate")
                        Text(
                            "${records.size} registro(s) encontrado(s)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                "Temp. Prom.",
                                if (temps.isEmpty()) "--" else "%.1f".format(temps.average()),
                                "°C", modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                "Hum. Prom.",
                                if (humids.isEmpty()) "--" else "%.1f".format(humids.average()),
                                "%", modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard(
                                "Temp. Máx",
                                temps.maxOrNull()?.let { "%.1f".format(it) } ?: "--",
                                "°C", modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                "Temp. Mín",
                                temps.minOrNull()?.let { "%.1f".format(it) } ?: "--",
                                "°C", modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        StatCard(
                            "Viento Prom.",
                            if (winds.isEmpty()) "--" else "%.1f".format(winds.average()),
                            "km/h"
                        )
                    }
                }
            }
        }
    }
}
