package com.project.weathertec.ui.comparison

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.weathertec.data.model.WeatherRecord
import com.project.weathertec.data.repository.WeatherRepository
import com.project.weathertec.ui.shared.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonScreen() {
    val repository = remember { WeatherRepository() }
    val scope = rememberCoroutineScope()

    var date1 by remember { mutableStateOf("") }
    var date2 by remember { mutableStateOf("") }
    var records1 by remember { mutableStateOf<List<WeatherRecord>>(emptyList()) }
    var records2 by remember { mutableStateOf<List<WeatherRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var compared by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // Hoy en UTC ms — el tope que no se debe superar
    val todayUtcMillis = remember {
        LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }
    val currentYear = remember { LocalDate.now().year }

    // Restricción común: no fechas futuras
    val noFutureDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            return utcTimeMillis <= todayUtcMillis
        }
        override fun isSelectableYear(year: Int): Boolean {
            return year <= currentYear
        }
    }

    val picker1State = rememberDatePickerState(selectableDates = noFutureDates)
    var showPicker1 by remember { mutableStateOf(false) }

    val picker2State = rememberDatePickerState(selectableDates = noFutureDates)
    var showPicker2 by remember { mutableStateOf(false) }

    if (showPicker1) {
        DatePickerDialog(
            onDismissRequest = { showPicker1 = false },
            confirmButton = {
                TextButton(onClick = {
                    picker1State.selectedDateMillis?.let { millis ->
                        date1 = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).format(formatter)
                    }
                    showPicker1 = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker1 = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = picker1State)
        }
    }

    if (showPicker2) {
        DatePickerDialog(
            onDismissRequest = { showPicker2 = false },
            confirmButton = {
                TextButton(onClick = {
                    picker2State.selectedDateMillis?.let { millis ->
                        date2 = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).format(formatter)
                    }
                    showPicker2 = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker2 = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = picker2State)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("⇔️ Comparación") })
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
            SectionTitle("Compara dos días")

            OutlinedTextField(
                value = date1,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha 1") },
                placeholder = { Text("Selecciona la primera fecha") },
                leadingIcon = {
                    IconButton(onClick = { showPicker1 = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Abrir calendario 1")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = date2,
                onValueChange = {},
                readOnly = true,
                label = { Text("Fecha 2") },
                placeholder = { Text("Selecciona la segunda fecha") },
                leadingIcon = {
                    IconButton(onClick = { showPicker2 = true }) {
                        Icon(Icons.Default.DateRange, contentDescription = "Abrir calendario 2")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (date1.length == 10 && date2.length == 10) {
                        compared = true
                        error = null
                        loading = true
                        scope.launch {
                            try {
                                records1 = repository.fetchFromFirebase(date1)
                                records2 = repository.fetchFromFirebase(date2)
                            } catch (e: Exception) {
                                error = "Error: ${e.message}"
                            } finally {
                                loading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = date1.length == 10 && date2.length == 10
            ) {
                Text("Comparar")
            }

            if (loading) LoadingScreen()
            else if (error != null) ErrorScreen(error!!)
            else if (compared) {
                if (records1.isEmpty() && records2.isEmpty()) {
                    EmptyScreen("Sin datos para las fechas seleccionadas")
                } else {
                    HorizontalDivider()

                    TabRow(selectedTabIndex = selectedTab) {
                        listOf("Tabla", "Temperatura", "Humedad", "Viento").forEachIndexed { i, t ->
                            Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(t) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))

                    val temps1  = records1.mapNotNull { it.temperature }
                    val temps2  = records2.mapNotNull { it.temperature }
                    val humids1 = records1.mapNotNull { it.humidity }
                    val humids2 = records2.mapNotNull { it.humidity }
                    val winds1  = records1.mapNotNull { it.windSpeed }
                    val winds2  = records2.mapNotNull { it.windSpeed }

                    when (selectedTab) {
                        0 -> ComparisonTable(
                            label1 = date1, records1 = records1,
                            label2 = date2, records2 = records2
                        )
                        1 -> ComparisonBarChart(
                            labels = listOf("Prom", "Máx", "Mín"),
                            series1 = listOf(temps1.average(), temps1.maxOrNull() ?: 0.0, temps1.minOrNull() ?: 0.0),
                            series2 = listOf(temps2.average(), temps2.maxOrNull() ?: 0.0, temps2.minOrNull() ?: 0.0),
                            label1 = date1, label2 = date2,
                            title = "Temperatura (°C) — $date1 vs $date2"
                        )
                        2 -> ComparisonBarChart(
                            labels = listOf("Prom", "Máx", "Mín"),
                            series1 = listOf(humids1.average(), humids1.maxOrNull() ?: 0.0, humids1.minOrNull() ?: 0.0),
                            series2 = listOf(humids2.average(), humids2.maxOrNull() ?: 0.0, humids2.minOrNull() ?: 0.0),
                            label1 = date1, label2 = date2,
                            title = "Humedad (%) — $date1 vs $date2"
                        )
                        3 -> ComparisonBarChart(
                            labels = listOf("Prom", "Máx", "Mín"),
                            series1 = listOf(winds1.average(), winds1.maxOrNull() ?: 0.0, winds1.minOrNull() ?: 0.0),
                            series2 = listOf(winds2.average(), winds2.maxOrNull() ?: 0.0, winds2.minOrNull() ?: 0.0),
                            label1 = date1, label2 = date2,
                            title = "Viento (km/h) — $date1 vs $date2"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComparisonTable(
    label1: String, records1: List<WeatherRecord>,
    label2: String, records2: List<WeatherRecord>
) {
    val temps1 = records1.mapNotNull { it.temperature }
    val temps2 = records2.mapNotNull { it.temperature }
    val humids1 = records1.mapNotNull { it.humidity }
    val humids2 = records2.mapNotNull { it.humidity }
    val winds1 = records1.mapNotNull { it.windSpeed }
    val winds2 = records2.mapNotNull { it.windSpeed }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("$label1 vs $label2")
        ComparisonRow("Métrica", label1, label2, isHeader = true)
        ComparisonRow("Temp. Prom", temps1.avg("°C"), temps2.avg("°C"))
        ComparisonRow("Temp. Máx", temps1.maxOrNull()?.let { "%.1f°C".format(it) } ?: "--", temps2.maxOrNull()?.let { "%.1f°C".format(it) } ?: "--")
        ComparisonRow("Temp. Mín", temps1.minOrNull()?.let { "%.1f°C".format(it) } ?: "--", temps2.minOrNull()?.let { "%.1f°C".format(it) } ?: "--")
        ComparisonRow("Hum. Prom", humids1.avg("%"), humids2.avg("%"))
        ComparisonRow("Viento Prom", winds1.avg("km/h"), winds2.avg("km/h"))
        ComparisonRow("Registros", "${records1.size}", "${records2.size}")
    }
}

fun List<Double>.avg(unit: String): String =
    if (isEmpty()) "--" else "%.1f$unit".format(average())

@Composable
fun ComparisonRow(label: String, val1: String, val2: String, isHeader: Boolean = false) {
    val style = if (isHeader) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
    val containerColor = if (isHeader) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = style, modifier = Modifier.weight(1.5f))
            Text(val1,  style = style, modifier = Modifier.weight(1f))
            Text(val2,  style = style, modifier = Modifier.weight(1f))
        }
    }
}