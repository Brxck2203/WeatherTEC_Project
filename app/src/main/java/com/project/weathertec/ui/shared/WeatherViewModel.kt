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

    private val _liveConditions = MutableStateFlow<UiState<WeatherRecord>>(UiState.Loading)
    val liveConditions: StateFlow<UiState<WeatherRecord>> = _liveConditions

    private val _records = MutableStateFlow<UiState<List<WeatherRecord>>>(UiState.Loading)
    val records: StateFlow<UiState<List<WeatherRecord>>> = _records

    val today: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

    fun loadDashboard() {
        viewModelScope.launch {
            _liveConditions.value = UiState.Loading
            _records.value = UiState.Loading

            val live = repository.fetchLiveConditions()
            _liveConditions.value = if (live != null) UiState.Success(live) else UiState.Empty

            try {
                val historical = repository.fetchFromFirebase(today)
                _records.value = if (historical.isNotEmpty()) UiState.Success(historical)
                else if (live != null) UiState.Success(listOf(live))
                else UiState.Empty
            } catch (e: Exception) {
                _records.value = UiState.Error(
                    if (e.message?.contains("timeout", ignoreCase = true) == true)
                        "Firebase no respondió a tiempo. Intenta nuevamente."
                    else "No se pudieron obtener datos de Firebase."
                )
            }
        }
    }

    fun loadByDate(startDate: String, endDate: String? = null) {
        viewModelScope.launch {
            _records.value = UiState.Loading
            try {
                val data = repository.fetchFromFirebase(startDate, endDate)
                _records.value = if (data.isNotEmpty()) UiState.Success(data) else UiState.Empty
            } catch (e: Exception) {
                _records.value = UiState.Error("Error al cargar datos: ${e.message}")
            }
        }
    }
}
