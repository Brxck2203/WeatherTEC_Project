package com.project.weathertec.ui.export

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.weathertec.data.model.WeatherRecord
import com.project.weathertec.data.repository.WeatherRepository
import com.project.weathertec.ui.shared.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen() {
    val context = LocalContext.current
    val repository = remember { WeatherRepository() }
    val scope = rememberCoroutineScope()

    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var records by remember { mutableStateOf<List<WeatherRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var fetched by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("💾 Exportar Datos") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("Exportar datos como CSV")
            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("Fecha inicio (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = endDate,
                onValueChange = { endDate = it },
                label = { Text("Fecha fin (YYYY-MM-DD)") },
                leadingIcon = { Icon(Icons.Default.DateRange, null) },
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    if (startDate.length == 10 && endDate.length == 10) {
                        loading = true; fetched = false; error = null
                        scope.launch {
                            try {
                                records = repository.fetchFromFirebase(startDate, endDate)
                                fetched = true
                            } catch (e: Exception) {
                                error = "Error: ${e.message}"
                            } finally {
                                loading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = startDate.length == 10 && endDate.length == 10
            ) {
                Text("Cargar Datos")
            }

            if (loading) LoadingScreen()
            else if (error != null) ErrorScreen(error!!)
            else if (fetched) {
                if (records.isEmpty()) {
                    EmptyScreen("Sin datos para ese rango")
                } else {
                    Text(
                        "${records.size} registro(s) encontrado(s)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Divider()
                    Button(
                        onClick = {
                            val csv = buildCsv(records)
                            shareText(context, csv, "WeatherTEC_$startDate-$endDate.csv")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Compartir CSV")
                    }
                    Text(
                        "Vista previa (primeros 5):",
                        style = MaterialTheme.typography.labelMedium
                    )
                    records.take(5).forEach { r ->
                        Text(
                            "${r.date} ${r.time} | ${r.temperature?.let { "%.1f°C".format(it) }} | " +
                                    "${r.humidity?.let { "%.0f%%".format(it) }} | ${r.conditions}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

fun buildCsv(records: List<WeatherRecord>): String {
    val header = "Date,Time,Temperature(C),Humidity(%),WindSpeed(km/h),Conditions,Source\n"
    val rows = records.joinToString("\n") { r ->
        "${r.date},${r.time},${r.temperature ?: ""},${r.humidity ?: ""}," +
                "${r.windSpeed ?: ""},\"${r.conditions}\",${r.source}"
    }
    return header + rows
}

fun shareText(context: Context, text: String, fileName: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, fileName)
    }
    context.startActivity(Intent.createChooser(intent, "Exportar como CSV"))
}
