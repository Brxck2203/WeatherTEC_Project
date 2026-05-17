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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RangeAverageScreen(vm: WeatherViewModel = viewModel()) {
    var startDate by remember { mutableStateOf("") }
    var endDate   by remember { mutableStateOf("") }
    val recordsState by vm.records.collectAsState()
    var searched by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val dateError = startDate.length == 10 && endDate.length == 10 && startDate > endDate

    // Hoy en UTC ms — el tope que no se debe superar
    val todayUtcMillis = remember {
        LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }
    val currentYear = remember { LocalDate.now().year }

    // DatePicker para fecha inicio (no permite fechas futuras)
    val startPickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                return utcTimeMillis <= todayUtcMillis
            }
            override fun isSelectableYear(year: Int): Boolean {
                return year <= currentYear
            }
        }
    )
    var showStartPicker by remember { mutableStateOf(false) }

    // DatePicker para fecha fin — no fechas futuras y no antes de startDate
    val endPickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (utcTimeMillis > todayUtcMillis) return false
                if (startDate.length < 10) return true
                val startMillis = startDate.toEpochMillis()
                return utcTimeMillis >= startMillis
            }
            override fun isSelectableYear(year: Int): Boolean {
                return year <= currentYear
            }
        }
    )
    var showEndPicker by remember { mutableStateOf(false) }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    if (showStartPicker) {
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startPickerState.selectedDateMillis?.let { millis ->
                        startDate = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).format(formatter)
                        if (endDate.isNotEmpty() && endDate < startDate) endDate = ""
                    }
                    showStartPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = startPickerState)
        }
    }

    if (showEndPicker) {
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endPickerState.selectedDateMillis?.let { millis ->
                        endDate = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).format(formatter)
                    }
                    showEndPicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = endPickerState)
        }
    }

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

            OutlinedTextField(
                value = startDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha inicio") },
                placeholder = { Text("Selecciona fecha de inicio") },
                leadingIcon = {
                    IconButton(onClick = { showStartPicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Abrir calendario inicio")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                isError = dateError
            )

            OutlinedTextField(
                value = endDate,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha fin") },
                placeholder = { Text("Selecciona fecha de fin") },
                leadingIcon = {
                    IconButton(onClick = { showEndPicker = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Abrir calendario fin")
                    }
                },
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

                        TabRow(selectedTabIndex = selectedTab) {
                            listOf("Barras x día", "Líneas").forEachIndexed { i, t ->
                                Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        when (selectedTab) {
                            0 -> {
                                val dayLabels = dayGroups.map { it.date.takeLast(5) }
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
                            1 -> {
                                val dayLabels = dayGroups.map { it.date.takeLast(5) }
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

private fun String.toEpochMillis(): Long {
    return try {
        val parts = this.split("-")
        val year = parts[0].toInt(); val month = parts[1].toInt(); val day = parts[2].toInt()
        java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).also {
            it.set(year, month - 1, day, 0, 0, 0)
            it.set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    } catch (e: Exception) { 0L }
}