package com.project.weathertec.ui.export

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.project.weathertec.data.model.WeatherRecord
import com.project.weathertec.data.repository.WeatherRepository
import com.project.weathertec.ui.shared.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen() {
    val context = LocalContext.current
    val repository = remember { WeatherRepository() }
    val scope = rememberCoroutineScope()

    // Modo: 0 = fecha única, 1 = rango
    var mode by remember { mutableIntStateOf(0) }
    var singleDate by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var records by remember { mutableStateOf<List<WeatherRecord>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var fetched by remember { mutableStateOf(false) }
    var savedFile by remember { mutableStateOf<File?>(null) }

    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // No permitir fechas futuras
    val todayUtcMillis = remember {
        LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }
    val currentYear = remember { LocalDate.now().year }
    val noFutureDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayUtcMillis
        override fun isSelectableYear(year: Int) = year <= currentYear
    }

    // Validación de rango
    val dateRangeError = mode == 1 &&
            startDate.length == 10 && endDate.length == 10 && startDate > endDate

    // ===== DatePickers =====
    val singlePickerState = rememberDatePickerState(selectableDates = noFutureDates)
    var showSinglePicker by remember { mutableStateOf(false) }

    val startPickerState = rememberDatePickerState(selectableDates = noFutureDates)
    var showStartPicker by remember { mutableStateOf(false) }

    val endPickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                if (utcTimeMillis > todayUtcMillis) return false
                if (startDate.length < 10) return true
                return utcTimeMillis >= startDate.toEpochMillis()
            }
            override fun isSelectableYear(year: Int) = year <= currentYear
        }
    )
    var showEndPicker by remember { mutableStateOf(false) }

    if (showSinglePicker) {
        DatePickerDialog(
            onDismissRequest = { showSinglePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    singlePickerState.selectedDateMillis?.let { millis ->
                        singleDate = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).format(formatter)
                    }
                    showSinglePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showSinglePicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = singlePickerState) }
    }

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
        ) { DatePicker(state = startPickerState) }
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
        ) { DatePicker(state = endPickerState) }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("💾 Exportar Datos") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("Exportar datos como CSV")

            // Selector de modo (fecha única / rango)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "¿Qué deseas exportar?",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = mode == 0,
                            onClick = {
                                mode = 0
                                fetched = false; records = emptyList(); savedFile = null
                            }
                        )
                        Text("Una sola fecha")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = mode == 1,
                            onClick = {
                                mode = 1
                                fetched = false; records = emptyList(); savedFile = null
                            }
                        )
                        Text("Rango de fechas")
                    }
                }
            }

            // Campos según el modo
            if (mode == 0) {
                OutlinedTextField(
                    value = singleDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha") },
                    placeholder = { Text("Selecciona una fecha") },
                    leadingIcon = {
                        IconButton(onClick = { showSinglePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Abrir calendario")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
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
                    isError = dateRangeError
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
                    isError = dateRangeError
                )
                if (dateRangeError) {
                    Text(
                        "⚠️ La fecha fin debe ser posterior a la fecha inicio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Determinar si el botón debe estar habilitado
            val canFetch = when (mode) {
                0 -> singleDate.length == 10
                else -> startDate.length == 10 && endDate.length == 10 && !dateRangeError
            }

            Button(
                onClick = {
                    loading = true; fetched = false; error = null; savedFile = null
                    scope.launch {
                        try {
                            records = if (mode == 0) {
                                repository.fetchFromFirebase(singleDate)
                            } else {
                                repository.fetchFromFirebase(startDate, endDate)
                            }
                            fetched = true
                        } catch (e: Exception) {
                            error = "Error: ${e.message}"
                        } finally {
                            loading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canFetch
            ) {
                Text("Cargar Datos")
            }

            if (loading) LoadingScreen()
            else if (error != null) ErrorScreen(error!!)
            else if (fetched) {
                if (records.isEmpty()) {
                    EmptyScreen("Sin datos para la(s) fecha(s) seleccionada(s)")
                } else {
                    Text(
                        "${records.size} registro(s) encontrado(s)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider()

                    // Nombre del archivo según el modo
                    val fileName = if (mode == 0) {
                        "WeatherTEC_$singleDate.csv"
                    } else {
                        "WeatherTEC_${startDate}_a_${endDate}.csv"
                    }

                    // Botón 1: Guardar/Descargar el archivo CSV
                    Button(
                        onClick = {
                            val csv = buildCsv(records)
                            savedFile = saveCsvToFile(context, csv, fileName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generar archivo CSV")
                    }

                    // Si ya está generado, mostrar opciones de compartir/abrir
                    savedFile?.let { file ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "✅ Archivo generado",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    file.name,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { shareCsvFile(context, file) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Share, null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Compartir")
                                    }
                                    OutlinedButton(
                                        onClick = { openCsvFile(context, file) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Abrir")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Vista previa (primeros 5):",
                        style = MaterialTheme.typography.labelMedium
                    )
                    records.take(5).forEach { r ->
                        Text(
                            "${r.date} ${r.time} | ${r.temperature?.let { "%.1f°C".format(it) } ?: "--"} | " +
                                    "${r.humidity?.let { "%.0f%%".format(it) } ?: "--"} | ${r.conditions}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

// ===== Helpers =====

fun buildCsv(records: List<WeatherRecord>): String {
    val header = "Date,Time,Temperature(C),Humidity(%),WindSpeed(km/h),Conditions,Source\n"
    val rows = records.joinToString("\n") { r ->
        "${r.date},${r.time},${r.temperature ?: ""},${r.humidity ?: ""}," +
                "${r.windSpeed ?: ""},\"${r.conditions}\",${r.source}"
    }
    return header + rows
}

/** Guarda el CSV como archivo real en cache/exports/ y devuelve el File. */
fun saveCsvToFile(context: Context, csv: String, fileName: String): File {
    val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
    val file = File(exportsDir, fileName)
    file.writeText(csv)
    return file
}

/** Comparte el archivo CSV usando un URI seguro via FileProvider. */
fun shareCsvFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir CSV"))
}

/** Abre el archivo CSV con la app que el usuario elija. */
fun openCsvFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "text/csv")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Abrir CSV"))
    } catch (e: Exception) {
        // Si no hay app para abrir CSV, ofrecer compartir
        shareCsvFile(context, file)
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