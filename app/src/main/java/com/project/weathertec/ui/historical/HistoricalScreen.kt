package com.project.weathertec.ui.historical

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("📊 Histórico") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        val records = state.data
                        val tempVals = records.mapNotNull { it.temperature }
                        val humVals  = records.mapNotNull { it.humidity }
                        val windVals = records.mapNotNull { it.windSpeed }

                        // Panel resumen máx/mín/prom
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard("T. Prom", "%.1f".format(tempVals.average()), "°C", modifier = Modifier.weight(1f))
                            StatCard("T. Máx",  "%.1f".format(tempVals.max()),     "°C", modifier = Modifier.weight(1f))
                            StatCard("T. Mín",  "%.1f".format(tempVals.min()),     "°C", modifier = Modifier.weight(1f))
                        }

                        // Tabs: gráfica variable / comparativa / tabla
                        TabRow(selectedTabIndex = selectedTab) {
                            listOf("Por variable", "Comparativa", "Tabla").forEachIndexed { i, t ->
                                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t) })
                            }
                        }

                        val timePoints = records.map { it.time.take(5) }
                        val tempPoints = timePoints.zip(tempVals)
                        val humPoints  = timePoints.zip(humVals)
                        val windPoints = records.mapNotNull { r -> r.windSpeed?.let { r.time.take(5) to it } }

                        when (selectedTab) {
                            0 -> {
                                var varTab by remember { mutableIntStateOf(0) }
                                TabRow(selectedTabIndex = varTab) {
                                    listOf("Temperatura", "Humedad", "Viento").forEachIndexed { i, t ->
                                        Tab(selected = varTab == i, onClick = { varTab = i }, text = { Text(t) })
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                when (varTab) {
                                    0 -> SingleLineChart(tempPoints, Color(0xFFE57373), "Temperatura (°C)")
                                    1 -> SingleLineChart(humPoints,  Color(0xFF64B5F6), "Humedad (%)")
                                    2 -> SingleLineChart(windPoints, Color(0xFF81C784), "Viento (km/h)")
                                }
                            }
                            1 -> MultiLineChart(
                                tempValues = tempPoints,
                                humValues  = humPoints,
                                windValues = windPoints
                            )
                            2 -> {
                                LazyColumn(
                                    modifier = Modifier.height(400.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    item {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            listOf("Hora", "Temp.", "Hum.", "Viento").forEach {
                                                Text(it, style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            }
                                        }
                                    }
                                    items(records) { record -> HistoricalRow(record) }
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
            Text(StatsUtils.formatValue(record.temperature) + "°C", style = MaterialTheme.typography.bodyMedium)
            Text(StatsUtils.formatValue(record.humidity, 0) + "%", style = MaterialTheme.typography.bodyMedium)
            Text(StatsUtils.formatValue(record.windSpeed) + " km/h", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
