package com.project.weathertec.ui.range

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.weathertec.data.utils.StatsUtils
import com.project.weathertec.ui.shared.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeAverageScreen(vm: WeatherViewModel = viewModel()) {
    var startDate by remember { mutableStateOf("") }
    var endDate   by remember { mutableStateOf("") }
    val recordsState by vm.records.collectAsState()
    var searched by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val dateError = startDate.length == 10 && endDate.length == 10 && endDate < startDate

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("📅 Promedio por Rango") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("Selecciona un rango de fechas")
            Text(
                "Formato: YYYY-MM-DD",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("Fecha inicio") },
                placeholder = { Text("2026-05-01") },
                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                modifier = Modifier.fillMaxWidth(),
                isError = dateError
            )
            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it },
                label = { Text("Fecha fin") },
                placeholder = { Text("2026-05-12") },
                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                modifier = Modifier.fillMaxWidth(),
                isError = dateError
            )
            if (dateError) {
                Text(
                    "⚠️ La fecha fin debe ser posterior a la fecha inicio",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Button(
                onClick = {
                    if (startDate.length == 10 && endDate.length == 10 && !dateError) {
                        searched = true
                        vm.loadByDate(startDate, endDate)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = startDate.length == 10 && endDate.length == 10 && !dateError
            ) {
                Text("Calcular Promedio")
            }

            if (searched) {
                when (val state = recordsState) {
                    is UiState.Loading -> LoadingScreen()
                    is UiState.Empty   -> EmptyScreen("Sin datos para ese rango")
                    is UiState.Error   -> ErrorScreen(state.message)
                    is UiState.Success -> {
                        val records = state.data
                        val tempStats = StatsUtils.calcStats(StatsUtils.getValues(records, "temperature"))
                        val humStats  = StatsUtils.calcStats(StatsUtils.getValues(records, "humidity"))
                        val windStats = StatsUtils.calcStats(StatsUtils.getValues(records, "windSpeed"))
                        val dayGroups = StatsUtils.groupByDay(records, "temperature")
                        val humDays   = StatsUtils.groupByDay(records, "humidity")
                        val windDays  = StatsUtils.groupByDay(records, "windSpeed")

                        HorizontalDivider()
                        SectionTitle("${StatsUtils.formatDate(startDate)} → ${StatsUtils.formatDate(endDate)}")
                        Text("${records.size} registros en ${dayGroups.size} día(s)",
                            style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))

                        // Panel resumen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard("Temp. Prom.", StatsUtils.formatValue(tempStats?.avg), "°C", modifier = Modifier.weight(1f))
                            StatCard("Hum. Prom.",  StatsUtils.formatValue(humStats?.avg, 0), "%",  modifier = Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatCard("Temp. Máx", StatsUtils.formatValue(tempStats?.max), "°C", modifier = Modifier.weight(1f))
                            StatCard("Temp. Mín", StatsUtils.formatValue(tempStats?.min), "°C", modifier = Modifier.weight(1f))
                        }
                        StatCard("Viento Prom.", StatsUtils.formatValue(windStats?.avg), "km/h")

                        Spacer(Modifier.height(8.dp))

                        // Tabs de gráficas
                        TabRow(selectedTabIndex = selectedTab) {
                            listOf("Barras x día", "Líneas").forEachIndexed { i, t ->
                                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        when (selectedTab) {
                            // Gráfica de barras por día
                            0 -> {
                                val dayLabels = dayGroups.map { it.day.takeLast(5) }
                                var barTab by remember { mutableIntStateOf(0) }
                                TabRow(selectedTabIndex = barTab) {
                                    listOf("Temperatura", "Humedad", "Viento").forEachIndexed { i, t ->
                                        Tab(selected = barTab == i, onClick = { barTab = i }, text = { Text(t) })
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                when (barTab) {
                                    0 -> BarChart(
                                        series = dayLabels.zip(dayGroups.map { it.stats?.avg ?: 0.0 }),
                                        color = Color(0xFFE57373),
                                        title = "Temperatura promedio por día (°C)"
                                    )
                                    1 -> BarChart(
                                        series = dayLabels.zip(humDays.map { it.stats?.avg ?: 0.0 }),
                                        color = Color(0xFF64B5F6),
                                        title = "Humedad promedio por día (%)"
                                    )
                                    2 -> BarChart(
                                        series = dayLabels.zip(windDays.map { it.stats?.avg ?: 0.0 }),
                                        color = Color(0xFF81C784),
                                        title = "Viento promedio por día (km/h)"
                                    )
                                }
                            }
                            // Gráfica de líneas con evolución temporal
                            1 -> {
                                val dayLabels = dayGroups.map { it.day.takeLast(5) }
                                MultiLineChart(
                                    tempValues = dayLabels.zip(dayGroups.map { it.stats?.avg ?: 0.0 }),
                                    humValues  = dayLabels.zip(humDays.map { it.stats?.avg ?: 0.0 }),
                                    windValues = dayLabels.zip(windDays.map { it.stats?.avg ?: 0.0 })
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
