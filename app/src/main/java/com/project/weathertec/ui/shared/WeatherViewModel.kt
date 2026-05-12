package com.project.weathertec.ui.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.weathertec.data.model.WeatherRecord
import com.project.weathertec.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
    object Empty : UiState<Nothing>()
}

class WeatherViewModel : ViewModel() {

    private val repository = WeatherRepository()

    // Dato más reciente leído de Firebase (para el Dashboard)
    private val _latestRecord = MutableStateFlow<UiState<WeatherRecord>>(UiState.Loading)
    val latestRecord: StateFlow<UiState<WeatherRecord>> = _latestRecord

    // Lista de registros para cualquier pantalla que necesite histórico
    private val _records = MutableStateFlow<UiState<List<WeatherRecord>>>(UiState.Loading)
    val records: StateFlow<UiState<List<WeatherRecord>>> = _records

    val today: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    /**
     * Carga el dashboard:
     * 1. Lee los registros de hoy desde Firebase
     * 2. El más reciente = "condición actual"
     * 3. Todos los de hoy = estadísticas del día
     *
     * La app NUNCA llama a Google Weather directamente.
     */
    fun loadDashboard() {
        viewModelScope.launch {
            _latestRecord.value = UiState.Loading
            _records.value = UiState.Loading
            try {
                val todayRecords = repository.fetchFromFirebase(today)
                if (todayRecords.isEmpty()) {
                    _latestRecord.value = UiState.Empty
                    _records.value = UiState.Empty
                } else {
                    // El más reciente es el último (ya vienen ordenados por timestamp)
                    _latestRecord.value = UiState.Success(todayRecords.last())
                    _records.value = UiState.Success(todayRecords)
                }
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Firebase no respondió a tiempo. Intenta nuevamente."
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Sin conexión a internet."
                    else -> "Error al conectar con Firebase: ${e.message}"
                }
                _latestRecord.value = UiState.Error(msg)
                _records.value = UiState.Error(msg)
            }
        }
    }

    /**
     * Carga registros de Firebase para una fecha o rango de fechas.
     * Usado por: Histórico, Promedio por Rango, Comparación, Exportar.
     */
    fun loadByDate(startDate: String, endDate: String? = null) {
        viewModelScope.launch {
            _records.value = UiState.Loading
            try {
                val data = repository.fetchFromFirebase(startDate, endDate)
                _records.value = if (data.isNotEmpty()) UiState.Success(data) else UiState.Empty
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        "Firebase no respondió a tiempo. Intenta nuevamente."
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                        "Sin conexión a internet."
                    else -> "Error al cargar datos: ${e.message}"
                }
                _records.value = UiState.Error(msg)
            }
        }
    }
}
