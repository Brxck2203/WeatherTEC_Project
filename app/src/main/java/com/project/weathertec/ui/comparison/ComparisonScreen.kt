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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("↔️ Comparación") })
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
                onValueChange = { date1 = it },
                label = { Text("Fecha 1 (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = date2,
                onValueChange = { date2 = it },
                label = { Text("Fecha 2 (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.DateRange, null) },
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
                    Divider()
                    ComparisonTable(
                        label1 = date1, records1 = records1,
                        label2 = date2, records2 = records2
                    )
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
        SectionTitle("Comparación: $label1 vs $label2")

        ComparisonRow("Métrica", label1, label2, isHeader = true)
        ComparisonRow(
            "Temp. Prom",
            temps1.avg("°C"),
            temps2.avg("°C")
        )
        ComparisonRow(
            "Temp. Máx",
            temps1.maxOrNull()?.let { "%.1f°C".format(it) } ?: "--",
            temps2.maxOrNull()?.let { "%.1f°C".format(it) } ?: "--"
        )
        ComparisonRow(
            "Temp. Mín",
            temps1.minOrNull()?.let { "%.1f°C".format(it) } ?: "--",
            temps2.minOrNull()?.let { "%.1f°C".format(it) } ?: "--"
        )
        ComparisonRow("Hum. Prom", humids1.avg("%"), humids2.avg("%"))
        ComparisonRow("Viento Prom", winds1.avg("km/h"), winds2.avg("km/h"))
        ComparisonRow("Registros", "${records1.size}", "${records2.size}")
    }
}

fun List<Double>.avg(unit: String): String =
    if (isEmpty()) "--" else "%.1f$unit".format(average())

@Composable
fun ComparisonRow(label: String, val1: String, val2: String, isHeader: Boolean = false) {
    val style = if (isHeader) MaterialTheme.typography.titleSmall
    else MaterialTheme.typography.bodyMedium
    val containerColor = if (isHeader) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = style, modifier = Modifier.weight(1.5f))
            Text(val1, style = style, modifier = Modifier.weight(1f))
            Text(val2, style = style, modifier = Modifier.weight(1f))
        }
    }
}
