package com.project.weathertec.ui.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StatCard(
    label: String,
    value: String,
    unit: String = "",
    color: Color = MaterialTheme.colorScheme.primaryContainer,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (value.isNotEmpty()) "$value $unit".trim() else "--",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        ),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Cargando datos...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun EmptyScreen(message: String = "Sin datos disponibles") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: (() -> Unit)? = null) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
            if (onRetry != null) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry) { Text("Reintentar") }
            }
        }
    }
}

@Composable
fun WeatherIcon(conditions: String, modifier: Modifier = Modifier) {
    val emoji = when {
        conditions.contains("sun", ignoreCase = true) ||
                conditions.contains("clear", ignoreCase = true) ||
                conditions.contains("despejado", ignoreCase = true) -> "☀️"
        conditions.contains("cloud", ignoreCase = true) ||
                conditions.contains("nublado", ignoreCase = true) -> "⛅"
        conditions.contains("rain", ignoreCase = true) ||
                conditions.contains("lluvia", ignoreCase = true) -> "🌧️"
        conditions.contains("storm", ignoreCase = true) ||
                conditions.contains("tormenta", ignoreCase = true) -> "⛈️"
        conditions.contains("fog", ignoreCase = true) ||
                conditions.contains("neblina", ignoreCase = true) -> "🌫️"
        else -> "🌤️"
    }
    Text(text = emoji, style = MaterialTheme.typography.displayMedium, modifier = modifier)
}
